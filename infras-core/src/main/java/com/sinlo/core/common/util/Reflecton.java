package com.sinlo.core.common.util;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reflecton the reflection util
 *
 * @author sinlo
 */
public class Reflecton {

    private Reflecton() {
    }

    public static Object getFieldValue(Object tar, String name) {
        return getFieldValue(tar, name, null, true);
    }

    public static boolean setFieldValue(Object tar, String name, Object value) {
        return setFieldValue(tar, name, value, null, true);
    }

    public static Object getFieldValue(Object tar, Field field) {
        return getFieldValue(tar, null, field, true);
    }

    public static boolean setFieldValue(Object tar, Field field, Object value) {
        return setFieldValue(tar, null, value, field, true);
    }

    /**
     * 取得目标对象指定字段名的字段的值
     * Get the [ value ] of the field of [ tar ]
     *
     * @param name      字段名，可以为空<br/> file name, could be null
     * @param useGetter 是否使用getter<br/> use getter or not
     */
    public static Object getFieldValue(Object tar, String name, Field field, boolean useGetter) {
        if (tar != null) {
            try {
                if (useGetter) {//优先使用getter
                    Method getter = getter(tar.getClass(), name == null ? field.getName() : name);
                    if (getter != null) {
                        return getter.invoke(tar);
                    }
                }
                if (field == null) field = getField(tar.getClass(), name);
                field.setAccessible(true);
                return field.get(tar);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 设置目标对象指定字段名的字段的值
     * Set the [ value ] of the field of [ tar ]
     *
     * @param name      字段名，可以为空<br/> file name, could be null
     * @param useSetter 是否使用setter<br/> use setter or not
     */
    public static boolean setFieldValue(Object tar, String name, Object value, Field field, boolean useSetter) {
        if (tar != null && value != null) {
            try {
                if (useSetter) {
                    Method setter = setter(tar.getClass(), name == null ? field.getName() : name, value.getClass());
                    if (setter != null) {
                        setter.invoke(tar, value);
                        return true;
                    }
                }
                if (field == null) field = getField(tar.getClass(), name);
                field.setAccessible(true);
                field.set(tar, value);
                return true;
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 连接两个字符串，连接处大写
     * Concatenate two given string, and uppercase the joint character
     *
     * @param prefix 前缀字符串
     * @param suffix 后缀字符串
     */
    public static String humping(String prefix, String suffix) {
        if (prefix == null) prefix = "";
        if (suffix == null || suffix.isEmpty()) return prefix;
        String joint = suffix.substring(0, 1).toUpperCase(); //连接处首字母大写
        return prefix + joint + suffix.substring(1);
    }

    /**
     * 驼峰转下划线
     * CamelCase to underscore_case
     *
     * @param name 符合驼峰命名规则的源名称 <br/> a camel cased name
     * @return 符合下划线命名规则的名称 <br/> a underscore cased name
     */
    public static String underscore(String name) {
        if (name != null && !name.isEmpty()) {
            StringBuilder builder = new StringBuilder().append(name.charAt(0));
            for (int i = 1; i < name.length(); i++) {
                if (Character.isUpperCase(name.charAt(i))) {//大写转下划线
                    builder.append('_').append(Character.toLowerCase(name.charAt(i)));
                } else {
                    builder.append(name.charAt(i));
                }
            }
            return builder.toString();
        }
        return name;
    }

    /**
     * 下划线转驼峰
     * underscore_case to CamelCase
     *
     * @param name   符合下划线命名规则的名称 <br/> a underscore cased name
     * @param pascal 是否帕斯卡命名法 <br/> pascal case styled camel case
     * @return 符合驼峰命名规则的源名称 <br/> a camel cased name
     */
    public static String cameling(String name, boolean pascal) {
        if (name != null && !name.isEmpty()) {
            StringBuilder builder = new StringBuilder()
                    .append(pascal ? Character.toLowerCase(name.charAt(0)) : Character.toUpperCase(name.charAt(0)));
            for (int i = 1; i < name.length(); i++) {
                if (name.charAt(i) == '_') {
                    builder.append(Character.toUpperCase(name.charAt(++i)));
                } else {
                    builder.append(name.charAt(i));
                }
            }
        }
        return name;
    }

    /**
     * 首字母小写
     * Initial character to lowercase
     */
    public static String lowerInitial(String str) {
        return castInitial(str, false);
    }

    /**
     * 首字母大写
     * Initial character to uppercase
     */
    public static String upperInitial(String str) {
        return castInitial(str, true);
    }

    /**
     * 首字母大小写转换
     * Initial character to lower or upper case
     */
    public static String castInitial(String str, boolean capital) {
        if (str == null || str.isEmpty()) return str;
        String initial = str.substring(0, 1);
        return (capital ? initial.toUpperCase() : initial.toLowerCase())
                + str.substring(1);
    }

    /**
     * 取得目标类中指定字段的getter
     * Get getter
     */
    public static Method getter(Class<?> tar, String fieldName) {
        try {
            return tar.getMethod(humping("get", fieldName));
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    /**
     * 取得目标类中指定字段的setter
     * Get setter
     */
    public static Method setter(Class<?> tar, String fieldName, Class<?> fieldClass) {
        try {
            return tar.getMethod(humping("set", fieldName), fieldClass);
        } catch (NoSuchMethodException ignored) {
        }
        return null;
    }

    /**
     * 取得目标对象指定字段名的字段
     * Get field
     */
    public static Field getField(Class<?> tar, String name) {
        if (tar != null) {
            for (; !Object.class.equals(tar); tar = tar.getSuperclass()) {
                try {
                    return tar.getDeclaredField(name);
                } catch (NoSuchFieldException | SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 找到第一个匹配的注解
     * Find first matching annotation
     */
    public static <T extends Annotation> T findFirstAnnotation(Class<?> tar, Class<T> annotationClass) {
        if (tar != null) {
            for (Field field : tar.getDeclaredFields()) {
                T annot = field.getAnnotation(annotationClass);
                if (annot != null) {
                    return annot;
                }
            }
        }
        return null;
    }

    /**
     * Backport of java.lang.reflect.Method#isDefault()
     */
    public static boolean isDefaultMethod(Method method) {
        return ((method.getModifiers()
                & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
                && method.getDeclaringClass().isInterface();
    }

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(T t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(t);
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
