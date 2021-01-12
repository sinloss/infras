package com.sinlo.sponte.spec;

import com.sinlo.sponte.util.Typer;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The extension class creator
 *
 * @author sinlo
 */
public class Ext extends WithAnnotations<Ext> {

    /**
     * Package Name
     */
    private final String pn;
    /**
     * Imports
     */
    private final List<String> i = new LinkedList<>();
    /**
     * Class Name
     */
    private final String cn;
    /**
     * Super Interfaces
     */
    private final List<String> si = new LinkedList<>();
    /**
     * Target Class
     */
    private final String tc;
    /**
     * Super Class
     */
    private String sc = "";
    /**
     * Constants
     */
    private final Map<String, String> c = new HashMap<>();
    /**
     * Methods
     */
    private final List<Method> m = new LinkedList<>();
    /**
     * Just Implement
     */
    private final boolean ji;

    private Ext(String pn, String cn, String tc, boolean ji) {
        this.pn = pn;
        this.cn = cn;
        this.tc = tc;
        this.ji = ji;
    }

    public class Method extends WithAnnotations<Method> {

        /**
         * Return Value
         */
        private final String rv;

        /**
         * Modifiers
         */
        private final String m;

        /**
         * Name
         */
        private final String n;

        /**
         * Arguments
         */
        private final List<Argument> a;

        /**
         * Lines of Codes
         */
        private final List<String> lc = new LinkedList<>();

        private Method(String m, String rv, String n, Argument... a) {
            this.m = m == null ? "" : m;
            this.rv = rv == null || rv.isEmpty() ? "void" : rv;
            this.n = n;
            this.a = a == null ? Collections.emptyList() : Arrays.asList(a);
        }

        /**
         * Prepare arguments from an argument array
         */
        public String varargs(String name) {
            return IntStream.range(0, a.size())
                    .mapToObj(i -> String.format("(%s) %s[%s]",
                            a.get(i).t, name, i))
                    .collect(Collectors.joining(","));
        }

        /**
         * Get the string of all actual parameters
         */
        public String passing() {
            return a.stream().map(a -> a.n)
                    .collect(Collectors.joining(","));
        }

        /**
         * Just do the given lines of codes
         */
        public Ext lines(String... lc) {
            if (lc == null || lc.length == 0) {
                return this.empty();
            }
            Collections.addAll(this.lc, lc);
            return Ext.this;
        }

        /**
         * Just do nothing
         */
        public Ext empty() {
            if (!"void".equals(rv)) {
                this.lc.clear();
                this.lc.add("return ".concat(Typer.zeroValue(rv)));
            }
            return Ext.this;
        }

        /**
         * Just call the original method of the target
         */
        public Ext raw() {
            this.lc.add((!"void".equals(rv) ? "return " : "")
                    .concat("t.").concat(n).concat("(")
                    .concat(passing()).concat(")"));
            return Ext.this;
        }
    }

    public static class Argument extends WithAnnotations<Argument> {
        /**
         * Type
         */
        private final String t;
        /**
         * Name
         */
        private final String n;

        private Argument(String t, String n) {
            this.t = t;
            this.n = n;
        }

        public static Argument of(String t, String n) {
            return new Argument(t, n);
        }

        public static Argument of(String argument) {
            String[] arg = argument.split(" ");
            if (arg.length != 2)
                throw new IllegalArgumentException(
                        "An argument must consist of a type and a name");
            return new Argument(arg[0], arg[1]);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            everyAnnotation(a -> builder.append(a).append(" "));
            return builder.append(t).append(" ").append(n).toString();
        }
    }

    /**
     * Create an extension instance of the given extended class
     */
    public static Object create(Class<?> extended, Object t) throws ClassNotFoundException {
        return Typer.create(
                Class.forName(where(extended.getCanonicalName())
                        .concat(".")
                        .concat(extended.getSimpleName())
                        .concat(Ext.class.getSimpleName())),
                new Class[]{extended},
                t);
    }

    /**
     * Create an {@link Ext} creator
     *
     * @param packageName   the {@link #pn} which is the package name of the source file that
     *                      will be built
     * @param targetClass   the {@link #tc} which is the target class that are extended
     * @param justImplement the {@link #ji} which indicates that the extension does not wrap
     *                      any instance of the target class
     */
    public static Ext of(String packageName, String targetClass, boolean justImplement) {
        return new Ext(packageName,
                targetClass.concat(Ext.class.getSimpleName()),
                targetClass, justImplement);
    }

