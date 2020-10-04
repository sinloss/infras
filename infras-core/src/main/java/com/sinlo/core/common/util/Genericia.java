package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.Node;
import com.sinlo.sponte.util.Pool;
import com.sinlo.sponte.util.Typer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Genericia the generic util
 *
 * @author sinlo
 */
public class Genericia {

    private Genericia() {
    }

    /**
     * Get all generic superclass and interfaces together
     */
    public static Type[] getGenericSupers(Class<?> clz) {
        final Type superclass = clz.getGenericSuperclass();
        if (superclass != null && !Object.class.equals(superclass)) {
            return Arria.append(clz.getGenericInterfaces(), superclass);
        }
        return clz.getGenericInterfaces();
    }

    /**
     * Get a {@link ChainMap generic chain map} of the given prototype
     */
    public static ChainMap chainMap(Class<?> prototype) {
        return chains(prototype, new Chains()).toMap();
    }

    private static Chains chains(Class<?> prototype, Chains chains) {
        Type[] supers = getGenericSupers(prototype);
        if (supers != null) {
            for (Type type : supers) {
                if (type instanceof ParameterizedType) {
                    final ParameterizedType pt = ((ParameterizedType) type);
                    Type[] tas = pt.getActualTypeArguments(); // actual
                    Type rawType = pt.getRawType();
                    if (rawType instanceof Class) {
                        final Class<?> superclass = (Class<?>) rawType;
                        TypeVariable<? extends Class<?>>[] tps =
                                superclass.getTypeParameters(); // declared
                        for (int i = 0; i < tas.length; i++) {
                            // try to link
                            chains.link(new TypeNode(prototype, tas[i]),
                                    new TypeNode(superclass, tps[i]));
                        }
                        chains(superclass, chains);
                    }
                }
            }
        }
        return chains;
    }

    /**
     * The generic chain map which maps the {@link #key(Class, String) key} of the type param
     * of the furthest super class/interface of a generic chain and the value of a
     * {@link Node#rooted(Node) root node} of a linked list
     */
    public static class ChainMap {
        private final Map<String, TypeNode> chainMap;

        private ChainMap(Map<String, TypeNode> chainMap) {
            this.chainMap = Collections.unmodifiableMap(chainMap);
        }

        /**
         * Get the corresponding {@link Node#rooted(Node) root node} of the given type param
         * of the specific class
         *
         * @see #key(Class, String)
         */
        public TypeNode get(Class<?> clz, String typeName) {
            return chainMap.get(key(clz, typeName));
        }

        /**
         * @see #get(Class, String)
         * @see #key(Class, int)
         */
        public TypeNode get(Class<?> clz, int typeIndex) {
            return chainMap.get(key(clz, typeIndex));
        }

        /**
         * Same as {@link #key(Class, String)} but using the index of the specific type
         * param instead of its name
         *
         * @param typeIndex the index of the specific type
         * @see #key(Class, String)
         */
        public static String key(Class<?> clz, int typeIndex) {
            return key(clz,
                    clz.getTypeParameters()[typeIndex].getTypeName());
        }

        /**
         * Create a key representing the given {@link TypeNode}
         *
         * @param typeNode the given {@link TypeNode}
         * @return key string
         * @see #key(Class, String)
         */
        public static String key(TypeNode typeNode) {
            return key(typeNode.clz, typeNode.type == null
                    ? "" : typeNode.type.getTypeName());
        }

        /**
         * Create a key representing the type param of the specific class
         *
         * @param clz      the specific class
         * @param typeName the name of the type param
         * @return key string
         */
        public static String key(Class<?> clz, String typeName) {
            return clz.getName()
                    .concat("-")
                    .concat(typeName);
        }
    }

    /**
     * Node of {@link Chains} containing class and type param info
     */
    public static class TypeNode extends Node<TypeNode> {

        /**
         * The concrete class where the {@link #type} resides
         */
        public final Class<?> clz;
        /**
         * The type parameter type
         */
        public final Type type;

        public TypeNode(Class<?> clz, Type type) {
            this.clz = clz;
            this.type = type;
        }

    }


    private static class Chains {

        private final List<TypeNode> roots = new ArrayList<>();

        private Chains link(TypeNode aim, TypeNode next) {
            try {
                if (aim == null) return this;
                for (TypeNode root : roots) {
                    TypeNode last = root.prev();
                    if (last == null) continue;
                    if (aim.type.equals(last.type)) {
                        last.join(next);
                    }
                }
                if (next.prev() == null) {
                    // not linked then add root
                    roots.add(Node.rooted(aim).join(next));
                }
            } catch (Node.NotDetachedException e) {
                e.printStackTrace();
            }
            return this;
        }

        private ChainMap toMap() {
            return new ChainMap(roots.stream()
                    .collect(Collectors
                            .toMap(r -> ChainMap.key(r.prev()),
                                    r -> r)));
        }
    }

    private static class Privat {
        private final Pool.Simple<String> pool = new Pool.Simple<>();
        private final Class<?> anchor;

        private Privat(Class<?> anchor) {
            this.anchor = anchor;
        }

        private String get(Class<?> prototype) {
            return pool.get(prototype.getName(),
                    () -> Typer.descriptor(chainMap(prototype)
                            .get(anchor, 0).type.getTypeName()));
        }
    }

    /**
     * The self awareness of the generic type parameter's real identity, any generic
     * class can achieve this awareness by simply extends or implements this interface
     * and assign the type parameter you want to be aware of
     */
    @SuppressWarnings("unused")
    public interface Aware<T> {

        Privat p = new Privat(Aware.class);

        default String aw() {
            return p.get(this.getClass());
        }
    }

    /**
     * To provide another awareness in combination with {@link Aware}
     */
    @SuppressWarnings("unused")
    public interface Bware<T> {
        Privat p = new Privat(Bware.class);

        default String bw() {
            return p.get(this.getClass());
        }
    }

    /**
     * Same as {@link Bware}, yet another
     */
    @SuppressWarnings("unused")
    public interface Cware<T> {
        Privat p = new Privat(Cware.class);

        default String cw() {
            return p.get(this.getClass());
        }
    }

    /**
     * Same as {@link Cware}, yet another
     */
    @SuppressWarnings("unused")
    public interface Dware<T> {
        Privat p = new Privat(Dware.class);

        default String dw() {
            return p.get(this.getClass());
        }
    }

    /**
     * Same as {@link Dware}, yet another, and I think {@code A, B, C, D, E}, five of
     * them is enough
     */
    @SuppressWarnings("unused")
    public interface Eware<T> {
        Privat p = new Privat(Eware.class);

        default String ew() {
            return p.get(this.getClass());
        }
    }
}
