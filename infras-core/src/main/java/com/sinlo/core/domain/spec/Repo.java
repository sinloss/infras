package com.sinlo.core.domain.spec;

import com.sinlo.core.common.util.Genericia;

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

        Genericia.TypeNode tn = Genericia.chainMap(clz).get(Repo.class, 0);
        if (tn != null && tn.type instanceof Class) {
            EntityName.mapping.put(clz.getName(), support = tn.type.getTypeName());
            return support;
        }
        return EntityName.NA;
    }

    class EntityName {
        private static final Map<String, String> mapping = new HashMap<>();
        public static final String NA = "NOT.AVAILABLE";
    }
}
