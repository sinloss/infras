package com.sinlo.sponte.core;

import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.Perch;
import com.sinlo.sponte.util.Profiler;
import com.sinlo.sponte.util.ProxyedSponte;
import com.sinlo.sponte.util.SponteFiler;
import com.sinlo.sponte.util.Typer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * The main processor for the {@link Sponte} and all annotations annotated
 * with {@link Sponte}
 *
 * @author sinlo
 */
@SupportedAnnotationTypes({Sponte.NAME, Must.NAME})
public class Spontaneously extends AbstractProcessor {

    private Messager log;
    private Types types;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        types = processingEnv.getTypeUtils();
        log = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> types,
                           RoundEnvironment env) {
        try {
            // spontaneously root
            Path root = SponteFiler.root();

            if (SponteFiler.locked()) {
                final Set<String> existed = SponteFiler.spontedNames();
                try (final PrintWriter pw = SponteFiler.writerSponted()) {
                    all(root, types, env, (e, qname) -> {
                        if (ElementKind.ANNOTATION_TYPE.equals(e.getKind())
                                && !existed.contains(qname)) {
                            // check repeatable
                            if (e.getAnnotation(Repeatable.class) != null) {
                                error("Sponte/Must does not support repeatable annotations", e);
                            }
                            // check element type
                            Target target = e.getAnnotation(Target.class);
                            if (target != null) {
                                for (ElementType type : target.value()) {
                                    if (Perch.support(type)) continue;
                                    error(String.format(
                                            "Sponte does not support ElementType %s",
                                            type.toString()),
                                            e);
                                }
                            }
                            // ok, print
                            pw.println(qname);
                            existed.add(qname);
                        }
                    });
                }
            } else {
                all(root, types, env, null);
            }

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void all(Path root, Set<? extends TypeElement> annotations,
                     RoundEnvironment roundEnv, BiConsumer<Element, String> and) {
        for (TypeElement t : annotations) {
            final String typeName = t.getQualifiedName().toString();
            final boolean roundOne = Sponte.class.getName().equals(typeName);
            manifest(root.resolve(typeName), roundEnv.getElementsAnnotatedWith(t),
                    (e, enc) -> {
                        Sponte sponte = new ProxyedSponte(roundOne ? e : t);
                        if (!roundOne) must(t, e, enc);
                        String qualified = enc.getQualifiedName().toString();
                        // try to invoke the compile aware
                        compiling(e, sponte, qualified);
                        // and then
                        if (and != null) and.accept(e, qualified);
                    });
        }
    }

    private void must(TypeElement t, Element e, TypeElement enc) {
        Must[] musts = t.getAnnotationsByType(Must.class);
        if (musts.length == 0) return;

        Perch perch = Perch.regard(e.getKind());
        for (Must must : musts) {
            if (!must.value().equals(perch)) continue;

            Must.Mirror.EXTEND.check(must, e.asType(), types, parent -> error(String.format(
                    "The %s annotated element must extend the type %s",
                    t.getQualifiedName().toString(), parent.toString()), e));

            Must.Mirror.IN.check(must, enc.asType(), types, parent -> error(String.format(
                    "The type where %s annotated element reside must extend the type %s",
                    t.getQualifiedName().toString(), parent.toString()), e));

        }
    }

    private void compiling(Element e, Sponte sponte, String qname) {
        try {
            Class<? extends CompileAware> type = sponte.compiling();
            if (type.isInterface()) return;

            CompileAware aware = CompileAware.Pri.get(type,
                    Sponte.Keys.get(sponte, type),
                    () -> Typer.create(type));
            if (aware != null) {
                aware.onCompile(this.processingEnv,
                        Class.forName(qname), e);
            }
        } catch (ClassNotFoundException ignored) {
        } catch (IllegalStateException ise) {
            Throwable cause = ise.getCause();
            if (cause instanceof ClassNotFoundException) {
                error("The given compile aware class must exist before compiling", e);
            }
        }
    }

    private static void manifest(Path path, Set<? extends Element> elements,
                                 BiConsumer<Element, TypeElement> and) {
        try (final PrintWriter pw = SponteFiler.writer(path, false)) {
            elements.forEach(e -> {
                // find enclosing type
                Profiler profiler = new Profiler();
                TypeElement enc = profiler.trace(e);
                if (enc == null) return;

                if (and != null)
                    and.accept(e, enc);
                pw.println(profiler.toString());
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        try {
            if (!SponteFiler.lock()) {
                return SponteFiler.spontedNames();
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return super.getSupportedAnnotationTypes();
    }

    public void error(String message, Element e) {
        this.log.printMessage(Diagnostic.Kind.ERROR, message, e);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
