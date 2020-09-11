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

    /* ========================= 实例处理 ========================== */

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
     *
     * @param tar  目标对象
     * @param name 字段名
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
     *
     * @param tar   目标对象
     * @param name  指定属性名
     * @param value 属性值
     * @return <b>true</b>: 设置成功
     * <b>false</b>: 设置失败，对象及其继承链中不存在指定的属性
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

    /* ========================== 命名处理 ===========================*/

    /**
     * 关键字连接处大写
     *
     * @param prefix 前缀关键字
     * @param suffix 后缀关键字
     */
    public static String humping(String prefix, String suffix) {
        if (prefix == null) prefix = "";
        if (suffix == null || suffix.isEmpty()) return prefix;
        String joint = suffix.substring(0, 1).toUpperCase();//连接处首字母大写
        return prefix + joint + suffix.substring(1);
    }

    /**
     * 驼峰转下划线
     *
     * @param name 符合驼峰命名规则的源名称
     * @return 符合下划线命名规则的名称
     */
    public static String underlining(String name) {
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
     *
     * @param name 符合下划线命名规则的名称
     * @param lite 是否小驼峰
     * @return 符合驼峰命名规则的名称
     */
    public static String cameling(String name, boolean lite) {
        if (name != null && !name.isEmpty()) {
            StringBuilder builder = new StringBuilder()
                    .append(lite ? Character.toLowerCase(name.charAt(0)) : Character.toUpperCase(name.charAt(0)));
            for (int i = 1; i < name.length(); i++) {
                if (name.charAt(i) == '_') {
                    //如果是下划线，跳至下个字符，且将其大写加入builder
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
     */
    public static String lowerInitial(String str) {
        return castInitial(str, false);
    }

    /**
     * 首字母大写
     */
    public static String upperInitial(String str) {
        return castInitial(str, true);
    }

    /**
     * 首字母大小写转换
     *
     * @param str     需处理的字符串
     * @param capital 大小写标识，true为大写，false为小写
     * @return 转换后的字符串
     */
    public static String castInitial(String str, boolean capital) {
        if (str == null || str.isEmpty()) return str;
        String initial = str.substring(0, 1);
        return (capital ? initial.toUpperCase() : initial.toLowerCase())
                + str.substring(1);
    }

    /* ====================== 元信息获取 ======================= */

    /**
     * 取得目标类中指定字段的getter
     *
     * @param tar       目标类
     * @param fieldName 字段名
     * @return getter
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
     *
     * @param tar        目标类
     * @param fieldName  字段名
     * @param fieldClass 字段类型
     * @return setter
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
     *
     * @param tar  目标对象
     * @param name 字段名
     * @return 找到的field
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
     *
     * @param tar             目标类Class
     * @param annotationClass 注解类Class
     * @param <T>             注解类的范型
     * @return 找到则返回相应的注解，否则返回null
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
     * public的，非abstract的，非static的，定义在interface里面，实现在类里面的方法
     */
    public static boolean isDefaultMethod(Method method) {
        return ((method.getModifiers()
                & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC)
                && method.getDeclaringClass().isInterface();
    }

    /* ===================== 对象重构 ======================= */

    @SuppressWarnings("unchecked")
    public static <T> T deepClone(T t) {
        //将对象写到流里
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(t);
            //从流里读回来
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
