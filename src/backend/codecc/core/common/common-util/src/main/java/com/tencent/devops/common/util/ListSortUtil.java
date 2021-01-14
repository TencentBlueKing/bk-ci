/*
 * Tencent is pleased to support the open source community by making BlueKing available.
 * Copyright (C) 2017-2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the MIT License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://opensource.org/licenses/MIT
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.devops.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * List通用排序工具类
 *
 * @version V1.0
 * @date 2019/10/30
 */
public class ListSortUtil
{
    private static Logger logger = LoggerFactory.getLogger(ListSortUtil.class);

    private static final String SORT_ASC = "asc";

    private static final String SORT_DESC = "desc";

    // 在使用正则表达式时，利用好其预编译功能，可以有效加快正则匹配速度。 说明：不要在方法体内定义：Pattern pattern = Pattern.compile(规则)
    private static Pattern NUMBER_PATTERN = Pattern.compile("^[+-]?[0-9]+$");

    /**
     * 对List数组排序
     *
     * @param list 源数据 排序集合
     * @param sort 升序 还是 降序，默认升序
     * @return List
     */
    public static List<?> sort(List<?> list, final String sort)
    {
        Collections.sort(list, (Comparator<Object>) (o1, o2) ->
        {
            int ret = 0;
            if (o1 instanceof Integer)
            {
                ret = ((Integer) o1).compareTo((Integer) o2);
            }
            else if (o1 instanceof Double)
            {
                ret = ((Double) o1).compareTo((Double) o2);
            }
            else if (o1 instanceof Long)
            {
                ret = ((Long) o1).compareTo((Long) o2);
            }
            else if (o1 instanceof Float)
            {
                ret = ((Float) o1).compareTo((Float) o2);
            }
            else if (o1 instanceof Date)
            {
                ret = ((Date) o1).compareTo((Date) o2);
            }
            else if (isDouble(String.valueOf(o1)) && isDouble(String.valueOf(o2)))
            {
                ret = (new Double(o1.toString())).compareTo(new Double(o2.toString()));
            }
            else
            {
                ret = String.valueOf(o1).compareTo(String.valueOf(o2));
            }
            if (null != sort && SORT_DESC.equalsIgnoreCase(sort))
            {
                return -ret;
            }
            else
            {
                return ret;
            }
        });
        return list;
    }


    /**
     * List 泛型 排序
     *
     * @param list  源数据 排序集合
     * @param field 排序的数据字段名称
     * @param sort  升序 还是 降序，默认升序
     * @param <T>   泛型T
     * @return List
     */
    public static <T> List<T> sort(List<T> list, final String field, final String sort)
    {
        Collections.sort(list, (o1, o2) ->
        {
            int ret = 0;
            try
            {
                Method method1 = o1.getClass().getDeclaredMethod(getMethodName(field), null);
                Method method2 = o2.getClass().getDeclaredMethod(getMethodName(field), null);
                Field field1 = o1.getClass().getDeclaredField(field);
                field1.setAccessible(true);
                Class<?> type = field1.getType();
                if (type == int.class)
                {
                    ret = ((Integer) field1.getInt(o1)).compareTo(field1.getInt(o2));
                }
                else if (type == double.class)
                {
                    ret = ((Double) field1.getDouble(o1)).compareTo(field1.getDouble(o2));
                }
                else if (type == long.class)
                {
                    ret = ((Long) field1.getLong(o1)).compareTo(field1.getLong(o2));
                }
                else if (type == float.class)
                {
                    ret = ((Float) field1.getFloat(o1)).compareTo(field1.getFloat(o2));
                }
                else if (type == Date.class)
                {
                    ret = ((Date) field1.get(o1)).compareTo((Date) field1.get(o2));
                }
                else if (isDouble(String.valueOf(field1.get(o1))) && isDouble(String.valueOf(field1.get(o2))))
                {
                    ret = (new Double(method1.invoke(o1).toString())).compareTo(new Double(method2.invoke(o2).toString()));
                }
                else if (getPercentage(String.valueOf(field1.get(o1))) != null && getPercentage(String.valueOf(field1.get(o2))) != null)
                {
                    ret = Double.compare(getPercentage(String.valueOf(field1.get(o1))), getPercentage(String.valueOf(field1.get(o2))));
                }
                else
                {
                    ret = String.valueOf(field1.get(o1)).compareToIgnoreCase(String.valueOf(field1.get(o2)));
                }

            }
            catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException e)
            {
                logger.error("sort [{}] list by [{}] [{}] exception:", o1.getClass(), field, sort, e);
            }

            if (StringUtils.isNotEmpty(sort) && SORT_DESC.equalsIgnoreCase(sort))
            {
                return -ret;
            }
            else
            {
                return ret;
            }
        });
        return list;
    }

    private static Double getPercentage(String str)
    {
        try {
            return NumberFormat.getPercentInstance().parse(str).doubleValue();
        } catch (ParseException e) {
            return null;
        }
    }


    private static boolean isDouble(String str)
    {
        boolean flag = false;
        if (isInteger(str) || isFloat(str))
        {
            flag = true;
        }
        return flag;
    }

    private static boolean isInteger(String str)
    {
        Matcher matcher = NUMBER_PATTERN.matcher(str);
        return matcher.find();
    }


    private static boolean isFloat(String str)
    {
        return str.matches("[\\d]+\\.[\\d]+");
    }


    /**
     * List 泛型 排序
     *
     * @param list   源数据 排序集合
     * @param fields 排序的数据字段名称
     * @param sorts  升序 还是 降序
     * @param <T>    泛型T
     * @return List
     */
    public static <T> List<T> sort(List<T> list, final String[] fields, final String[] sorts)
    {
        if (null != fields && fields.length > 0)
        {
            for (int index = 0; index < fields.length; index++)
            {
                String sortRule = SORT_ASC;
                if (null != sorts && sorts.length >= index && null != sorts[index])
                {
                    sortRule = sorts[index];
                }
                sort(list, fields[index], sortRule);
            }
        }
        return list;
    }

    private static String getMethodName(String str)
    {
        StringBuffer name = new StringBuffer();
        name = name.append("get").append(firstLetterToCapture(str));
        return name.toString();
    }

    private static String firstLetterToCapture(String name)
    {
        char[] arr = name.toCharArray();
        arr[0] -= 32;
        return String.valueOf(arr);
    }
}
