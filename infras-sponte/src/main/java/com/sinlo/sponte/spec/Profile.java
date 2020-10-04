package com.sinlo.sponte.spec;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The profile of the annotation
 *
 * @author sinlo
 */
public class Profile {

    public static final char METHOD_OPEN = '(';
    public static final char METHOD_ARG_NAME_DELIMITER = ' ';
    public static final char METHOD_ARG_DELIMITER = ',';
    public static final char METHOD_CLOSE = ')';

    public static final Class<?>[] EMPTY_ARGV = new Class[0];

    private static final Pool.Simple<Builder> pool = new Pool.Simple<>();

    public final Sponte sponte;
    public final Perch perch;
    public final Annotation subject;
    public final Class<?> type;
    public final Field field;
    public final Method method;
    public final Constructor<?> constructor;
    public final Arg arg;

    /**
     * Tells if the subject is {@link Sponte} and the type of profiled element is
     * an {@link Annotation}, that is, the profiled element might just want to
     * inherit the values from the {@link Sponte} annotating on it instead of
     * being processed by the handlers defined in the said {@link Sponte}
     */
    public boolean meta() {
        return subject instanceof Sponte && this.type.isAnnotation();
    }

    /**
     * Create a {@link Profile} by creating and populate a {@link Builder} based on
     * the given {@code expr}
     */
    public static Profile of(String expr, Class<? extends Annotation> c) {
        return pool.get(expr, () -> populate(null, new Builder(), expr, null))
                .subject(c).build();
    }

    /**
     * Clear {@link #pool}
     */
    public static void purge() {
        pool.purge();
    }

    private static Builder populate(Perch perch, Builder builder, String left, String right) {
        try {
            if (builder.perch == null) {
                builder.perch(perch);
            }
            if (perch != Perch.TYPE) {
                // recursive the next
                Perch next = Perch.identify(left);
                String[] parts = next == null || next == Perch.TYPE
                        ? new String[]{left} : left.split(next.sign);
                populate(next, builder, parts[0], parts.length >= 2 ? parts[1] : "");
            }

            if (perch == null) return builder;
            switch (perch) {
                case TYPE:
                    builder.type(left);
                    break;
                case PARAMETER:
                    builder.arg(Integer.parseInt(right))
                            .parameter();
                    break;
                case METHOD:
                    int open = right.indexOf(METHOD_OPEN);
                    builder.argv(right.substring(open))
                            .method(right.substring(0, open));
                    break;
                case CONSTRUCTOR:
                    builder.argv(right).constructor();
                    break;
                case FIELD:
                    builder.field(right);
                    break;
            }
        } catch (NoSuchFieldException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return builder;
    }

    private Profile(Sponte sponte, Perch perch, Annotation subject, Class<?> type, Field field, Method method,
                    Constructor<?> constructor, Arg arg) {
        this.sponte = sponte;
        this.perch = perch;
        this.subject = subject;
        this.type = type;
        this.field = field;
        this.method = method;
        this.constructor = constructor;
        this.arg = arg;
    }

    public static class Arg {

        public final Parameter parameter;
        public final int index;
        public final String name;

        private Arg(Parameter parameter, int index, String name) {
            this.parameter = parameter;
            this.index = index;
            this.name = name;
        }
    }

    public static class Builder {
        private Sponte sponte;
        private Perch perch;
        private Annotation subject;
        private Class<?> type;
        private Field field;
        private Class<?>[] argv = EMPTY_ARGV;
        private String[] argvNames;
        private Method method;
        private Constructor<?> constructor;
        // properties for the arg
        private Parameter parameter;
        private int index = -1;
        private String name;

        public Builder perch(Perch perch) {
            this.perch = perch;
            return this;
        }

        /**
         * Set the {@link #subject}, it depends on all the necessary fields which
         * should be populated beforehand
         */
        public Builder subject(Class<? extends Annotation> c) {
            if (c == null) return this;

            AnnotatedElement element = null;
            switch (perch) {
                case FIELD:
                    element = field;
                    break;
                case PARAMETER:
                    element = parameter;
                    break;
                case METHOD:
                    element = method;
                    break;
                case CONSTRUCTOR:
                    element = constructor;
                    break;
                case TYPE:
                    element = type;
                    break;
            }
            if (element == null) return this;

            subject = element.getAnnotation(c);
            if (subject instanceof Sponte) {
                this.sponte = (Sponte) subject;
            } else {
                this.sponte = c.getAnnotation(Sponte.class);
            }
            return this;
        }

        /**
         * Set the {@link #type} using the given qualified name
         */
        public Builder type(String qname) throws ClassNotFoundException {
            this.type = Class.forName(qname);
            return this;
        }

        /**
         * Set the {@link #field} using the given field name, and the {@link #type}
         * which should be set beforehand
         *
         * @see #type(String)
         */
        public Builder field(String name) throws NoSuchFieldException {
            this.field = type.getDeclaredField(name);
            return this;
        }

        /**
         * Set the {@link #method} using the given method name, basing on the {@link #type}
         * and the {@link #argv} which both should be set beforehand
         *
         * @see #type(String)
         * @see #argv(String)
         */
        public Builder method(String name) throws NoSuchMethodException {
            this.method = type.getDeclaredMethod(name, argv);
            return this;
        }

        /**
         * Set the {@link #constructor} using the given constructor name, basing on the
         * {@link #type} and the {@link #argv} which both should be set beforehand
         *
         * @see #type(String)
         * @see #argv(String)
         */
        public Builder constructor() throws NoSuchMethodException {
            this.constructor = type.getDeclaredConstructor(argv);
            return this;
        }

        /**
         * Set the {@link #argv} using the given parameter signature string
         */
        public Builder argv(String signature) throws ClassNotFoundException {
            if (signature == null) return this;

            int len = signature.length();
            if (signature.indexOf(METHOD_OPEN) != 0 ||
                    signature.indexOf(METHOD_CLOSE) != len - 1 ||
                    len == 2) return this;

            String[] rawArgs = signature.substring(1, len - 1)
                    .split(String.valueOf(METHOD_ARG_DELIMITER));

            List<Class<?>> all = new ArrayList<>();
            List<String> allNames = new ArrayList<>();
            for (String rawArg : rawArgs) {
                int delim = rawArg.indexOf(METHOD_ARG_NAME_DELIMITER);
                all.add(Typer.forName(rawArg.substring(0, delim)));
                allNames.add(rawArg.substring(delim + 1));
            }
            argv = all.toArray(EMPTY_ARGV);
            argvNames = allNames.toArray(new String[0]);
            return this;
        }

        /**
         * Set the {@link #index} which will be used as the {@link Arg#index} in the
         * finally built entity
         */
        public Builder arg(int index) {
            this.name = argvNames[this.index = index];
            return this;
        }

        /**
         * Set the {@link #parameter} using the preset {@link #method} or {@link #constructor},
         * basing on the {@link #index} of the said {@link #parameter}
         */
        public Builder parameter() {
            this.parameter = (constructor != null ? constructor : method)
                    .getParameters()[this.index];
            return this;
        }

        /**
         * Finally build the entity
         */
        public Profile build() {
            return new Profile(sponte, perch, subject, type, field, method, constructor,
                    new Arg(parameter, index, name));
        }
    }
}
