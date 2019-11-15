/*
 * Copyright (c) 2017. Tencent BlueKing
 */

package com.tencent.bk.devops.atom.utils.json;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 封装不同的输出风格的Jackson 使用不同的builder函数创建实例.
 */
@SuppressWarnings("all")
public class JsonMapper {

    private ObjectMapper mapper;

    private JsonMapper(Include include) {
        mapper = new ObjectMapper();
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }
        // 遇到在Bean类中没有的Json字段时不报错。
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * Jackson Mapper实例
     *
     * @return ObjectMapper
     */
    public ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * 创建只输出非Null的属性 并且非Empty的集合属性 到Json字符串的Mapper,建议在外部接口中使用.
     * 注意：如果参数值为空的话，则字段不会在JSON串中出现，所以对外部时也要注意使用。
     *
     * @return JsonMapper
     */
    public static JsonMapper nonEmptyMapper() {
        return build(Include.NON_EMPTY);
    }

    /**
     * 创建输出所有字段的Mapper，不管是默认值 还是 null empty的字段，最全的输出
     *
     * @return JsonMapper
     */
    public static JsonMapper allOutPutMapper() {
        return build(Include.ALWAYS);
    }

    /**
     * 构建出希望使用的Jackson
     *
     * @param include Include
     * @return JsonMapper
     */
    public static JsonMapper build(Include include) {
        return new JsonMapper(include);
    }

    /**
     * Object可以是Bean，也可以是Collection或数组。
     * 如果对象为Null, 返回"null".
     * 如果集合为空集合, 返回"[]".
     *
     * @param object object
     * @return String
     */
    public String toJson(Object object) {

        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 反序列化简单的Bean对象
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(String, TypeReference)
     *
     * @see #fromJson(String, TypeReference)
     */
    public <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return mapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 反序列化复杂的泛型对象
     * <p>
     * 如果JSON字符串为Null或"null"字符串, 返回Null.
     * 如果JSON字符串为"[]", 返回空集合.
     */
    public <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return mapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            return null;
        }
    }
}
