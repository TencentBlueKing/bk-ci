package com.tencent.devops.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class GsonUtils
{
    private static Logger logger = LoggerFactory.getLogger(GsonUtils.class);

    public static String toJson(Object obj)
    {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }

    public static String toCamelJson(Object obj)
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingStrategy(MyPropertyNamingPolicy.CAMEL_SCORE_POLICY);
        Gson gson = gsonBuilder.create();
        return gson.toJson(obj);
    }

    public static String toUnderScoreJson(Object obj)
    {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingStrategy(MyPropertyNamingPolicy.UNDER_SCORE_POLICY);
        Gson gson = gsonBuilder.create();
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String jsonStr, Class<T> classOfT)
    {
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, classOfT);
    }

    public static <T> T fromJson(String jsonStr, Type type)
    {
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, type);
    }

    public static String getValue(String key, String strJson)
    {
        JsonParser p = new JsonParser();
        JsonElement value = p.parse(strJson).getAsJsonObject().get(key);
        if (value == null)
        {
            return null;
        }
        String strValue = null;
        try
        {
            strValue = value.getAsString();
        }
        catch (Exception e)
        {
            logger.error("getValue from json exception", e);
        }
        return strValue;
    }

    /**
     * 对象转换成Json字符串，并且避免把html标签转换为Unicode转义字符
     *
     * @param obj
     * @return
     * @author austinshen
     * @date 2017/10/3
     * @version V2.4
     */
    public static String toJsonDisableHtmlEscaping(Object obj)
    {
        // Gson会把html标签转换为Unicode转义字符。避免转义的使用方法是：
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(obj);
    }


    /**
     * gson默认不序列化null值，需要的时候可序列化,且null值转为空
     *
     * @version V3.2.0
     * @date 2018/9/7
     */
    public static String toJsonSerialNullsAndDisableHtml(Object obj)
    {
        Gson gson = new GsonBuilder().serializeNulls().
                disableHtmlEscaping().create();
        return gson.toJson(obj);
    }


    public static <T> T fromJsonDisableHtmlEscaping(String jsonStr, Class<T> classOfT)
    {
        // Gson会把html标签转换为Unicode转义字符。避免转义的使用方法是：
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.fromJson(jsonStr, classOfT);
    }

    public static <T> T fromJsonDisableHtmlEscaping(String jsonStr, Type type)
    {
        // Gson会把html标签转换为Unicode转义字符。避免转义的使用方法是：
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.fromJson(jsonStr, type);
    }

    /**
     * @param jsonStr
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T fromJsonPreventChangeIntegerToDouble(String jsonStr, Type type)
    {
        Gson gson = new GsonBuilder().
                registerTypeAdapter(Double.class, new JsonSerializer<Double>()
                {

                    @Override
                    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers)
                    {

                    }

                    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context)
                    {
                        if (src == src.longValue())
                        {
                            return new JsonPrimitive(src.longValue());
                        }
                        return new JsonPrimitive(src);
                    }
                }).create();
        return gson.fromJson(jsonStr, type);
    }

}
