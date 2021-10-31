package com.sinlo.sponte.core;

import com.sinlo.sponte.Must;
import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.spec.Agent;
import com.sinlo.sponte.spec.CompileAware;
import com.sinlo.sponte.spec.Perch;
import com.sinlo.sponte.spec.Ext;
import com.sinlo.sponte.util.*;

import javax.annotation.processing.FilerException;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The stage processing procedure definition and procedures
 *
 * @author sinlo
 */
@FunctionalInterface
public interface Procedure {

    void perform(Context.Subject cs) throws InterruptedException;

    /**
     * Interrupt if the current element is of class type, and not inheritable
     * while having no @Sponte directly presented on it
     */
    static void killUsurpers(Context.Subject cs) throws InterruptedException {
        if (cs.kind.isClass() && !cs.sponte.inheritable() &&
                cs.current.getAnnotation(Sponte.Inherit.class) == null
                && cs.current.getAnnotationMirrors().stream()
                .map(AnnotationMirror::getAnnotationType)
                .map(Objects::toString)
                .noneMatch(Sponte.class.getName()::equals)) {

            throw new InterruptedException();
        }
    }

    /**
     * Trace the type element and print the profiler profiled string
     */
    static void trace(Context.Subject cs) {
        trace(cs, cs::manifest);
    }

    /**
     * Trace the type element and consume the profiled string
     */
    static void trace(Context.Subject cs, Consumer<String> consumer) {
        Profiler profiler = new Profiler();

        if ((cs.enclosing = profiler.trace(cs.current)) != null) {
            consumer.accept(profiler.toString());
            cs.descriptor = Typer.descriptor(cs.enclosing);
            cs.qname = cs.enclosing.getQualifiedName().toString();
        }
    }

    /**
     * Validate context and subject
     */
    static void validate(Context.Subject cs) {
        if (ElementKind.ANNOTATION_TYPE.equals(cs.kind)
                && cs.idle()) {
            // check repeatable
            if (cs.current.getAnnotation(Repeatable.class) != null) {
                cs.error("Sponte/Must does not support repeatable annotations");
            }

            // check element type
            Target target = cs.current.getAnnotation(Target.class);
            if (target != null) {
                for (ElementType type : target.value()) {
                    if (Perch.support(type)) continue;
                    cs.error(String.format(
                            "Sponte does not support ElementType %s", type));
                }
            }
        }
    }

    /**
     * Test the {@link Must} conditions
     */
    static void must(Context.Subject cs) {
        cs.musts = cs.ctx.annotation.getAnnotationsByType(Must.class);
        if (cs.musts == null || cs.musts.length == 0) return;

        for (Must must : cs.musts) {
            boolean should = false;
            for (ElementKind kind : must.value()) {
                if (kind.equals(cs.kind)) {
                    should = true;
                    break;
                }
            }
            if (!should) continue;

            for (Must.Mirror mirror : Must.Mirror.values()) {
                mirror.check(must, cs);
            }
        }
    }

    /**
     * Do the {@link CompileAware} for the specified {@link Sponte#compiling()}
     */
    static void compiling(Context.Subject cs) {
        try {
            Class<? extends CompileAware> type = cs.sponte.compiling();
            if (type.isInterface()) return;

            CompileAware aware = CompileAware.pri.get(cs.sponte, type);
            if (aware != null) aware.onCompile(cs);
        } catch (IllegalStateException ise) {
            Throwable cause = ise.getCause();
            if (cause instanceof ClassNotFoundException) {
                // neglect the compiling
                if (cs.current.getAnnotation(Sponte.CompilingNeglect.class) != null) return;
                cs.error("The given compile aware class must exist before compiling");
            }
        }
    }

