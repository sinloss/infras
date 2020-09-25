package com.sinlo.core.common.util;

import com.sinlo.core.common.wraparound.Node;

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
     * Get a {@link ChainMap generic chain map} of the given prototype
     */
    public static ChainMap chainMap(Class<?> prototype) {
        return chains(prototype, new Chains()).toMap();
    }

    private static Chains chains(Class<?> prototype, Chains chains) {
        Type[] supers = Reflecton.getGenericSupers(prototype);
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

        public final Class<?> clz;
        public final Type type;

        public TypeNode(Class<?> clz, Type type) {
            this.clz = clz;
            this.type = type;
        }

    }


    private static class Chains {

        private final List<TypeNode> roots = new ArrayList<>();

        private Chains link(TypeNode aim, TypeNode next) {
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
            return this;
        }

        private ChainMap toMap() {
            return new ChainMap(roots.stream()
                    .collect(Collectors
                            .toMap(r -> ChainMap.key(r.prev()),
                                    r -> r)));
        }
    }

}
