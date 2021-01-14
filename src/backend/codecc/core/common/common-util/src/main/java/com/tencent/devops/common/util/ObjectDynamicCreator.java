package com.tencent.devops.common.util;

import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 反射工具类
 *
 * @version V1.0
 * @date 2019/11/15
 */
public class ObjectDynamicCreator
{
    private static Logger logger = LoggerFactory.getLogger(ObjectDynamicCreator.class);
    /**
     * 返回由Map的key对属性，value对应值组成的对应
     *
     * @param map Map<String,String>
     * @param cls Class
     * @return obj Object
     * @throws Exception
     */
    public static <T> T setFieldValueBySetMethod(Map<String, String> map, Class<T> cls)
    {
        Field[] fields = cls.getDeclaredFields();
        T obj = null;
        try
        {
            obj = cls.newInstance();
            for (Field field : fields)
            {
                Class<?> clsType = field.getType();
                String name = field.getName();
                String strSet = "set" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
                Method methodSet = cls.getDeclaredMethod(strSet, clsType);
                if (map.containsKey(name))
                {
                    Object objValue = typeConversion(clsType, map.get(name));
                    methodSet.invoke(obj, objValue);
                }
            }
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e)
        {
            String errMsg = String.format("set map to class field failed! map: %s", JsonUtil.INSTANCE.toJson(map));
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, e);
        }
        return obj;
    }

    /**
     * 返回由Map的key对属性，value对应值组成的对应
     *
     * @param map Map<String,String>
     * @param cls Class
     * @return obj Object
     * @throws Exception
     */
    public static <T> T setFieldValue(Map<String, String> map, Class<T> cls)
    {
        Field[] fields = cls.getDeclaredFields();
        T obj;
        try
        {
            obj = cls.newInstance();
            for (Field field : fields)
            {
                Class<?> clsType = field.getType();
                String name = field.getName();
                if (map.containsKey(name))
                {
                    field.setAccessible(true);
                    Object objValue = typeConversion(clsType, map.get(name));
                    field.set(obj, objValue);
                }
            }
        }
        catch (IllegalAccessException | InstantiationException e)
        {
            String errMsg = String.format("set map to class field failed! map: %s", JsonUtil.INSTANCE.toJson(map));
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, e);
        }
        return obj;
    }


    /**
     * 复制非Null属性
     *
     * @param src
     * @param dest
     * @param cls
     */
    public static void copyNonNullPropertiesBySetMethod(Object src, Object dest, Class<?> cls)
    {
        Field[] fields = cls.getDeclaredFields();
        try
        {
            for (Field field : fields)
            {
                Class<?> clsType = field.getType();
                String name = field.getName();
                String strSet = "set" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
                String strGet = "get" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
                Method methodGet = cls.getDeclaredMethod(strGet);
                Method methodSet = cls.getDeclaredMethod(strSet, clsType);
                Object getResult = methodGet.invoke(src);
                if (getResult != null)
                {
                    methodSet.invoke(dest, getResult);
                }
            }
        }
        catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            String errMsg = String.format("copy non null properties failed! src: %s, dest: %s", JsonUtil.INSTANCE.toJson(src), JsonUtil.INSTANCE.toJson(dest));
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, e);
        }
    }

    /**
     * 复制非Null属性
     *
     * @param src
     * @param dest
     * @param cls
     */
    public static void copyNonNullProperties(Object src, Object dest, Class<?> cls, Set<String> fieldNames)
    {
        Field[] fields = cls.getDeclaredFields();
        try
        {
            for (Field field : fields)
            {
                field.setAccessible(true);
                Object getResult = field.get(src);
                if (fieldNames.contains(field.getName()) && getResult != null)
                {
                    field.set(dest, getResult);
                }
            }
        }
        catch (IllegalAccessException e)
        {
            String errMsg = String.format("copy non null properties failed! src: %s, dest: %s", JsonUtil.INSTANCE.toJson(src), JsonUtil.INSTANCE.toJson(dest));
            logger.error(errMsg);
            throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, e);
        }
    }

    public static Object typeConversion(Class<?> cls, String str)
    {
        Object obj = null;
        String nameType = cls.getSimpleName();

        if ("String".equals(nameType))
        {
            obj = str;
        }

        if ("Character".equals(nameType))
        {
            obj = str.charAt(1);
        }

        if (StringUtils.isNotEmpty(str))
        {
            if ("int".equals(nameType))
            {
                nameType = "Integer";
            }
            else if ("long".equals(nameType))
            {
                nameType = "Long";
            }

            if ("Integer".equals(nameType))
            {
                obj = Integer.valueOf(str);
            }

            if ("Float".equals(nameType))
            {
                obj = Float.valueOf(str);
            }
            if ("Double".equals(nameType))
            {
                obj = Double.valueOf(str);
            }

            if ("Boolean".equals(nameType))
            {
                obj = Boolean.valueOf(str);
            }
            if ("Long".equals(nameType))
            {
                obj = Long.valueOf(str);
            }

            if ("Short".equals(nameType))
            {
                obj = Short.valueOf(str);
            }
        }

        return obj;
    }
}
