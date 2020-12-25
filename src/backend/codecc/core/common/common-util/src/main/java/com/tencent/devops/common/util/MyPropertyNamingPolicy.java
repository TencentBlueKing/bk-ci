package com.tencent.devops.common.util;

import com.google.gson.FieldNamingStrategy;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;

/**
 * jackson serialization tool, reference: https://www.cnblogs.com/hujunzheng/p/6959182.html
 *
 * @version V3.5.0
 * @date 2018/12/14
 */
public enum MyPropertyNamingPolicy implements FieldNamingStrategy
{

    UNDER_SCORE_POLICY
            {
                @Override
                public String translateName(Field f)
                {
                    return underscoreName(f.getName());
                }
            },

    CAMEL_SCORE_POLICY
            {
                @Override
                public String translateName(Field f)
                {
                    return withoutUnderscoreName(f.getName());
                }
            };


    private static String underscoreName(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(name.substring(0, 1).toLowerCase());
        for (int i = 1; i < name.length(); ++i)
        {
            String s = name.substring(i, i + 1);
            String slc = s.toLowerCase();
            if (!(s.equals(slc)))
            {
                result.append("_").append(slc);
            }
            else
            {
                result.append(s);
            }
        }
        return result.toString();
    }

    private static String withoutUnderscoreName(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(name.substring(0, 1).toLowerCase());
        boolean underscore = false;
        for (int i = 1; i < name.length(); ++i)
        {
            String s = name.substring(i, i + 1);
            if ("_".equals(s))
            {
                underscore = true;
                continue;
            }
            else
            {
                if (underscore)
                {
                    s = s.toUpperCase();
                }
                underscore = false;
            }
            result.append(s);
        }
        return result.toString();
    }

}