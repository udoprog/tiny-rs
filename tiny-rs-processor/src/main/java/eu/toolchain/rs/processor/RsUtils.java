package eu.toolchain.rs.processor;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;
import eu.toolchain.rs.RsInjectBinding;
import eu.toolchain.rs.RsMapping;
import eu.toolchain.rs.RsMissingHeaderParameter;
import eu.toolchain.rs.RsMissingPathParameter;
import eu.toolchain.rs.RsMissingPayload;
import eu.toolchain.rs.RsMissingQueryParameter;
import eu.toolchain.rs.RsParameter;
import eu.toolchain.rs.RsRequestContext;
import eu.toolchain.rs.RsRoutesProvider;
import eu.toolchain.rs.RsTypeReference;
import eu.toolchain.rs.processor.annotation.ConsumesMirror;
import eu.toolchain.rs.processor.annotation.ContextMirror;
import eu.toolchain.rs.processor.annotation.DefaultValueMirror;
import eu.toolchain.rs.processor.annotation.HeaderParamMirror;
import eu.toolchain.rs.processor.annotation.PathMirror;
import eu.toolchain.rs.processor.annotation.PathParamMirror;
import eu.toolchain.rs.processor.annotation.ProducesMirror;
import eu.toolchain.rs.processor.annotation.QueryParamMirror;
import eu.toolchain.rs.processor.annotation.RsInjectBindingMirror;
import eu.toolchain.rs.processor.annotation.SuspendedMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Generated;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RsUtils {
    public static final String BINDING_NAME_FORMAT = "%s_Binding";
    public static final String MAPPING_METHOD_FORMAT = "%s_mapping";

    public static final Joiner UNDERSCORE_JOINER = Joiner.on('_');

    public static final String RS_PROCESSOR = RsProcessor.class.getCanonicalName();

    /* tiny-rs API types */
    public static final String RS_MAPPING = RsMapping.class.getCanonicalName();
    public static final String RS_TYPE_REFERENCE = RsTypeReference.class.getCanonicalName();
    public static final String RS_REQUEST_CONTEXT = RsRequestContext.class.getCanonicalName();
    public static final String RS_PARAMETER = RsParameter.class.getCanonicalName();
    public static final String RS_MISSING_PATH_PARAMETER =
            RsMissingPathParameter.class.getCanonicalName();
    public static final String RS_MISSING_QUERY_PARAMETER =
            RsMissingQueryParameter.class.getCanonicalName();
    public static final String RS_MISSING_HEADER_PARAMETER =
            RsMissingHeaderParameter.class.getCanonicalName();
    public static final String RS_MISSING_PAYLOAD = RsMissingPayload.class.getCanonicalName();
    public static final String RS_ROUTES_PROVIDER = RsRoutesProvider.class.getCanonicalName();
    public static final String RS_INJECT_BINDING = RsInjectBinding.class.getCanonicalName();

    /* javax.ws.rs API types */
    public static final String PATH = Path.class.getCanonicalName();
    public static final String CONSUMES = Consumes.class.getCanonicalName();
    public static final String PRODUCES = Produces.class.getCanonicalName();
    public static final String PATH_PARAM = PathParam.class.getCanonicalName();
    public static final String QUERY_PARAM = QueryParam.class.getCanonicalName();
    public static final String HEADER_PARAM = HeaderParam.class.getCanonicalName();
    public static final String DEFAULT_VALUE = DefaultValue.class.getCanonicalName();
    public static final String GET = GET.class.getCanonicalName();
    public static final String POST = POST.class.getCanonicalName();
    public static final String PUT = PUT.class.getCanonicalName();
    public static final String DELETE = DELETE.class.getCanonicalName();
    public static final String OPTIONS = OPTIONS.class.getCanonicalName();

    /* javax.ws.rs.container types */
    public static final String SUSPENDED = Suspended.class.getCanonicalName();
    public static final String ASYNC_RESPONSE = AsyncResponse.class.getCanonicalName();

    /* javax.ws.rs.core types */
    public static final String CONTEXT = Context.class.getCanonicalName();

    /* annotations to look for */
    public static final Set<String> ANNOTATION_NAMES = ImmutableSet.copyOf(
            Stream.of(PATH, PRODUCES, CONSUMES, GET, POST, PUT, DELETE, OPTIONS).iterator());

    /* other useful types */
    public static final String VOID = Void.class.getCanonicalName();

    public static final String LIST = List.class.getCanonicalName();
    public static final String ARRAY_LIST = ArrayList.class.getCanonicalName();
    public static final String OPTIONAL = Optional.class.getCanonicalName();
    public static final String FUNCTION = Function.class.getCanonicalName();

    public static final String COLLECTORS = Collectors.class.getCanonicalName();

    public static final String STRING = String.class.getCanonicalName();
    public static final String SHORT = Short.class.getCanonicalName();
    public static final String INTEGER = Integer.class.getCanonicalName();
    public static final String LONG = Long.class.getCanonicalName();
    public static final String UUID = UUID.class.getCanonicalName();

    public static final String GENERATED_PACKAGE = Generated.class.getPackage().getName();
    public static final String GENERATED = Generated.class.getSimpleName();

    public static final String OVERRIDE_PACKAGE = Override.class.getPackage().getName();
    public static final String OVERRIDE = Override.class.getSimpleName();

    public static final String INJECT_PACKAGE = "javax.inject";
    public static final String INJECT = "Inject";

    private final Types types;
    private final Elements elements;
    /* annotations to scan for */
    private final Set<TypeElement> annotations;

    public RsUtils(Types types, Elements elements) {
        this.types = types;
        this.elements = elements;
        this.annotations = ImmutableSet.copyOf(
                ANNOTATION_NAMES.stream().map(elements::getTypeElement).iterator());
    }

    public boolean isPrimitive(TypeMirror type) {
        return type instanceof PrimitiveType;
    }

    public TypeMirror box(TypeMirror type) {
        if (type instanceof PrimitiveType) {
            return types.boxedClass((PrimitiveType) type).asType();
        }

        if (type instanceof NoType) {
            final NoType no = (NoType) type;

            if (no.getKind() == TypeKind.VOID) {
                return elements.getTypeElement(VOID).asType();
            }
        }

        return type;
    }

    /**
     * Re-fetch the given element from the environment.
     *
     * This might be necessary to update type information which was not available on previous
     * rounds.
     *
     * @param element Element to fetch.
     * @return A refreshed version of the specified element from the environment.
     */
    public TypeElement refetch(TypeElement element) {
        return elements.getTypeElement(element.getQualifiedName());
    }

    public List<AnnotationMirror> getAnnotations(final Element element, final String lookFor) {
        final ImmutableList.Builder<AnnotationMirror> results = ImmutableList.builder();

        for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
            if (!annotation.getAnnotationType().toString().equals(lookFor)) {
                continue;
            }

            results.add(annotation);
        }

        return results.build();
    }

    public AnnotationValues getElementValuesWithDefaults(Element element, AnnotationMirror a) {
        final ImmutableMap.Builder<String, AnnotationValue> builder = ImmutableMap.builder();

        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : elements
                .getElementValuesWithDefaults(a)
                .entrySet()) {
            builder.put(e.getKey().getSimpleName().toString(), e.getValue());
        }

        return new AnnotationValues(element, a, builder.build());
    }

    public <T extends Annotation> Optional<AnnotationMirror> annotation(
            final Element element, final String name
    ) {
        for (final AnnotationMirror a : getAnnotations(element, name)) {
            return Optional.of(a);
        }

        return Optional.empty();
    }

    public Optional<RsInjectBindingMirror> rsInjectBinding(final Element element) {
        return annotation(element, RS_INJECT_BINDING).map(
                a -> RsInjectBindingMirror.getFor(this, element, a));
    }

    public Optional<PathMirror> path(final Element element) {
        return annotation(element, PATH).map(a -> PathMirror.getFor(this, element, a));
    }

    public Optional<ConsumesMirror> consumes(final Element element) {
        return annotation(element, CONSUMES).map(a -> ConsumesMirror.getFor(this, element, a));
    }

    public Optional<ProducesMirror> produces(final Element element) {
        return annotation(element, PRODUCES).map(a -> ProducesMirror.getFor(this, element, a));
    }

    public Optional<DefaultValueMirror> defaultValue(final Element element) {
        return annotation(element, DEFAULT_VALUE).map(
                a -> DefaultValueMirror.getFor(this, element, a));
    }

    public Optional<PathParamMirror> pathParam(final Element element) {
        return annotation(element, PATH_PARAM).map(a -> PathParamMirror.getFor(this, element, a));
    }

    public Optional<QueryParamMirror> queryParam(final Element element) {
        return annotation(element, QUERY_PARAM).map(a -> QueryParamMirror.getFor(this, element, a));
    }

    public Optional<HeaderParamMirror> headerParam(final Element element) {
        return annotation(element, HEADER_PARAM).map(
                a -> HeaderParamMirror.getFor(this, element, a));
    }

    public Optional<SuspendedMirror> suspended(final Element element) {
        return annotation(element, SUSPENDED).map(a -> SuspendedMirror.getFor(this, element, a));
    }

    public Optional<ContextMirror> context(final Element element) {
        return annotation(element, CONTEXT).map(a -> ContextMirror.getFor(this, element, a));
    }

    public Set<TypeElement> annotations() {
        return annotations;
    }

    public boolean isOptional(TypeMirror valueType) {
        if (!(valueType instanceof DeclaredType)) {
            return false;
        }

        final DeclaredType d = (DeclaredType) valueType;
        final TypeElement t = (TypeElement) d.asElement();
        return t.getQualifiedName().toString().equals(OPTIONAL);
    }

    public boolean isVoid(TypeMirror type) {
        return type.getKind() == TypeKind.VOID;
    }

    public boolean isList(TypeMirror valueType) {
        if (!(valueType instanceof DeclaredType)) {
            return false;
        }

        final DeclaredType d = (DeclaredType) valueType;
        final TypeElement t = (TypeElement) d.asElement();
        return t.getQualifiedName().toString().equals(LIST);
    }

    public String bindingName(final Element root) {
        final ImmutableList.Builder<String> parts = ImmutableList.builder();

        Element element = root;

        do {
            if (element.getKind() != ElementKind.CLASS &&
                    element.getKind() != ElementKind.INTERFACE) {
                throw new IllegalArgumentException(
                        String.format("Element is not interface or class (%s)", element));
            }

            if (element.getEnclosingElement().getKind() == ElementKind.CLASS &&
                    !element.getModifiers().contains(Modifier.STATIC)) {
                throw new IllegalArgumentException(
                        String.format("Nested element must be static (%s)", element));
            }

            parts.add(element.getSimpleName().toString());
            element = element.getEnclosingElement();
        } while (element.getKind() != ElementKind.PACKAGE);

        return String.format(BINDING_NAME_FORMAT, UNDERSCORE_JOINER.join(parts.build().reverse()));
    }

    public ClassName rsMappingRaw() {
        return ClassName.get(elements.getTypeElement(RS_MAPPING));
    }

    public ClassName rsTypeReferenceRaw() {
        return ClassName.get(elements.getTypeElement(RS_TYPE_REFERENCE));
    }

    public ParameterizedTypeName rsMapping(final TypeName parameter) {
        return ParameterizedTypeName.get(rsMappingRaw(), parameter);
    }

    public ParameterizedTypeName rsTypeReference(final TypeName parameter) {
        return ParameterizedTypeName.get(rsTypeReferenceRaw(), parameter);
    }

    public ClassName rsRequestContext() {
        return ClassName.get(elements.getTypeElement(RS_REQUEST_CONTEXT));
    }

    public ClassName rsParameter() {
        return ClassName.get(elements.getTypeElement(RS_PARAMETER));
    }

    public ClassName rsMissingPathParameter() {
        return ClassName.get(elements.getTypeElement(RS_MISSING_PATH_PARAMETER));
    }

    public ClassName rsMissingQueryParameter() {
        return ClassName.get(elements.getTypeElement(RS_MISSING_QUERY_PARAMETER));
    }

    public ClassName rsMissingHeaderParameter() {
        return ClassName.get(elements.getTypeElement(RS_MISSING_HEADER_PARAMETER));
    }

    public ClassName rsMissingPayload() {
        return ClassName.get(elements.getTypeElement(RS_MISSING_PAYLOAD));
    }

    public ParameterizedTypeName rsRoutesProvider(final TypeName parameter) {
        final ClassName raw = ClassName.get(elements.getTypeElement(RS_ROUTES_PROVIDER));
        return ParameterizedTypeName.get(raw, parameter);
    }

    public ParameterizedTypeName optional(final TypeName parameter) {
        final ClassName raw = ClassName.get(elements.getTypeElement(OPTIONAL));
        return ParameterizedTypeName.get(raw, parameter);
    }

    public ParameterizedTypeName list(final TypeName parameter) {
        final ClassName raw = ClassName.get(elements.getTypeElement(LIST));
        return ParameterizedTypeName.get(raw, parameter);
    }

    public ClassName function() {
        return ClassName.get(elements.getTypeElement(FUNCTION));
    }

    public ClassName arrayList() {
        return ClassName.get(elements.getTypeElement(ARRAY_LIST));
    }

    public ClassName collectors() {
        return ClassName.get(elements.getTypeElement(COLLECTORS));
    }

    public ClassName string() {
        return ClassName.get(elements.getTypeElement(STRING));
    }

    public ClassName shortType() {
        return ClassName.get(elements.getTypeElement(SHORT));
    }

    public ClassName integerType() {
        return ClassName.get(elements.getTypeElement(INTEGER));
    }

    public ClassName longType() {
        return ClassName.get(elements.getTypeElement(LONG));
    }

    public ClassName uuidType() {
        return ClassName.get(elements.getTypeElement(UUID));
    }

    public ClassName asyncResponse() {
        return ClassName.get(elements.getTypeElement(ASYNC_RESPONSE));
    }

    /**
     * Gets the first generic parameter for the given type.
     *
     * @param parent The type to get the parameter for.
     * @return The first type, if available.
     */
    public Optional<? extends TypeMirror> firstParameter(final TypeMirror parent) {
        if (!(parent instanceof DeclaredType)) {
            throw new IllegalArgumentException("Not a declared type: " + parent);
        }

        return DeclaredType.class.cast(parent).getTypeArguments().stream().findFirst();
    }

    /**
     * Get the mapping method name for the given element.
     *
     * @param method The element to get the name for.
     * @return The mapping name of the element.
     */
    public String mappingMethod(final ExecutableElement method) {
        return String.format(MAPPING_METHOD_FORMAT, method.getSimpleName());
    }

    /**
     * Find the greatest common super-type between a list of types.
     *
     * Most likely there will be more than one candidate. Matching types are looked for in
     * breadth-first order, so the first common type found will be the one returned.
     *
     * @param types Types to match between.
     * @return A greatest common super type.
     */
    public TypeName greatestCommonSuperType(final LinkedHashSet<TypeMirror> types) {
        return greatestCommonSuperType(types, ImmutableSet.of());
    }

    /**
     * Get first method annotation from the given element. Fails with broken result on multiple
     * annotations present, returns empty if none.
     *
     * @param element Element to get annotations for.
     * @return An optional result containing the annotation.
     */
    public Optional<String> method(final Element element) {
        final List<String> methods = new ArrayList<>();

        annotation(element, GET).ifPresent(v -> methods.add(HttpMethod.GET));
        annotation(element, POST).ifPresent(v -> methods.add(HttpMethod.POST));
        annotation(element, PUT).ifPresent(v -> methods.add(HttpMethod.PUT));
        annotation(element, DELETE).ifPresent(v -> methods.add(HttpMethod.DELETE));
        annotation(element, OPTIONS).ifPresent(v -> methods.add(HttpMethod.OPTIONS));

        if (methods.size() > 1) {
            throw new BrokenElement(
                    "Only one of @GET, @POST, @PUT, @DELETE, or @OPTIONS may be present", element);
        }

        return methods.stream().findFirst();
    }

    public AnnotationSpec generatedAnnotation() {
        return AnnotationSpec
                .builder(ClassName.get(GENERATED_PACKAGE, GENERATED))
                .addMember("value", "$S", RS_PROCESSOR)
                .build();
    }

    public AnnotationSpec injectAnnotation() {
        return AnnotationSpec.builder(ClassName.get(INJECT_PACKAGE, INJECT)).build();
    }

    public AnnotationSpec overrideAnnotation() {
        return AnnotationSpec.builder(ClassName.get(OVERRIDE_PACKAGE, OVERRIDE)).build();
    }

    private TypeName greatestCommonSuperType(
            final LinkedHashSet<TypeMirror> types, final Set<TypeElement> seen
    ) {
        if (types.size() == 1) {
            return TypeName.get(types.iterator().next());
        }

        final List<LinkedHashMap<TypeElement, DeclaredType>> all =
                ImmutableList.copyOf(types.stream().map(this::mapHierarchy).iterator());

        if (all.isEmpty()) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }

        final Iterator<LinkedHashMap<TypeElement, DeclaredType>> it = all.iterator();
        final LinkedHashMap<TypeElement, DeclaredType> first = it.next();

        while (it.hasNext()) {
            first.keySet().retainAll(it.next().keySet());
        }

        if (first.isEmpty()) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }

        final TypeElement raw = first.keySet().iterator().next();

        if (seen.contains(raw)) {
            return WildcardTypeName.subtypeOf(TypeName.OBJECT);
        }

        if (raw.getTypeParameters().isEmpty()) {
            return WildcardTypeName.subtypeOf(ClassName.get(raw));
        }

        final List<TypeName> parameters = new ArrayList<>();

        for (int index = 0; index < raw.getTypeParameters().size(); index++) {
            final LinkedHashSet<TypeMirror> parameterTypes = new LinkedHashSet<>();

            for (final LinkedHashMap<TypeElement, DeclaredType> sub : all) {
                final DeclaredType right = sub.get(raw);
                parameterTypes.add(right.getTypeArguments().get(index));
            }

            if (parameterTypes.size() == 1) {
                parameters.add(
                        WildcardTypeName.subtypeOf(TypeName.get(parameterTypes.iterator().next())));
                continue;
            }

            final Set<TypeElement> nextSeen =
                    ImmutableSet.<TypeElement>builder().addAll(seen).add(raw).build();
            final TypeName type = greatestCommonSuperType(parameterTypes, nextSeen);
            parameters.add(type);
        }

        final TypeName parameterized =
                ParameterizedTypeName.get(ClassName.get(raw), parameters.toArray(new TypeName[0]));
        return WildcardTypeName.subtypeOf(parameterized);
    }

    private LinkedHashMap<TypeElement, DeclaredType> mapHierarchy(final TypeMirror start) {
        final LinkedHashMap<TypeElement, DeclaredType> set = new LinkedHashMap<>();

        final LinkedList<TypeMirror> queue = new LinkedList<>();

        queue.add(start);

        while (!queue.isEmpty()) {
            final TypeMirror next = queue.pop();

            if (!(next instanceof DeclaredType)) {
                continue;
            }

            final DeclaredType declared = (DeclaredType) next;
            final TypeElement element = (TypeElement) declared.asElement();

            set.remove(element);
            set.put(element, declared);

            queue.add(element.getSuperclass());
            queue.addAll(element.getInterfaces());
        }

        return set;
    }
}
