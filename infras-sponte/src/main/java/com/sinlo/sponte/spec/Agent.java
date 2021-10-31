package com.sinlo.sponte.spec;

import com.sinlo.sponte.Sponte;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The agent
 *
 * @author sinlo
 * @see Sponte#agent()
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Agent {

    /**
     * The {@link Class} of the agent
     */
    Class<? extends Bond> value();

    /**
     * The generic parameter values that will be assigned to the generated agent class, the
     * string must starts with the &lt; and ends with the &gt;
     */
    String generify() default "";

    /**
     * The annotations that is expected to be forwarded to the generated agent class
     */
    Class<? extends Annotation>[] forward() default {};

    /**
     * Ignore the annotated method, that is the method will be directly called in the
     * generated agent class
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Ignore {
    }

    /**
     * To make an extra annotation on the specific place, this is repeatable
     */
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Repeatable(Annotate.All.class)
    @interface Annotate {

        /**
         * The annotation type
         */
        Class<? extends Annotation> value();

        /**
         * The values' expression, that is the whole part inside the parenthesis of the
         * annotation
         */
        String values() default "";

        @Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        @interface All {
            Annotate[] value();
        }
    }

    String SIGNATURE = ".agent";

    /**
     * The MI6
     */
    Pool.Simple<Bond> MI6 = new Pool.Simple<>();

    /**
     * Agent 007
     */
    interface Bond {

        /**
         * Agent on the mission
         */
        <T> T act(Context context, Mission<T> mission);

        /**
         * Sneaky throw
         */
        @SuppressWarnings("unchecked")
        default <T, E extends Throwable> T toss(Throwable e) throws E {
            throw (E) e;
        }
    }

    /**
     * The mission that calls
     */
    @FunctionalInterface
    interface Mission<R> {
        R call(Object... args) throws Exception;
    }

    /**
     * Mission context
     */
    class Context {
        /**
         * A reference to the delegated object
         */
        public final Ext.I self;
        /**
         * The name of the method
         */
        public final String name;
        /**
         * The unique method signature
         */
        public final String signature;
        /**
         * A set of all original annotations on the delegated method
         */
        public final Set<String> annotations;
        /**
         * The arguments that are passed into the delegated method
         */
        public final Object[] args;

        public Context(Ext.I self, String name, String signature,
                       String[] annotations, Object... args) {
            this.self = self;
            this.name = name;
            this.signature = signature;
            Collections.addAll(this.annotations = new HashSet<>(), annotations);
            this.args = args;
        }

        /**
         * Alter the current context with new arguments
         */
        public Context alter(Object... args) {
            if (this.args.length != args.length) {
                throw new IllegalArgumentException(String.format(
                        "The number of given new arguments does not match [ %s ] as expected",
                        this.args.length));
            }
            // copy args
            System.arraycopy(args, 0, this.args, 0, this.args.length);
            return this;
        }

        /**
         * Reflect the underlying {@link Method} of the current context;
         */
        public Method reflect() {
            try {
                return self.getClass().getDeclaredMethod(name,
                        Arrays.stream(args).map(Object::getClass).toArray(Class[]::new));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Check if the original method has the given annotation
         */
        public boolean has(Class<? extends Annotation> annotation) {
            return this.annotations.contains(annotation.getCanonicalName());
        }
    }

    /**
     * The task force consists of many agents
     */
    class TaskForce implements Bond {
        private final Bond[] bonds;

        private TaskForce(Bond[] bonds) {
            this.bonds = bonds;
        }

        @Override
        public <T> T act(Context context, Mission<T> mission) {
            try {
                return act(0, context, mission, context.args);
            } catch (Exception e) {
                return toss(e);
            }
        }

        private <T> T act(int i, Context context, Mission<T> mission, Object... args) throws Exception {
            if (i >= bonds.length) {
                return mission.call(args);
            }
            return bonds[i].act(
                    i == 0 ? context : context.alter(args),
                    (a) -> act(i + 1, context, mission, a));
        }
    }

    class M {

        /**
         * Create an extended target with a group of task force
         */
        public static Object create(Class<?> ext, Object t) {
            // try to create a task force
            M.taskForce(ext);
            try {
                return Ext.create(ext, t);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        static void taskForce(Class<?> guise) {
            Set<String> names = Sponte.Fo.names(guise.getCanonicalName().concat(SIGNATURE));
            if (names.isEmpty()) return;
            if (names.size() == 1) {
                // a single agent does not have teammates
                names.stream().findFirst().map(Typer::create)
                        .map(o -> (Bond) o)
                        .ifPresent(o -> MI6.place(guise.getCanonicalName(), o));
                return;
            }

            TaskForce taskForce = new TaskForce(names.stream()
                    .map(Typer::create)
                    .map(o -> (Bond) o)
                    .toArray(Bond[]::new));
            MI6.place(guise.getCanonicalName(), taskForce);
        }

        /**
         * Try to populate annotation information into the given {@link WithAnnotations}
         */
        public static void annotate(WithAnnotations<?> wfa, Element e, DeclaredType[] forwarded) {
            for (DeclaredType f : forwarded) {
                String name = ((TypeElement) f.asElement()).getQualifiedName().toString();
                Map<String, String> values = Typer.values(e, name);
                if (values == null) continue;

                wfa.annotated(String.format("@%s(%s)",
                        name, values.entrySet().stream()
                                .map(entry ->
                                        entry.getKey().concat("=").concat(entry.getValue()))
                                .collect(Collectors.joining(","))));
            }
            // the assigned annotations
            Annotate[] annotates = e.getAnnotationsByType(Agent.Annotate.class);
            if (annotates.length == 0) return;
            for (Annotate annotate : annotates) {
                wfa.annotated(String.format("@%s(%s)",
                        Typer.mirror(annotate::value)
                                .getQualifiedName(), annotate.values()));
            }
        }
    }
}
