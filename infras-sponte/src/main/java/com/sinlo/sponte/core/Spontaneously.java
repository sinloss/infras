package com.sinlo.sponte.core;

import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.sinlo.sponte.Sponte.*;

/**
 * The main processor for the {@link Sponte} and all annotations annotated
 * with {@link Sponte}
 *
 * @author sinlo
 */
@SupportedAnnotationTypes({Sponte.NAME, Must.NAME})
public class Spontaneously extends AbstractProcessor {

    private int round = 0;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    /**
     * Entrypoint
     */
    @Override
    public boolean process(Set<? extends TypeElement> types,
                           RoundEnvironment roundEnv) {
        if (round++ != 0) return true;
        // get the current stage
        Stage stage = Stage.which();
        // get all existed sponted annotation names
        Set<String> existed = Fo.SPONTED.names();

        // the proc in which the main process logic is being held
        BiConsumer<TypeElement, TypeElement> proc = (t, a) -> {
            Context ctx = new Context(this.processingEnv, t, existed);
            roundEnv.getElementsAnnotatedWith(a).stream()
                    .map(ctx::subject).forEach(stage::process);
            ctx.close();
        };

        if (Stage.INHERIT.equals(stage)) {
            // iterate all the annotation presented in the existed set,
            // which is the annotation that are inherited by the current
            // processing elements
            types.forEach(a -> a.getAnnotationMirrors().stream()
                    .map(AnnotationMirror::getAnnotationType)
                    .filter(t -> existed.contains(t.toString()))
                    .map(t -> (TypeElement) t.asElement())
                    .forEach(t -> proc.accept(t, a)));
        } else {
            types.forEach(a -> proc.accept(a, a));
        }
        return true;
    }

    private Set<String> names(Stage stage) {
        switch (stage) {
            case INHERIT:
                return Fo.INHERITANCE.names();
            case MANIFEST:
                return Fo.SPONTED.names();
            case SPONTE:
            default:
                return super.getSupportedAnnotationTypes();
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Stage stage = Stage.which().advance();
        if (Stage.FIN.equals(stage)) {
            return Collections.emptySet();
        }

        Set<String> names = names(stage);
        if (names == null || names.isEmpty()) {
            stage.fin();
        }
        return names;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Another round processing of {@link Spontaneously}
     *
     * @author sinlo
     */
    @SupportedAnnotationTypes({Sponte.NAME, Must.NAME})
    public static class AnotherStage extends Spontaneously {
    }

    /**
     * Yet another round processing of {@link Spontaneously}
     *
     * @author sinlo
     */
    @SupportedAnnotationTypes({Sponte.NAME, Must.NAME})
    public static class YetAnotherStage extends Spontaneously {
    }

    /**
     * The final round processing of {@link Spontaneously}
     *
     * @author sinlo
     */
    @SupportedAnnotationTypes({Sponte.NAME, Must.NAME})
    public static class FinStage extends Spontaneously {
    }
}
