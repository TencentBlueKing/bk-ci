/*
 * Copyright (c) 2017. Tencent BlueKing
 */

package com.tencent.bk.devops.atom.utils.json;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.google.common.collect.Maps;
import com.tencent.bk.devops.atom.utils.json.annotation.SkipLogField;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Json工具类
 */
@SuppressWarnings("all")
public class JsonUtil {

    private static final Map<String, JsonMapper> jsonMappers = Maps.newConcurrentMap();

    /**
     * 序列化时忽略bean中的某些字段,字段需要用注解SkipLogFields包括
     *
     * @param bean 对象
     * @param <T>  对象类型
     * @return Json字符串
     * @see SkipLogField
     */
    public static <T> String skipLogFields(T bean) {
        return jsonMappers.computeIfAbsent("__skipLogFields__" + bean.getClass().getName(), (String s) -> {
            JsonMapper nonEmptyMapper = JsonMapper.nonEmptyMapper();
            Class<?> aClass = bean.getClass();
            Set<String> skipFields = new HashSet<>();
            while (aClass != null) {
                Field[] fields = aClass.getDeclaredFields();
                for (Field field : fields) {
                    SkipLogField fieldAnnotation = field.getAnnotation(SkipLogField.class);
                    if (fieldAnnotation == null) {
                        continue;
                    }
                    if (fieldAnnotation.value().trim().length() > 0) {
                        skipFields.add(fieldAnnotation.value());
                    } else {
                        skipFields.add(field.getName());
                    }
                }
                aClass = aClass.getSuperclass();
            }
            if (!skipFields.isEmpty()) {
                nonEmptyMapper.getMapper().addMixIn(bean.getClass(), SkipLogField.class);
                // 仅包含
                FilterProvider filterProvider = new SimpleFilterProvider()
                        .addFilter(SkipLogField.class.getAnnotation(JsonFilter.class).value(),
                                SimpleBeanPropertyFilter.serializeAllExcept(skipFields));
                nonEmptyMapper.getMapper().setFilterProvider(filterProvider);

            }
            return nonEmptyMapper;
        }).toJson(bean);
    }

    /**
     * 从Json串中解析成bean对象,支持参数泛型
     *
     * @param jsonString    Json字符串
     * @param typeReference 对象类
     * @param <T>           对象类型
     * @return  对象
     */
    public static <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
        return jsonMappers.computeIfAbsent("__all__", s -> JsonMapper.allOutPutMapper()).fromJson(jsonString, typeReference);
    }

    /**
     * 从Json串中解析成bean对象
     *
     * @param jsonString Json字符串
     * @param beanClass  对象类
     * @param <T>        对象类型
     * @return 对象
     */
    public static <T> T fromJson(String jsonString, Class<T> beanClass) {
        return jsonMappers.computeIfAbsent("__all__", s -> JsonMapper.allOutPutMapper()).fromJson(jsonString, beanClass);
    }

    /**
     * 创建输出所有字段的Json，不管字段值是默认值 还是等于 null 还是空集合的字段，全输出,可用于外部接口协议输出
     *
     * @param bean 对象
     * @param <T>  对象类型
     * @return Json字符串
     */
    public static <T> String toJson(T bean) {
        return jsonMappers.computeIfAbsent("__all__", s -> JsonMapper.allOutPutMapper()).toJson(bean);
    }

    /**
     * 注意，此只输出一个bean对象中字段值不为null的字段值才会序列到json，可用于外部系统协议输出。
     *
     * @param bean 对象
     * @param <T>  对象类型
     * @return Json字符串
     */
    public static <T> String toNonEmptyJson(T bean) {
        return jsonMappers.computeIfAbsent("__non_empty__", s -> JsonMapper.nonEmptyMapper()).toJson(bean);
    }
}
