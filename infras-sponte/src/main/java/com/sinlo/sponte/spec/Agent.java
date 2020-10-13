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
import java.util.concurrent.Callable;
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

    Class<? extends Bond> value();

    String generify() default "";

    Class<? extends Annotation>[] forward() default {};

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface Ignore {
    }

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Repeatable(Annotate.All.class)
    @interface Annotate {
        Class<? extends Annotation> value();

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
        <T> T act(Context context, Callable<T> mission);
    }

    /**
     * Mission context
     */
    class Context {
        public final Object self;
        public final String name;
        public final String signature;
        public final Class<? extends Annotation> pivot;
        public final Set<String> annotations;
        public final Object[] args;

        public Context(Object self, String name, String signature,
                       Class<? extends Annotation> pivot,
                       String[] annotations, Object... args) {
            this.self = self;
            this.name = name;
            this.signature = signature;
            this.pivot = pivot;
            Collections.addAll(this.annotations = new HashSet<>(), annotations);
            this.args = args;
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
        public <T> T act(Context context, Callable<T> mission) {
            try {
                return act(0, context, mission);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private <T> T act(int i, Context context, Callable<T> mission) throws Exception {
            if (i >= bonds.length) {
                return mission.call();
            }
            return bonds[i].act(context, () -> act(i + 1, context, mission));
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
