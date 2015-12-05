package eu.toolchain.rs.processor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.ws.rs.HttpMethod;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import eu.toolchain.rs.processor.annotation.DefaultValueMirror;
import eu.toolchain.rs.processor.annotation.HeaderParamMirror;
import eu.toolchain.rs.processor.annotation.PathMirror;
import eu.toolchain.rs.processor.annotation.PathParamMirror;
import eu.toolchain.rs.processor.annotation.QueryParamMirror;
import eu.toolchain.rs.processor.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RsClassProcessor {
    public static final List<String> DEFAULT_PATH = ImmutableList.of();
    public static final CharMatcher SLASH = CharMatcher.anyOf("/");
    public static final Splitter SLASH_SPLITTER = Splitter.on(SLASH);
    public static final Joiner SLASH_JOINER = Joiner.on("/");
    public static final Joiner PARAMETER_JOINER = Joiner.on(", ");

    final Types types;
    final Elements elements;
    final RsUtils utils;

    public Result<JavaFile> process(final TypeElement element) {
        final String packageName = elements.getPackageOf(element).getQualifiedName().toString();

        final Result<List<String>> unverifiedRootPath = path(element);

        final TypeSpec.Builder generated = TypeSpec.classBuilder(utils.bindingName(element));

        generated.addAnnotation(utils.generatedAnnotation());

        final FieldSpec instanceField =
                FieldSpec.builder(TypeName.get(element.asType()), "instance")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL).build();

        generated.addModifiers(Modifier.PUBLIC);

        generated.addField(instanceField);

        generated.addMethod(constructor(element, instanceField));

        return unverifiedRootPath.flatMap(rootPath -> {
            final ImmutableList.Builder<Result<Consumer<TypeSpec.Builder>>> unverifiedHandlers =
                    ImmutableList.builder();

            final LinkedHashSet<TypeMirror> returnTypes = new LinkedHashSet<>();
            final ImmutableList.Builder<ExecutableElement> methods = ImmutableList.builder();

            for (final Element enclosed : element.getEnclosedElements()) {
                if (enclosed.getKind() != ElementKind.METHOD) {
                    continue;
                }

                if (enclosed.getModifiers().contains(Modifier.PRIVATE)) {
                    continue;
                }

                final ExecutableElement executable = (ExecutableElement) enclosed;

                method(executable).ifPresent(method -> {
                    methods.add(executable);
                    returnTypes.add(utils.box(executable.getReturnType()));
                    unverifiedHandlers
                            .add(endpointSetup(executable, instanceField, rootPath, method));
                });
            }

            final TypeName routesReturnType = utils.greatestCommonType(returnTypes);

            unverifiedHandlers.add(Result.ok(routesMethod(routesReturnType, methods)));

            return Result.combine(unverifiedHandlers.build()).map(handlers -> {
                handlers.stream().forEach(h -> h.accept(generated));

                return JavaFile.builder(packageName, generated.build()).skipJavaLangImports(true)
                        .indent("    ").build();
            });
        });
    }

    private Consumer<Builder> routesMethod(final TypeName returnType,
            final ImmutableList.Builder<ExecutableElement> methods) {
        return builder -> {
            final MethodSpec.Builder method = MethodSpec.methodBuilder("routes");
            method.addModifiers(Modifier.PUBLIC);

            method.returns(ParameterizedTypeName.get(utils.list(),
                    ParameterizedTypeName.get(utils.rsMapping(), returnType)));
            method.addStatement("final $T<$T<$T>> $L = new $T<>()", utils.list(), utils.rsMapping(),
                    returnType, "routes", utils.arrayList());

            for (final ExecutableElement executable : methods.build()) {
                method.addStatement("$L.add($L())", "routes", utils.mappingMethod(executable));
            }

            method.addStatement("return $L", "routes");

            builder.addMethod(method.build());
        };
    }

    private Result<List<String>> path(final Element element) {
        return utils.path(element).map(p -> p.map(PathMirror::getValue).map(this::processPath))
                .orElse(Result.ok(DEFAULT_PATH));
    }

    private Result<List<String>> consumes(final Element element) {
        return utils.consumes(element).map(o -> o.map(v -> v.getValue()))
                .orElseGet(() -> Result.ok(ImmutableList.of()));
    }

    private Result<List<String>> produces(final Element element) {
        return utils.produces(element).map(o -> o.map(v -> v.getValue()))
                .orElseGet(() -> Result.ok(ImmutableList.of()));
    }

    private Optional<String> method(final Element element) {
        if (utils.hasGET(element)) {
            return Optional.of(HttpMethod.GET);
        }

        if (utils.hasPOST(element)) {
            return Optional.of(HttpMethod.POST);
        }

        if (utils.hasPUT(element)) {
            return Optional.of(HttpMethod.PUT);
        }

        if (utils.hasDELETE(element)) {
            return Optional.of(HttpMethod.DELETE);
        }

        if (utils.hasOPTIONS(element)) {
            return Optional.of(HttpMethod.OPTIONS);
        }

        return Optional.empty();
    }

    private List<String> processPath(final String input) {
        final String trimmed = SLASH.trimFrom(input);

        if ("".equals(trimmed)) {
            return DEFAULT_PATH;
        }

        return SLASH_SPLITTER.splitToList(trimmed);
    }

    private MethodSpec constructor(final TypeElement element, final FieldSpec instanceField) {
        final ParameterSpec instance =
                ParameterSpec.builder(TypeName.get(element.asType()), "instance")
                        .addModifiers(Modifier.FINAL).build();

        final MethodSpec.Builder b = MethodSpec.constructorBuilder();
        b.addModifiers(Modifier.PUBLIC);
        b.addParameter(instance);

        b.addStatement("this.$N = $N", instanceField, instance);

        return b.build();
    }

    private Result<Consumer<TypeSpec.Builder>> endpointSetup(final ExecutableElement endpoint,
            final FieldSpec instanceField, final List<String> root, final String method) {
        final Result<MethodSpec> unverifiedHandler = handlerMethod(endpoint, instanceField);

        final Result<List<String>> unverifiedPath = path(endpoint);
        final Result<List<String>> unverifiedConsumes = consumes(endpoint);
        final Result<List<String>> unverifiedProduces = produces(endpoint);

        final MethodSpec.Builder mapping = MethodSpec.methodBuilder(utils.mappingMethod(endpoint));
        mapping.addModifiers(Modifier.PUBLIC);
        mapping.returns(ParameterizedTypeName.get(utils.rsMapping(),
                TypeName.get(utils.box(endpoint.getReturnType()))));

        return Result.combineDifferent(unverifiedPath, unverifiedHandler, unverifiedConsumes,
                unverifiedProduces).map(v -> {
                    final List<String> path = unverifiedPath.get();
                    final MethodSpec handler = unverifiedHandler.get();

                    final ChainStatement stmt = new ChainStatement().add("return ")
                            .add("$T.<$T>builder()", utils.rsMapping(),
                                    TypeName.get(utils.box(endpoint.getReturnType())))
                            .add(".method($S)", method)
                            .addVarString(".path(%s)",
                                    ImmutableList.copyOf(Iterables.concat(root, path)))
                            .add(".handle(this::$L)", endpoint.getSimpleName().toString())
                            .addVarString(".consumes(%s)", unverifiedConsumes.get())
                            .addVarString(".produces(%s)", unverifiedProduces.get())
                            .add(".build()");

                    mapping.addStatement(stmt.format(), stmt.arguments());

                    return builder -> {
                        builder.addMethod(handler);
                        builder.addMethod(mapping.build());
                    };
                });
    }

    private Result<MethodSpec> handlerMethod(final ExecutableElement method,
            final FieldSpec instanceField) {
        final MethodSpec.Builder handler =
                MethodSpec.methodBuilder(method.getSimpleName().toString());
        final ParameterSpec ctx =
                ParameterSpec.builder(utils.rsRequestContext(), "ctx", Modifier.FINAL).build();

        final ImmutableList.Builder<Result<Consumer<MethodSpec.Builder>>> consumers =
                ImmutableList.builder();
        final ImmutableList.Builder<String> variables = ImmutableList.builder();
        final AtomicInteger payloadParameters = new AtomicInteger();

        for (final VariableElement parameter : method.getParameters()) {
            final Result<Optional<DefaultValueMirror>> defaultValue = utils.defaultValue(parameter)
                    .map(p -> p.map(v -> Optional.of(v))).orElse(Result.ok(Optional.empty()));

            final List<Result<Consumer<MethodSpec.Builder>>> consumer = new ArrayList<>();

            utils.pathParam(parameter).ifPresent(pathParam -> {
                consumer.add(handlePathParam(ctx, variables, parameter, pathParam, defaultValue));
            });

            utils.queryParam(parameter).ifPresent(queryParam -> {
                consumer.add(handleQueryParam(ctx, variables, parameter, queryParam, defaultValue));
            });

            utils.headerParam(parameter).ifPresent(headerParam -> {
                consumer.add(
                        handleHeaderParam(ctx, variables, parameter, headerParam, defaultValue));
            });

            if (consumer.size() > 1) {
                consumers.add(Result.brokenElement(
                        "Only one of @PathParam, @QueryParam, or @HeaderParam may be present "
                                + "at the same time",
                        parameter));
                continue;
            }

            if (consumer.isEmpty()) {
                payloadParameters.incrementAndGet();
                consumer.add(
                        handlePayload(ctx, variables, payloadParameters, parameter, defaultValue));
            }

            consumers.addAll(consumer);
        }

        return Result.combine(consumers.build()).map(factories -> {
            handler.addParameter(ctx);
            handler.addModifiers(Modifier.PUBLIC);
            handler.returns(TypeName.get(method.getReturnType()));

            for (final Consumer<MethodSpec.Builder> factory : factories) {
                factory.accept(handler);
            }

            handler.addStatement("return $N.$L($L)", instanceField,
                    method.getSimpleName().toString(), PARAMETER_JOINER.join(variables.build()));
            return handler.build();
        });
    }

    private Result<Consumer<MethodSpec.Builder>> handlePayload(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final AtomicInteger payloadParameters,
            final VariableElement parameter,
            final Result<Optional<DefaultValueMirror>> defaultValue) {
        if (payloadParameters.get() > 1) {
            return Result.brokenElement("There must only be one payload argument", parameter);
        }

        if (utils.isList(parameter.asType())) {
            return Result.brokenElement("Payload argument must not be list", parameter);
        }

        return defaultValue.map(def -> {
            final Consumer<ChainStatement> get = stmt -> stmt.add(".getPayload()");

            final Consumer<ChainStatement> handleAbsent = stmt -> {
                if (def.isPresent()) {
                    stmt.add(".orElseGet($N.provideDefaultPayload($S))", ctx, def.get());
                } else {
                    stmt.add(".orElseThrow(() -> new $T())", utils.rxMissingPayload());
                }
            };

            return provideArgument(ctx, variables, parameter, get, handleAbsent);
        });
    }

    private Result<Consumer<MethodSpec.Builder>> handlePathParam(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final VariableElement parameter,
            final Result<PathParamMirror> unvPathParam,
            final Result<Optional<DefaultValueMirror>> unvDefaultValue) {
        if (utils.isList(parameter.asType())) {
            return Result.brokenElement("Path parameter argument must not be list", parameter);
        }

        return Result.combineDifferent(unvPathParam, unvDefaultValue).map(v -> {
            final PathParamMirror pathParam = unvPathParam.get();
            final Optional<DefaultValueMirror> defaultValue = unvDefaultValue.get();

            final Consumer<ChainStatement> get = stmt -> {
                stmt.add(".getPathParameter($S)", pathParam.getValue());
            };

            final Consumer<ChainStatement> handleAbsent = stmt -> {
                if (defaultValue.isPresent()) {
                    stmt.add(".orElseGet($N.provideDefaultPathParameter($S, $S))", ctx,
                            pathParam.getValue(), defaultValue.get().getValue());
                } else {
                    stmt.add(".orElseThrow(() -> new $T($S))", utils.rsMissingPathParameter(),
                            pathParam.getValue());
                }
            };

            return provideArgument(ctx, variables, parameter, get, handleAbsent);
        });
    }

    private Result<Consumer<MethodSpec.Builder>> handleQueryParam(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final VariableElement parameter,
            final Result<QueryParamMirror> unvQueryParam,
            final Result<Optional<DefaultValueMirror>> unvDefaultValue) {
        return Result.combineDifferent(unvQueryParam, unvDefaultValue).map(v -> {
            final QueryParamMirror queryParam = unvQueryParam.get();
            final Optional<DefaultValueMirror> defaultValue = unvDefaultValue.get();

            final Consumer<ChainStatement> get = stmt -> {
                stmt.add(".getQueryParameter($S)", queryParam.getValue());
            };

            final Consumer<ChainStatement> handleAbsent = stmt -> {
                if (defaultValue.isPresent()) {
                    stmt.add(".orElseGet($N.provideDefaultQueryParameter($S, $S))", ctx,
                            queryParam.getValue(), defaultValue.get().getValue());
                } else {
                    stmt.add(".orElseThrow(() -> new $T($S))", utils.rsMissingQueryParameter(),
                            queryParam.getValue());
                }
            };

            final Consumer<ChainStatement> getList = stmt -> {
                stmt.add(".getAllQueryParameters($S)", queryParam.getValue());
            };

            return provideArgument(ctx, variables, parameter, get, handleAbsent,
                    Optional.of(getList));
        });
    }

    private Result<Consumer<MethodSpec.Builder>> handleHeaderParam(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final VariableElement parameter,
            final Result<HeaderParamMirror> unvHeaderParam,
            final Result<Optional<DefaultValueMirror>> unvDefaultValue) {
        return Result.combineDifferent(unvHeaderParam, unvDefaultValue).map(v -> {
            final HeaderParamMirror headerParam = unvHeaderParam.get();
            final Optional<DefaultValueMirror> defaultValue = unvDefaultValue.get();

            final Consumer<ChainStatement> get = stmt -> {
                stmt.add(".getHeaderParameter($S)", headerParam.getValue());
            };

            final Consumer<ChainStatement> handleAbsent = stmt -> {
                if (defaultValue.isPresent()) {
                    stmt.add(".orElseGet($N.provideDefaultQueryParameter($S, $S))", ctx,
                            headerParam.getValue(), defaultValue.get().getValue());
                } else {
                    stmt.add(".orElseThrow(() -> new $T($S))", utils.rsMissingHeaderParameter(),
                            headerParam.getValue());
                }
            };

            final Consumer<ChainStatement> getList = stmt -> {
                stmt.add(".getAllHeaderParameters($S)", headerParam.getValue());
            };

            return provideArgument(ctx, variables, parameter, get, handleAbsent,
                    Optional.of(getList));
        });
    }

    private Consumer<MethodSpec.Builder> provideArgument(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final VariableElement variable,
            final Consumer<ChainStatement> get, final Consumer<ChainStatement> handleAbsent) {
        return provideArgument(ctx, variables, variable, get, handleAbsent, Optional.empty());
    }

    private Consumer<MethodSpec.Builder> provideArgument(final ParameterSpec ctx,
            final ImmutableList.Builder<String> variables, final VariableElement variable,
            final Consumer<ChainStatement> get, final Consumer<ChainStatement> handleAbsent,
            final Optional<Consumer<ChainStatement>> getList) {
        final TypeMirror variableTypeMirror = variable.asType();
        final String variableName = variable.getSimpleName().toString();
        final boolean optional = utils.isOptional(variableTypeMirror);
        final boolean list = utils.isList(variableTypeMirror);

        final TypeName variableType;

        if (optional || list) {
            variableType = TypeName.get(utils.firstParameter(variableTypeMirror));
        } else {
            variableType = TypeName.get(variableTypeMirror);
        }

        variables.add(variableName);

        final ChainStatement stmt = new ChainStatement();

        if (optional) {
            stmt.add("final $T<$T> $L = $N", utils.optional(), variableType, variableName, ctx);
            get.accept(stmt);
        } else if (list) {
            stmt.add("final $T<$T> $L = $N", utils.list(), variableType, variableName, ctx);
            getList.orElseThrow(() -> new IllegalStateException("providing list not supported"))
                    .accept(stmt);
        } else {
            stmt.add("final $T $L = $N", variableType, variableName, ctx);
            get.accept(stmt);
        }

        if (optional || list) {
            if (variableType.equals(utils.string())) {
                stmt.add(".map($T::asString)", utils.rsParameter());
            } else if (variableType.equals(utils.shortType())) {
                stmt.add(".map($T::asShort)", utils.rsParameter());
            } else if (variableType.equals(utils.integerType())) {
                stmt.add(".map($T::asInteger)", utils.rsParameter());
            } else if (variableType.equals(utils.longType())) {
                stmt.add(".map($T::asLong)", utils.rsParameter());
            } else if (variableType.equals(utils.uuidType())) {
                stmt.add(".map($T::asUUID)", utils.rsParameter());
            } else {
                stmt.add(".map(v -> v.asType($T.class))", variableType);
            }

            if (list) {
                stmt.add(".collect($T.toList())", utils.collectors());
            }
        } else {
            handleAbsent.accept(stmt);

            if (variableType.equals(utils.string())) {
                stmt.add(".asString()");
            } else if (variableType.equals(utils.shortType())) {
                stmt.add(".asShort()");
            } else if (variableType.equals(utils.integerType())) {
                stmt.add(".asInteger()");
            } else if (variableType.equals(utils.longType())) {
                stmt.add(".asLong()");
            } else if (variableType.equals(utils.uuidType())) {
                stmt.add(".asUUID()");
            } else {
                stmt.add(".asType($T.class)", variableType);
            }
        }

        return builder -> {
            builder.addStatement(stmt.format(), stmt.arguments());
        };
    }

    @Data
    public static class Pair<L, R> {
        private final L left;
        private final R right;
    }
}