    /**
     * Create an {@link Ext} using the information provided by the given {@link TypeElement}
     *
     * @see #of(String, String, boolean)
     */
    public static Ext of(TypeElement te, boolean justImplement) {
        return of(where(te.getQualifiedName().toString()),
                te.getSimpleName().toString(), justImplement);
    }

    /**
     * Determine where the extension class of the given fqn should be
     */
    public static String where(String fqn) {
        return fqn.substring(0, fqn.lastIndexOf('.')).toLowerCase();
    }

    /**
     * import
     */
    public Ext importing(String... i) {
        Collections.addAll(this.i, i);
        return this;
    }

    /**
     * implements
     */
    public Ext implementing(String... si) {
        Collections.addAll(this.si, si);
        return this;
    }

    /**
     * extends
     */
    public Ext extending(String sc) {
        this.sc = sc;
        return this;
    }

    /**
     * constants
     */
    public Ext constant(String name, String value) {
        this.c.put(name, value);
        return this;
    }

    /**
     * Add a method with string argument expressions
     */
    public Method method(String modifiers, String returnValue, String name, String... args) {
        return method(modifiers, returnValue, name, Arrays.stream(args)
                .map(Argument::of).toArray(Argument[]::new));
    }

    /**
     * Add a method with {@link Argument} objects
     */
    public Method method(String modifiers, String returnValue, String name, Argument... args) {
        Method m = new Method(modifiers, returnValue, name, args);
        this.m.add(m);
        return m;
    }

    /**
     * Create the {@link Builder}
     */
    public Builder create(Filer filer, TypeElement pivot) throws IOException {
        this.implementing(Ext.I.class.getCanonicalName())
                .method("public", "Class<?>", "type", (Argument[]) null)
                .lines(String.format("return %s.class", tc))
                .method("public", "Class<? extends Annotation>", "pivot", (Argument[]) null)
                .lines(String.format("return %s.class", pivot.getQualifiedName()));
        return new Builder(filer.createSourceFile(pn.concat(".").concat(cn)));
    }

    /**
     * The final builder
     */
    public class Builder {
        private final JavaFileObject jfo;

        private Builder(JavaFileObject jfo) {
            this.jfo = jfo;
        }

        /**
         * Build the source file
         */
        public void build() throws IOException {
            try (PrintWriter w = new PrintWriter(jfo.openOutputStream())) {
                w.println(String.format("package %s;", pn));

                i.forEach(i -> w.println(String.format("import %s;", i)));
                everyAnnotation(w::println);
                w.println(String.format("public class %s%s{", cn,
                        (sc.isEmpty() ? "" : String.format(" extends %s", sc))
                                .concat(si.isEmpty() ? "" : String.format(
                                        " implements %s", String.join(",", si)))));

                c.forEach((k, v) -> w.println(String.format("public static final %s = %s;", k, v)));

                if (!ji) {
                    w.println(String.format("private final %s t;", tc));
                    w.println(String.format("public %s(%s t){this.t=t;}", cn, tc));
                }

                m.forEach(m -> {
                    m.everyAnnotation(w::println);
                    w.println(String.format("%s %s %s(%s){", m.m, m.rv, m.n,
                            m.a.stream().map(Argument::toString).collect(Collectors.joining(","))));
                    m.lc.forEach(l -> w.println(String.format("%s;", l)));
                    w.println("}");
                });

                w.println("}");
            }
        }
    }

    /**
     * The interface which all {@link Ext} built classes implement
     */
    public interface I {

        default Agent.Context ctx(String name, String sig, String[] notes, Object... args) {
            return new Agent.Context(this, name, sig, notes, args);
        }

        default Agent.Bond agent() {
            return Agent.MI6.get(type().getName());
        }

        /**
         * Simple cast
         */
        @SuppressWarnings("unchecked")
        default <T> T as(Class<T> t) {
            return (T) this;
        }

        Class<? extends Annotation> pivot();

        Class<?> type();
    }
}

abstract class WithAnnotations<T extends WithAnnotations<T>> {

    /**
     * Forwarded Annotations
     */
    private final List<String> fa = new LinkedList<>();

    @SuppressWarnings("unchecked")
    public T annotated(String fa) {
        this.fa.add(fa);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T annotated(String... fa) {
        Collections.addAll(this.fa, fa);
        return (T) this;
    }

    private String regulate(String a) {
        return a.startsWith("@") ? a : "@".concat(a);
    }

    /**
     * On every annotation expression inside of the {@link #fa} list do the
     * given action
     */
    public void everyAnnotation(Consumer<String> action) {
        if (!fa.isEmpty()) {
            fa.stream().map(this::regulate).forEach(action);
        }
    }
}