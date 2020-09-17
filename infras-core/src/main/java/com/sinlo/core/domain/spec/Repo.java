package com.sinlo.core.domain.spec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Repo the infrastructure repository
 *
 * @author sinlo
 */
public interface Repo<T extends Entity> {

    void create(T t);

    void update(T t);

    void delete(T t);

    /**
     * @return the type name of entity class which is supported by this repo
     */
    default String support() {
        return support(this.getClass());
    }

    /**
     * @return the type name of the given entity class which is supported by this repo
     */
    static String support(Class<?> clz) {
        String support = EntityName.mapping.get(clz.getName());
        if (support != null) return support;

        Type[] interfaces = clz.getGenericInterfaces();
        if (interfaces.length == 0) return EntityName.NA;

        for (Type type : interfaces) {
            if (type.getTypeName().startsWith(Repo.class.getName())) {
                support = ((ParameterizedType) type).getActualTypeArguments()[0].getTypeName();
                EntityName.mapping.put(clz.getName(), support);
                return support;
            }
        }
        return EntityName.NA;
    }

    class EntityName {
        private static final Map<String, String> mapping = new HashMap<>();
        public static final String NA = "NOT.AVAILABLE";
    }
}
