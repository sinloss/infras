package com.sinlo.sponte.core;

import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.Ext;
import com.sinlo.sponte.util.ProxyedSponte;
import com.sinlo.sponte.util.Typer;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * The processing context
 *
 * @author sinlo
 */
public class Context {

    /**
     * The processing environment
     */
    public final ProcessingEnvironment env;
    /**
     * The type utils from {@link ProcessingEnvironment#getTypeUtils()}
     */
    public final Types types;
    /**
     * The messager from {@link ProcessingEnvironment#getMessager()}
     */
    public final Messager messager;
    /**
     * The filer from {@link ProcessingEnvironment#getFiler()}
     */
    public final Filer filer;
    /**
     * The annotation type element
     */
    public final TypeElement annotation;
    /**
     * The qualified name of the {@link #annotation}
     */
    public final String qname;
    /**
     * The descriptor of the {@link #annotation}
     */
    public final String descriptor;

    /**
     * The {@link Sponte.Writer} of manifest files
     */
    private final Sponte.Writer wm;
    /**
     * The {@link Sponte.Writer} of inheritance manifest files
     */
    private final Sponte.Writer wim;
    /**
     * Current subject
     */
    private Subject subject;
    /**
     * If error occurred
     */
    private boolean erred = false;

    Context(ProcessingEnvironment env, TypeElement annotation, Set<String> existed) {
        this.messager = env.getMessager();
        this.qname = (this.annotation = annotation)
                .getQualifiedName().toString();
        this.descriptor = Typer.descriptor(this.annotation);
        this.wm = Sponte.Writer.of(qname, true);
        this.wim = Sponte.Writer.of(
                qname.concat(Sponte.Fo.INHERITANCE.name), true);
        this.env = env;
        this.types = env.getTypeUtils();
        this.filer = env.getFiler();
    }

    boolean close() {
        wm.close();
        if (wim != null) wim.close();
        if (subject != null) subject.close();
        Sponte.Fo.closeAll();
        return !erred;
    }

    /**
     * Get the root path
     */
    public Path root() {
        FileObject fo = null;
        try {
            fo = filer.createResource(StandardLocation.CLASS_OUTPUT,
                    "", "_t_m_p_" + System.currentTimeMillis(),
                    (Element[]) null);
            return Paths.get(fo.toUri()).getParent().getParent();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fo != null) fo.delete();
        }
    }

    /**
     * @see Filer#createResource(JavaFileManager.Location, CharSequence, CharSequence, Element...)
     */
    public FileObject res(String pkg, String name) {
        try {
            return filer.createResource(
                    StandardLocation.CLASS_OUTPUT, pkg, name, (Element[]) null);
        } catch (IOException ignored) {
        }
        return null;
    }

    Subject subject(Element element) {
        if (subject != null) subject.close();
        return subject = new Subject(element);
    }

    /**
     * The current processing subject
     */
    public class Subject {

        /**
         * The {@link Context}
         */
        public final Context ctx;
        /**
         * The current element
         */
        public final Element current;
        /**
         * The current element kind
         */
        public final ElementKind kind;
        /**
         * The enclosing type element
         */
        public TypeElement enclosing;
        /**
         * The descriptor name of the type
         */
        public String descriptor;
        /**
         * The qualified name of the type
         */
        public String qname;
        /**
         * Musts
         */
        public Must[] musts;
        /**
         * Sponte
         */
        public Sponte sponte;

        /**
         * The {@link Sponte.Writer} of agent manifest files
         */
        private Sponte.Writer wam;

        Subject(Element current) {
            this.ctx = Context.this;
            this.kind = (this.current = current).getKind();
        }

        /**
         * The required {@link #sponte} is on the {@link #current}
         */
        void currentSponte() {
            sponte = new ProxyedSponte(current);
        }

        /**
         * The required {@link #sponte} is on the {@link #ctx#annotation}
         */
        void contextSponte() {
            sponte = new ProxyedSponte(ctx.annotation);
        }

        /**
         * Process the inheritable annotation element, and interrupt the following process
         * if it's not
         */
        void inheritance() throws InterruptedException {
            if (ElementKind.ANNOTATION_TYPE.equals(kind) &&
                    (sponte.inheritable() ||
                            current.getAnnotation(Sponte.Inherit.class) != null)) {
                Sponte.Fo.INHERITANCE.println(qname, null);
                wim.println(descriptor, null);
            } else {
                throw new InterruptedException();
            }
        }

        void agent(String agent) {
            if (wam == null) {
                wam = Sponte.Writer.of(qname.concat(Agent.SIGNATURE), true);
            }
            wam.println(agent, null);
        }

        void sponted() {
            if (ElementKind.ANNOTATION_TYPE.equals(kind)
                    && idle()) {
                Sponte.Fo.SPONTED.println(qname, null);
            }
        }

        /**
         * Not exists in the manifest of {@link Sponte.Fo#SPONTED}
         */
        boolean idle() {
            return !Sponte.Fo.SPONTED.contains(qname);
        }

        void manifest(String entry) {
            ctx.wm.println(entry, () -> {
                if (subject.current.getAnnotationMirrors().stream().
                        map(AnnotationMirror::getAnnotationType)
                        .map(Typer::descriptor)
                        .filter(e -> e.equals(ctx.descriptor) || ctx.wim.contains(e))
                        .count() > 1) {
                    error(String.format(
                            "Could not annotate [ %s ] or anyone of its inheritors on the same element",
                            ctx.annotation));
                }
            });
        }

        /**
         * Create an {@link Ext} of the current type that does not just implement it
         *
         * @see #ext(boolean)
         */
        public Ext ext() {
            return ext(false);
        }

        /**
         * Create an {@link Ext} of the current type
         *
         * @see Ext#of(TypeElement, boolean)
         */
        public Ext ext(boolean justImplement) {
            return Ext.of(enclosing, justImplement);
        }

        /**
         * Error output
         */
        public void error(String message) {
            ctx.messager.printMessage(Diagnostic.Kind.ERROR, message, this.current);
            ctx.erred = true;
        }

        void close() {
            if (wam != null) wam.close();
        }

    }
}