    /**
     * Generate an adapter for the current annotation type element, this does not check the
     * {@link Sponte#inheritable()} attribute
     */
    static void adapter(Context.Subject cs) {
        if (!ElementKind.ANNOTATION_TYPE.equals(cs.kind)) return;

        try {
            String sc = cs.ctx.annotation.getSimpleName().toString();
            Ext ext = cs.ext()
                    .importing(cs.qname, cs.ctx.qname, Annotation.class.getName())
                    .implementing(sc)
                    .method("public",
                            "Class<? extends Annotation>",
                            "annotationType", (Ext.Argument[]) null)
                    .lines(String.format("return %s.class", sc));

            // the builder that throws FilerException if the source file is already
            // created
            Ext.Builder builder = ext.create(cs.ctx.filer, cs.ctx.annotation);

            // values implemented by the current type
            Set<String> implementedValues = Typer.values(cs.enclosing);
            // values assigned on the meta annotation
            Map<String, String> assignedValues = Typer.values(cs.enclosing, cs.ctx.qname);

            // methods which is values in terms of annotation
            Typer.methods(cs.ctx.annotation).forEach(method -> {
                String name = method.getSimpleName().toString();
                String value = null;
                if (implementedValues.contains(name)) {
                    value = "t.".concat(name).concat("()");
                } else if (assignedValues != null && assignedValues.containsKey(name)) {
                    value = String.valueOf(assignedValues.get(name));
                } else {
                    AnnotationValue val = method.getDefaultValue();
                    if (val != null) {
                        value = val.toString();
                    }
                }

                Ext.Method m = ext.method("public",
                        method.getReturnType().toString(), name, (Ext.Argument[]) null);
                if (value != null) {
                    m.lines(String.format("return %s", value));
                }
            });

            builder.build();
        } catch (FilerException fe) {
            cs.error("An annotation could only inherit one inheritable *Sponte* annotation");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate the agent for the current enclosing class or interface
     */
    @SuppressWarnings("unchecked")
    static void agent(Context.Subject cs) {
        ElementKind kind = cs.enclosing.getKind();
        // skip annotation type
        if (ElementKind.ANNOTATION_TYPE.equals(kind)) return;

        // get and check @Agent
        Agent def = cs.sponte.agent();
        if (def == ProxyedSponte.agent) return;
        TypeElement agent = Typer.mirror(def::value);
        if (agent.getKind().isInterface()) return;

        // forbid abstract class
        if (ElementKind.CLASS.equals(kind)
                && Typer.isAbstract(cs.enclosing)) {
            cs.error("Abstract class is not supported by @Agent");
            return;
        }

        // enlist the agent
        cs.agent(Typer.descriptor(agent));

        // if the enclosing type is an interface or not
        boolean isInterface = kind.isInterface();

        // forwarded annotation types
        DeclaredType[] forwarded = Typer.mirrorTypes(def::forward);

        // create an ext
        Ext ext = cs.ext(isInterface)
                .importing(cs.qname,
                        Agent.class.getCanonicalName(),
                        Agent.Context.class.getCanonicalName(),
                        Annotation.class.getCanonicalName());
        try {
            // the builder that throws FilerException if the source file is already
            // created
            Ext.Builder builder = ext.create(cs.ctx.filer, cs.ctx.annotation);

            // extends or implements
            String extending = cs.enclosing.getSimpleName().toString().concat(def.generify());
            if (isInterface) {
                ext.implementing(extending);
            } else {
                ext.extending(extending);
            }

            // try to annotate and forward type annotations
            Agent.M.annotate(ext, cs.enclosing, forwarded);

            // get all methods
            (isInterface ?
                    // itself
                    Chainer.of(cs.enclosing.asType())
                            // and all super interfaces
                            .and((List<TypeMirror>) cs.enclosing.getInterfaces())
                            .stream()
                            // as element
                            .map(cs.ctx.types::asElement)
                            // all their methods
                            .flatMap(Typer::methods)
                    :
                    // all declared methods in class
                    Typer.methods(cs.enclosing)
            ).filter(Typer::isNotPrivate).forEach(method -> {
                // method name
                String name = method.getSimpleName().toString();
                // method structure
                Ext.Method m = ext.method(
                        // modifiers without abstract
                        method.getModifiers().stream()
                                .filter(mod -> !Modifier.ABSTRACT.equals(mod))
                                .map(Modifier::toString)
                                .collect(Collectors.joining(" ")),
                        // return type
                        method.getReturnType().toString(),
                        // method name
                        name,
                        // parameters
                        method.getParameters().stream()
                                .map(p -> {
                                    Ext.Argument arg = Ext.Argument.of(p.asType().toString(), p.toString());
                                    // try to annotate and forward parameter annotations
                                    Agent.M.annotate(arg, p, forwarded);
                                    return arg;
                                })
                                .toArray(Ext.Argument[]::new));
                // try to annotate and forward method annotations
                Agent.M.annotate(m, method, forwarded);
                // method handling
                if (method.getAnnotation(Agent.Ignore.class) != null) {
                    m.raw();
                } else {
                    // actual parameters that we are passing
                    String actual = m.passing();
                    // if the return type is void or not
                    boolean voided = TypeKind.VOID.equals(method.getReturnType().getKind());
                    // formatted method body
                    m.lines(String.format(
                            "%s agent().act(ctx(" +
                                    "\"%s\",\"%s\",new String[]{%s}%s),%s)",
                            voided ? "" : "return ",
                            name,
                            Signature.of(method),
                            method.getAnnotationMirrors().stream()
                                    .map(AnnotationMirror::getAnnotationType)
                                    .map(Object::toString)
                                    .map(s -> "\"".concat(s).concat("\""))
                                    .collect(Collectors.joining(",")),
                            actual.isEmpty() ? "" : ",".concat(actual),
                            isInterface ? "null" : String.format("(args)->{%st.%s(%s);%s}",
                                    voided ? "" : "return ",
                                    name,
                                    m.varargs("args"),
                                    voided ? "return null;" : "")));
                }
            });

            builder.build();
        } catch (FilerException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
