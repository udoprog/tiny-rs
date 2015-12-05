package eu.toolchain.rs.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.squareup.javapoet.JavaFile;

import eu.toolchain.rs.processor.result.Result;
import lombok.Data;

@AutoService(Processor.class)
public class RsProcessor extends AbstractProcessor {
    private Filer filer;
    private Messager messager;
    private RsUtils utils;
    private RsClassProcessor classProcessor;

    private final List<DeferredProcessing> deferred = new ArrayList<>();

    @Override
    public void init(final ProcessingEnvironment env) {
        super.init(env);

        filer = env.getFiler();
        messager = new PrefixingMessager("tiny-rs", env.getMessager());

        final Elements elements = env.getElementUtils();
        final Types types = env.getTypeUtils();

        utils = new RsUtils(types, elements);
        classProcessor = new RsClassProcessor(types, elements, utils);

        if (env.getClass().getPackage().getName().startsWith("org.eclipse.jdt.")) {
            warnAboutBugEclipse300408();
        }
    }

    /**
     * Eclipse JDT does not preserve the original order of type fields, causing some Processor
     * assumptions to fail.
     */
    void warnAboutBugEclipse300408() {
        messager.printMessage(Diagnostic.Kind.WARNING,
                "processor might not work properly in Eclipse < 3.5, "
                        + "see https://bugs.eclipse.org/bugs/show_bug.cgi?id=300408");
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment env) {
        final ImmutableSet.Builder<DeferredProcessing> elementsToProcess = ImmutableSet.builder();

        if (env.processingOver()) {
            for (final DeferredProcessing d : deferred) {
                d.getBroken().get().writeError(messager);
            }

            return false;
        }

        // failing TypeElement's from last round
        if (!deferred.isEmpty()) {
            elementsToProcess
                    .addAll(deferred.stream().map(DeferredProcessing.refresh(utils)).iterator());
            deferred.clear();
        }

        elementsToProcess.addAll(discoverNewElements(env));

        final List<Processed> processed = processElements(elementsToProcess.build());

        for (final Processed p : processed) {
            final Result<JavaFile> serializer = p.getFile();

            if (!serializer.isOk()) {
                deferred.add(p.processing.withBroken(serializer));
                continue;
            }

            try {
                serializer.get().writeTo(filer);
            } catch (final Exception e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Failed to write:\n" + Throwables.getStackTraceAsString(e),
                        p.getProcessing().getElement());
            }
        }

        return false;
    }

    private Iterable<DeferredProcessing> discoverNewElements(final RoundEnvironment env) {
        final ImmutableSet.Builder<DeferredProcessing> newElements = ImmutableSet.builder();

        final Iterator<Element> elements = Iterators.concat(utils.annotations().stream()
                .map(env::getElementsAnnotatedWith).map(Set::iterator).iterator());

        while (elements.hasNext()) {
            addElement(newElements, elements.next());
        }

        return newElements.build();
    }

    private void addElement(final ImmutableSet.Builder<DeferredProcessing> newElements,
            final Element e) {
        if (e instanceof TypeElement) {
            newElements.add(new DeferredProcessing((TypeElement) e, Optional.empty()));
            return;
        }

        if (e instanceof ExecutableElement) {
            addElement(newElements, e.getEnclosingElement());
            return;
        }

        messager.printMessage(Diagnostic.Kind.ERROR, "Unsupported element", e);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return RsUtils.ANNOTATION_NAMES;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    List<Processed> processElements(Set<DeferredProcessing> elements) {
        final List<Processed> processed = new ArrayList<>();

        for (final DeferredProcessing processing : elements) {
            final Result<JavaFile> result = processElement(processing.getElement());
            processed.add(new Processed(result, processing));
        }

        return processed;
    }

    Result<JavaFile> processElement(TypeElement element) {
        if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
            return classProcessor.process(element);
        }

        return Result.brokenElement("Unsupported type, expected class or interface", element);
    }

    @Data
    public static class Processed {
        final Result<JavaFile> file;
        final DeferredProcessing processing;
    }
}
