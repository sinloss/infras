package com.sinlo.sponte.core;

import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.Ext;
import com.sinlo.sponte.util.ProxyedSponte;
import com.sinlo.sponte.util.SponteFiler;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.util.Set;

/**
 * The processing context
 *
 * @author sinlo
 */
class Context {

    private final Messager messager;
    /**
     * The {@link PrintWriter} of manifest files
     */
    private final PrintWriter wm;
    /**
     * The {@link PrintWriter} of inheritance manifest files
     */
    private PrintWriter wim;

    final ProcessingEnvironment env;
    final Types types;
    final TypeElement annotation;
    final String qname;
    /**
     * All existed type names listed in sponted files
     */
    final Set<String> existed;
    final Set<String> manifested;
    /**
     * Current subject
     */
    private Subject subject;

    Context(ProcessingEnvironment env, TypeElement annotation, Set<String> existed) {
        this.messager = env.getMessager();
        this.qname = (this.annotation = annotation)
                .getQualifiedName().toString();
        this.wm = SponteFiler.writer(Sponte.Fo.of(qname), true);
        this.existed = existed != null ? existed : Sponte.Fo.SPONTED.names();
        this.manifested = Sponte.Fo.names(qname);
        this.env = env;
        this.types = env.getTypeUtils();
    }

    void close() {
        wm.close();
        if (wim != null) wim.close();
        if (subject != null) subject.close();
        Sponte.Fo.closeAll();
    }

    Subject subject(Element element) {
        if (subject != null) subject.close();
        return subject = new Subject(element);
    }

    /**
     * The current processing subject
     */
    class Subject {

        final Context ctx;
        final Element current;
        final ElementKind kind;

        TypeElement enclosing;
        /**
         * The descriptor name of the type
         */
        String descriptor;
        /**
         * The qualified name of the type
         */
        String qname;
        /**
         * The {@link PrintWriter} of agent manifest files
         */
        private PrintWriter wam;

        Must[] musts;
        Sponte sponte;

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
            if (ElementKind.ANNOTATION_TYPE.equals(kind) && sponte.inheritable()) {
                Sponte.Fo.INHERITANCE.println(qname);
                if (wim == null) {
                    wim = SponteFiler.writer(Sponte.Fo.of(
                            ctx.qname.concat(Sponte.Fo.INHERITANCE.fn)), true);
                }
                wim.println(descriptor);
                wim.flush();
            } else {
                throw new InterruptedException();
            }
        }

        void agent(String agent) {
            if (wam == null) {
                wam = SponteFiler.writer(Sponte.Fo.of(
                        qname.concat(Agent.SIGNATURE)), true);
            }
            wam.println(agent);
            wam.flush();
        }

        void sponted() {
            if (ElementKind.ANNOTATION_TYPE.equals(kind)
                    && idle()) {
                Sponte.Fo.SPONTED.println(qname);
                ctx.existed.add(qname);
            }
        }

        /**
         * Not exists in the {@link #ctx#existed}
         */
        boolean idle() {
            return !ctx.existed.contains(qname);
        }

        void manifest(String entry) {
            if (ctx.manifested.contains(entry)) {
                error(String.format(
                        "Could only annotate one of [ %s ] or its inheritors on the same element",
                        ctx.annotation));
            } else {
                ctx.wm.println(entry);
                ctx.wm.flush();
                ctx.manifested.add(entry);
            }
        }

        /**
         * Create an {@link Ext} of the current type that does not just implement it
         *
         * @see #ext(boolean)
         */
        Ext ext() {
            return ext(false);
        }

        /**
         * Create an {@link Ext} of the current type
         *
         * @see Ext#of(TypeElement, boolean)
         */
        Ext ext(boolean justImplement) {
            return Ext.of(enclosing, justImplement);
        }

        /**
         * Error output
         */
        void error(String message) {
            ctx.messager.printMessage(Diagnostic.Kind.ERROR, message, this.current);
        }

        void close() {
            if (wam != null) wam.close();
        }
    }
}
