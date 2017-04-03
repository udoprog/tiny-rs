package eu.toolchain.rs.processor;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import eu.toolchain.rs.processor.annotation.ContextMirror;
import eu.toolchain.rs.processor.annotation.DefaultValueMirror;
import eu.toolchain.rs.processor.annotation.HeaderParamMirror;
import eu.toolchain.rs.processor.annotation.PathMirror;
import eu.toolchain.rs.processor.annotation.PathParamMirror;
import eu.toolchain.rs.processor.annotation.QueryParamMirror;
import eu.toolchain.rs.processor.annotation.SuspendedMirror;
import eu.toolchain.rs.processor.result.Result;
import java.util.ArrayList;
import java.util.Collections;
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
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RsClassProcessor {
    /* skips methods having any of the given modifiers */
    public static final ImmutableSet<Modifier> SKIP_MODIFIERS =
            ImmutableSet.of(Modifier.PRIVATE, Modifier.PROTECTED, Modifier.STATIC);
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
                /* match methods */
                if (enclosed.getKind() != ElementKind.METHOD) {
                    continue;
                }

                /* skip inaccessible methods (e.g. static, private) */
                if (!Collections.disjoint(enclosed.getModifiers(), SKIP_MODIFIERS)) {
                    continue;
                }

                final ExecutableElement executable = (ExecutableElement) enclosed;

                /* annotated with a method annotation, like @GET */
                utils.method(executable).ifPresent(resultMethod -> {
                    final Result<Consumer<Builder>> endpoint = resultMethod.flatMap(
                            method -> endpointSetup(executable, instanceField, rootPath, method));

                    methods.add(executable);
                    returnTypes.add(utils.box(executable.getReturnType()));
                    unverifiedHandlers.add(endpoint);
                });
            }

            final TypeName routesReturnType = utils.greatestCommonSuperType(returnTypes);

            unverifiedHandlers.add(Result.ok(routesMethod(routesReturnType, methods)));

            generated.addSuperinterface(utils.rsRoutesProvider(utils.rsMapping(routesReturnType)));

            return Result.combine(unverifiedHandlers.build()).map(handlers -> {
                handlers.forEach(h -> h.accept(generated));

                return JavaFile.builder(packageName, generated.build()).skipJavaLangImports(true)
                        .indent("    ").build();
            });
        });
    }

    private Consumer<Builder> routesMethod(final TypeName returnType,
            final ImmutableList.Builder<ExecutableElement> methods) {
        return builder -> {
            final MethodSpec.Builder method = MethodSpec.methodBuilder("routes");
            final ParameterizedTypeName routesReturnType = utils.list(utils.rsMapping(returnType));

            method.addAnnotation(utils.overrideAnnotation());
            method.addModifiers(Modifier.PUBLIC);

            method.returns(routesReturnType);
            method.addStatement("final $T $L = new $T<>()", routesReturnType, "routes",
                    utils.arrayList());

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

        if (utils.rsInjectBinding(element).isPresent()) {
            b.addAnnotation(utils.injectAnnotation());
        }

        return b.build();
    }

    private Result<Consumer<TypeSpec.Builder>> endpointSetup(final ExecutableElement endpoint,
            final FieldSpec instanceField, final List<String> root, final String method) {
        final Result<MethodSpec> resultHandler = handlerMethod(endpoint, instanceField);
        final Result<MethodSpec> resultMapping =
                mappingMethod(endpoint, instanceField, root, method);

        return Result.combineDifferent(resultHandler, resultMapping).map(v -> {
            final MethodSpec handler = resultHandler.get();
            final MethodSpec mapping = resultMapping.get();

            return builder -> {
                builder.addMethod(handler);
                builder.addMethod(mapping);
            };
        });
    }

    private Result<MethodSpec> mappingMethod(final ExecutableElement endpoint,
            final FieldSpec instanceField, final List<String> root, final String method) {
        final Result<List<String>> resultPath = path(endpoint);
        final Result<List<String>> resultConsumes = consumes(endpoint);
        final Result<List<String>> resultProduces = produces(endpoint);

        final Result<?> combined =
                Result.combineDifferent(resultPath, resultConsumes, resultProduces);

        return combined.map(v -> {
            final List<String> path = resultPath.get();
            final List<String> consumes = resultConsumes.get();
            final List<String> produces = resultProduces.get();

            final ChainStatement stmt = new ChainStatement().add("return ");

            stmt.add("$T.<$T>builder()", utils.rsMappingRaw(),
                    TypeName.get(utils.box(endpoint.getReturnType())));

            stmt.add(".method($S)", method);
            stmt.addVarString(".path(%s)", ImmutableList.copyOf(Iterables.concat(root, path)));

            if (utils.isVoid(endpoint.getReturnType())) {
                stmt.add(".voidHandle(this::$L)", endpoint.getSimpleName().toString());
            } else {
                stmt.add(".handle(this::$L)", endpoint.getSimpleName().toString());
            }

            stmt.addVarString(".consumes(%s)", consumes);
            stmt.addVarString(".produces(%s)", produces);
            stmt.add(".build()");

            final MethodSpec.Builder mapping =
                    MethodSpec.methodBuilder(utils.mappingMethod(endpoint));
            mapping.addModifiers(Modifier.PUBLIC);
            mapping.returns(utils.rsMapping(TypeName.get(utils.box(endpoint.getReturnType()))));

            mapping.addStatement(stmt.format(), stmt.arguments());
            return mapping.build();
        });
    }

    private Result<MethodSpec> handlerMethod(final ExecutableElement method,
            final FieldSpec instanceField) {
        final MethodSpec.Builder handler =
                MethodSpec.methodBuilder(method.getSimpleName().toString());

        /* re-declare thrown exceptions */
        method.getThrownTypes().forEach(thrownType -> {
            handler.addException(TypeName.get(thrownType));
        });

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

            utils.suspended(parameter).ifPresent(suspended -> {
                consumer.add(handleSuspended(ctx, variables, parameter, suspended));
            });

            utils.context(parameter).ifPresent(context -> {
                consumer.add(handleContext(ctx, variables, parameter, context));
            });

            if (TypeName.get(parameter.asType()).equals(utils.rsRequestContext())) {
                variables.add("ctx");
                consumer.add(Result.ok(builder -> {
                    // do nothing
                }));
            }

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

            handler.addStatement(
                    (utils.isVoid(method.getReturnType()) ? "" : "return ") + "$N.$L($L)",
                    instanceField, method.getSimpleName().toString(),
                    PARAMETER_JOINER.join(variables.build()));

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
                    stmt.add(".orElseThrow(() -> new $T())", utils.rsMissingPayload());
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

    private Result<Consumer<MethodSpec.Builder>> handleSuspended(final ParameterSpec ctx,
            ImmutableList.Builder<String> variables, VariableElement parameter,
            Result<SuspendedMirror> suspended) {
        if (!TypeName.get(parameter.asType()).equals(utils.asyncResponse())) {
            return Result.brokenElement("@Suspended arguments must be of type AsyncResponse",
                    parameter);
        }

        variables.add(parameter.getSimpleName().toString());

        return Result.ok(builder -> {
            builder.addStatement("final $T $L = $N.asSuspended()", utils.asyncResponse(),
                    parameter.getSimpleName().toString(), ctx);
        });
    }

    private Result<Consumer<MethodSpec.Builder>> handleContext(final ParameterSpec ctx,
            ImmutableList.Builder<String> variables, VariableElement parameter,
            Result<ContextMirror> context) {
        variables.add(parameter.getSimpleName().toString());

        final TypeName contextType = TypeName.get(parameter.asType());

        if (contextType instanceof ParameterizedTypeName) {
            return Result.brokenElement("@Context arguments must not be parameterized", parameter);
        }

        return Result.ok(builder -> {
            builder.addStatement("final $T $L = $N.getContext($T.class)", contextType,
                    parameter.getSimpleName().toString(), ctx, contextType);
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
            variableType = TypeName.get(utils.firstParameter(variableTypeMirror).orElseThrow(
                    () -> new RuntimeException("No parameter for type: " + variableTypeMirror)));
        } else {
            variableType = TypeName.get(variableTypeMirror);
        }

        variables.add(variableName);

        final ChainStatement stmt = new ChainStatement();

        if (optional) {
            stmt.add("final $T $L = $N", utils.optional(variableType), variableName, ctx);
            get.accept(stmt);
        } else if (list) {
            stmt.add("final $T $L = $N", utils.list(variableType), variableName, ctx);
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
