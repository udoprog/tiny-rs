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
import eu.toolchain.rs.processor.annotation.ConsumesMirror;
import eu.toolchain.rs.processor.annotation.ContextMirror;
import eu.toolchain.rs.processor.annotation.DefaultValueMirror;
import eu.toolchain.rs.processor.annotation.HeaderParamMirror;
import eu.toolchain.rs.processor.annotation.PathParamMirror;
import eu.toolchain.rs.processor.annotation.ProducesMirror;
import eu.toolchain.rs.processor.annotation.QueryParamMirror;
import eu.toolchain.rs.processor.annotation.SuspendedMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
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

    public JavaFile process(final TypeElement element) {
        final String packageName = elements.getPackageOf(element).getQualifiedName().toString();

        final List<String> rootPath = path(element);

        final TypeSpec.Builder generated = TypeSpec.classBuilder(utils.bindingName(element));

        generated.addAnnotation(utils.generatedAnnotation());

        final FieldSpec instanceField = FieldSpec
                .builder(TypeName.get(element.asType()), "instance")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .build();

        generated.addModifiers(Modifier.PUBLIC);

        generated.addField(instanceField);

        generated.addMethod(constructor(element, instanceField));

        setupHandlers(element, rootPath, generated, instanceField).forEach(
                h -> h.accept(generated));

        return JavaFile
                .builder(packageName, generated.build())
                .skipJavaLangImports(true)
                .indent("    ")
                .build();
    }

    private List<Consumer<Builder>> setupHandlers(
            final TypeElement element, final List<String> rootPath, final Builder generated,
            final FieldSpec instanceField
    ) {
        final ImmutableList.Builder<Consumer<Builder>> handlers = ImmutableList.builder();

        final LinkedHashSet<TypeMirror> returnTypes = new LinkedHashSet<>();
        final ImmutableList.Builder<ExecutableElement> methods = ImmutableList.builder();

        final List<String> parentConsumes = consumes(element);
        final List<String> parentProduces = produces(element);

        final AtomicInteger constantCount = new AtomicInteger();
        final Map<ParameterizedTypeName, FieldSpec> typeReferenceFields = new HashMap<>();

        final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants =
                typeReference -> typeReferenceFields.computeIfAbsent(typeReference, key -> {
                    final FieldSpec typeReferenceField = FieldSpec
                            .builder(typeReference, "TR" + constantCount.getAndIncrement())
                            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("new $T(){}", typeReference)
                            .build();

                    generated.addField(typeReferenceField);
                    return typeReferenceField;
                });

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
            utils.method(executable).ifPresent(method -> {
                final Consumer<Builder> endpoint =
                        endpointSetup(executable, instanceField, rootPath, method, parentConsumes,
                                parentProduces, typeReferenceConstants);

                methods.add(executable);
                returnTypes.add(utils.box(executable.getReturnType()));
                handlers.add(endpoint);
            });
        }

        final TypeName routesReturnType = utils.greatestCommonSuperType(returnTypes);

        handlers.add(routesMethod(routesReturnType, methods));

        generated.addSuperinterface(utils.rsRoutesProvider(utils.rsMapping(routesReturnType)));

        return handlers.build();
    }

    private Consumer<Builder> routesMethod(
            final TypeName returnType, final ImmutableList.Builder<ExecutableElement> methods
    ) {
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

    private List<String> path(final Element element) {
        return utils.path(element).map(p -> processPath(p.getValue())).orElse(DEFAULT_PATH);
    }

    private List<String> consumes(final Element element) {
        return utils.consumes(element).map(ConsumesMirror::getValue).orElseGet(ImmutableList::of);
    }

    private List<String> produces(final Element element) {
        return utils.produces(element).map(ProducesMirror::getValue).orElseGet(ImmutableList::of);
    }

    private List<String> processPath(final String input) {
        final String trimmed = SLASH.trimFrom(input);

        if ("".equals(trimmed)) {
            return DEFAULT_PATH;
        }

        return SLASH_SPLITTER.splitToList(trimmed);
    }

    private MethodSpec constructor(final TypeElement element, final FieldSpec instanceField) {
        final ParameterSpec instance = ParameterSpec
                .builder(TypeName.get(element.asType()), "instance")
                .addModifiers(Modifier.FINAL)
                .build();

        final MethodSpec.Builder b = MethodSpec.constructorBuilder();
        b.addModifiers(Modifier.PUBLIC);
        b.addParameter(instance);
        b.addStatement("this.$N = $N", instanceField, instance);

        if (utils.rsInjectBinding(element).isPresent()) {
            b.addAnnotation(utils.injectAnnotation());
        }

        return b.build();
    }

    private Consumer<TypeSpec.Builder> endpointSetup(
            final ExecutableElement endpoint, final FieldSpec instanceField,
            final List<String> root, final String method, final List<String> parentConsumes,
            final List<String> parentProduces,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        final MethodSpec handler = handlerMethod(endpoint, instanceField, typeReferenceConstants);

        final MethodSpec mapping =
                mappingMethod(endpoint, root, method, parentConsumes, parentProduces);

        return builder -> {
            builder.addMethod(handler);
            builder.addMethod(mapping);
        };
    }

    private MethodSpec mappingMethod(
            final ExecutableElement endpoint, final List<String> root, final String method,
            final List<String> parentConsumes, final List<String> parentProduces
    ) {
        final List<String> path = path(endpoint);

        final List<String> consumes = ImmutableList.<String>builder()
                .addAll(parentConsumes)
                .addAll(consumes(endpoint))
                .build();

        final List<String> produces = ImmutableList.<String>builder()
                .addAll(parentProduces)
                .addAll(produces(endpoint))
                .build();

        final ChainStatement stmt = new ChainStatement().add("return ");

        final TypeName returnTypeName = TypeName.get(utils.box(endpoint.getReturnType()));

        stmt.add("$T.<$T>builder()", utils.rsMappingRaw(), returnTypeName);

        stmt.add(".method($S)", method);
        stmt.addVarString(".path(%s)", ImmutableList.copyOf(Iterables.concat(root, path)));

        if (utils.isVoid(endpoint.getReturnType())) {
            stmt.add(".voidHandle(this::$L)", endpoint.getSimpleName().toString());
        } else {
            stmt.add(".handle(this::$L)", endpoint.getSimpleName().toString());
        }

        stmt.addVarString(".consumes(%s)", consumes);
        stmt.addVarString(".produces(%s)", produces);
        stmt.add(".returnType(new $T(){})", utils.rsTypeReference(returnTypeName));
        stmt.add(".build()");

        final MethodSpec.Builder mapping = MethodSpec.methodBuilder(utils.mappingMethod(endpoint));
        mapping.addModifiers(Modifier.PUBLIC);
        mapping.returns(utils.rsMapping(returnTypeName));

        mapping.addStatement(stmt.format(), stmt.arguments());
        return mapping.build();
    }

    private MethodSpec handlerMethod(
            final ExecutableElement method, final FieldSpec instanceField,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        final MethodSpec.Builder handler =
                MethodSpec.methodBuilder(method.getSimpleName().toString());

        /* re-declare thrown exceptions */
        method.getThrownTypes().forEach(thrownType -> {
            handler.addException(TypeName.get(thrownType));
        });

        final ParameterSpec ctx =
                ParameterSpec.builder(utils.rsRequestContext(), "ctx", Modifier.FINAL).build();

        final ImmutableList.Builder<Consumer<MethodSpec.Builder>> consumers =
                ImmutableList.builder();
        final ImmutableList.Builder<String> variables = ImmutableList.builder();
        final AtomicInteger payloadParameters = new AtomicInteger();

        for (final VariableElement parameter : method.getParameters()) {
            final Optional<DefaultValueMirror> defaultValue =
                    utils.defaultValue(parameter).map(Optional::of).orElseGet(Optional::empty);

            final List<Consumer<MethodSpec.Builder>> consumer = new ArrayList<>();

            utils.pathParam(parameter).ifPresent(pathParam -> {
                consumer.add(handlePathParam(ctx, variables, parameter, pathParam, defaultValue,
                        typeReferenceConstants));
            });

            utils.queryParam(parameter).ifPresent(queryParam -> {
                consumer.add(handleQueryParam(ctx, variables, parameter, queryParam, defaultValue,
                        typeReferenceConstants));
            });

            utils.headerParam(parameter).ifPresent(headerParam -> {
                consumer.add(handleHeaderParam(ctx, variables, parameter, headerParam, defaultValue,
                        typeReferenceConstants));
            });

            utils.suspended(parameter).ifPresent(suspended -> {
                consumer.add(handleSuspended(ctx, variables, parameter, suspended));
            });

            utils.context(parameter).ifPresent(context -> {
                consumer.add(handleContext(ctx, variables, parameter, context));
            });

            if (TypeName.get(parameter.asType()).equals(utils.rsRequestContext())) {
                variables.add("ctx");
                consumer.add(builder -> {
                    // do nothing
                });
            }

            if (consumer.size() > 1) {
                throw new BrokenElement(
                        "Only one of @PathParam, @QueryParam, or @HeaderParam may be present " +
                                "at the same time", parameter);
            }

            if (consumer.isEmpty()) {
                payloadParameters.incrementAndGet();
                consumer.add(
                        handlePayload(ctx, variables, payloadParameters, parameter, defaultValue,
                                typeReferenceConstants));
            }

            consumers.addAll(consumer);
        }

        handler.addParameter(ctx);
        handler.addModifiers(Modifier.PUBLIC);
        handler.returns(TypeName.get(method.getReturnType()));

        for (final Consumer<MethodSpec.Builder> factory : consumers.build()) {
            factory.accept(handler);
        }

        handler.addStatement((utils.isVoid(method.getReturnType()) ? "" : "return ") + "$N.$L($L)",
                instanceField, method.getSimpleName().toString(),
                PARAMETER_JOINER.join(variables.build()));

        return handler.build();
    }

    private Consumer<MethodSpec.Builder> handlePayload(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final AtomicInteger payloadParameters, final VariableElement parameter,
            final Optional<DefaultValueMirror> defaultValue,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        if (payloadParameters.get() > 1) {
            throw new BrokenElement("There must only be one payload argument", parameter);
        }

        if (utils.isList(parameter.asType())) {
            throw new BrokenElement("Payload argument must not be list", parameter);
        }

        final Consumer<ChainStatement> get = stmt -> stmt.add(".getPayload()");

        final Consumer<ChainStatement> handleAbsent = stmt -> {
            if (defaultValue.isPresent()) {
                stmt.add(".orElseGet($N.provideDefaultPayload($S))", ctx, defaultValue.get());
            } else {
                stmt.add(".orElseThrow(() -> new $T())", utils.rsMissingPayload());
            }
        };

        return provideArgument(ctx, variables, parameter, get, handleAbsent,
                typeReferenceConstants);
    }

    private Consumer<MethodSpec.Builder> handlePathParam(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final VariableElement parameter, final PathParamMirror pathParam,
            final Optional<DefaultValueMirror> defaultValue,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        if (utils.isList(parameter.asType())) {
            throw new BrokenElement("Path parameter argument must not be list", parameter);
        }

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

        return provideArgument(ctx, variables, parameter, get, handleAbsent,
                typeReferenceConstants);
    }

    private Consumer<MethodSpec.Builder> handleQueryParam(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final VariableElement parameter, final QueryParamMirror queryParam,
            final Optional<DefaultValueMirror> defaultValue,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
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

        return provideArgument(ctx, variables, parameter, get, handleAbsent, Optional.of(getList),
                typeReferenceConstants);
    }

    private Consumer<MethodSpec.Builder> handleHeaderParam(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final VariableElement parameter, final HeaderParamMirror headerParam,
            final Optional<DefaultValueMirror> defaultValue,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
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

        return provideArgument(ctx, variables, parameter, get, handleAbsent, Optional.of(getList),
                typeReferenceConstants);
    }

    private Consumer<MethodSpec.Builder> handleSuspended(
            final ParameterSpec ctx, ImmutableList.Builder<String> variables,
            VariableElement parameter, SuspendedMirror suspended
    ) {
        if (!TypeName.get(parameter.asType()).equals(utils.asyncResponse())) {
            throw new BrokenElement("@Suspended arguments must be of type AsyncResponse",
                    parameter);
        }

        variables.add(parameter.getSimpleName().toString());

        return builder -> {
            builder.addStatement("final $T $L = $N.asSuspended()", utils.asyncResponse(),
                    parameter.getSimpleName().toString(), ctx);
        };
    }

    private Consumer<MethodSpec.Builder> handleContext(
            final ParameterSpec ctx, ImmutableList.Builder<String> variables,
            VariableElement parameter, ContextMirror context
    ) {
        variables.add(parameter.getSimpleName().toString());

        final TypeName contextType = TypeName.get(parameter.asType());

        if (contextType instanceof ParameterizedTypeName) {
            throw new BrokenElement("@Context arguments must not be parameterized", parameter);
        }

        return builder -> {
            builder.addStatement("final $T $L = $N.getContext($T.class)", contextType,
                    parameter.getSimpleName().toString(), ctx, contextType);
        };
    }

    private Consumer<MethodSpec.Builder> provideArgument(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final VariableElement variable, final Consumer<ChainStatement> get,
            final Consumer<ChainStatement> handleAbsent,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        return provideArgument(ctx, variables, variable, get, handleAbsent, Optional.empty(),
                typeReferenceConstants);
    }

    private Consumer<MethodSpec.Builder> provideArgument(
            final ParameterSpec ctx, final ImmutableList.Builder<String> variables,
            final VariableElement variable, final Consumer<ChainStatement> get,
            final Consumer<ChainStatement> handleAbsent,
            final Optional<Consumer<ChainStatement>> getList,
            final Function<ParameterizedTypeName, FieldSpec> typeReferenceConstants
    ) {
        final TypeMirror variableTypeMirror = variable.asType();
        final String variableName = variable.getSimpleName().toString();
        final boolean optional = utils.isOptional(variableTypeMirror);
        final boolean list = utils.isList(variableTypeMirror);

        final TypeName variableType;

        if (optional || list) {
            variableType = TypeName.get(utils
                    .firstParameter(variableTypeMirror)
                    .orElseThrow(() -> new RuntimeException(
                            "No parameter for type: " + variableTypeMirror)));
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
            getList
                    .orElseThrow(() -> new IllegalStateException("providing list not supported"))
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
                stmt.add(".map(v -> v.asType($N))",
                        typeReferenceConstants.apply(utils.rsTypeReference(variableType)));
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
                stmt.add(".asType($N)",
                        typeReferenceConstants.apply(utils.rsTypeReference(variableType)));
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
