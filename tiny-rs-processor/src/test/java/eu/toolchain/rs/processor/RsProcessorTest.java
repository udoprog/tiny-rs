package eu.toolchain.rs.processor;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.Test;

import com.google.testing.compile.JavaFileObjects;

public class RsProcessorTest {
    @Test
    public void testQueryParams() {
        verifyProcessorFor("QueryParams");
    }

    @Test
    public void testPathParams() {
        verifyProcessorFor("PathParams");
    }

    @Test
    public void testHeaderParams() {
        verifyProcessorFor("HeaderParams");
    }

    @Test
    public void testDefaultValues() {
        verifyProcessorFor("DefaultValues");
    }

    @Test
    public void testConsumesProduces() {
        verifyProcessorFor("ConsumesProduces");
    }

    @Test
    public void testReturnTypeElision() {
        verifyProcessorFor("ReturnTypeElision");
    }

    @Test
    public void testGenericReturnTypeElision() {
        verifyProcessorFor("GenericReturnTypeElision");
    }

    @Test
    public void testGenericWildcard() {
        verifyProcessorFor("GenericWildcard");
    }

    @Test
    public void testInjectBindings() {
        verifyProcessorFor("InjectBindings");
    }

    @Test
    public void testSpecialParameters() {
        verifyProcessorFor("SpecialParameters");
    }

    @Test
    public void testThrowing() {
        verifyProcessorFor("Throwing");
    }

    @Test
    public void testContextArgument() {
        verifyProcessorFor("ContextArgument");
    }

    @Test
    public void testPayload() {
        verifyProcessorFor("Payload");
    }

    static void verifyProcessorFor(String name) {
        verifyProcessorFor(name, String.format(RsUtils.BINDING_NAME_FORMAT, name));
    }

    static void verifyProcessorFor(String sourceName, String first, String... rest) {
        final JavaFileObject source = resourcePathFor(sourceName);
        final JavaFileObject firstSerializer = resourcePathFor(first);

        final JavaFileObject restSerializers[] = new JavaFileObject[rest.length];

        for (int i = 0; i < rest.length; i++) {
            restSerializers[i] = resourcePathFor(rest[i]);
        }

        assert_().about(javaSource()).that(source).processedWith(new RsProcessor()).compilesWithoutError()
                .and().generatesSources(firstSerializer, restSerializers);
    }

    static void verifyFailingSerializer(String name) {
        final JavaFileObject source = resourcePathFor(name);
        assert_().about(javaSource()).that(source).processedWith(new RsProcessor()).failsToCompile();
    }

    static JavaFileObject resourcePathFor(String name) {
        return JavaFileObjects.forResource(String.format("processortests/%s.java", name));
    }
}
