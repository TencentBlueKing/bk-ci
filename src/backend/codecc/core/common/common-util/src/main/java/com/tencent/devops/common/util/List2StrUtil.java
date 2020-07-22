package com.tencent.devops.common.util;

import jersey.repackaged.com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * List转String工具类
 *
 * @author harryzhu
 * @version V4.0
 * @date 2019/2/22
 */
public class List2StrUtil
{
    /**
     * List转分号分隔String
     *
     * @param list
     * @return
     */
    public static String toString(Collection<String> list, String spliter)
    {
        StringBuffer resultBuf = new StringBuffer();
        if (CollectionUtils.isNotEmpty(list))
        {
            for (String item : list)
            {
                resultBuf.append(item).append(spliter);
            }
        }
        String resultStr = resultBuf.toString();
        if (StringUtils.isNotEmpty(resultStr))
        {
            return resultStr.substring(0, resultStr.length() - 1);
        }
        return "";
    }

    /**
     * String转List
     *
     * @param source
     * @return
     */
    public static List<String> fromString(String source, String spliter)
    {
        List<String> resultList = new ArrayList<>();
        if (StringUtils.isNotEmpty(source))
        {
            for (String owner : source.split(spliter))
            {
                resultList.add(owner);
            }
        }
        return resultList;
    }

    /**
     * 过滤重复的元素
     *
     * @param source
     * @param spliter
     * @return
     */
    public static String filterDup(String source, String spliter)
    {
        //IP地址去重
        if (StringUtils.isNotEmpty(source))
        {
            Set<String> bindIpSet = Sets.newHashSet();
            for (String bindIp : source.split(spliter))
            {
                bindIpSet.add(bindIp);
            }
            if (CollectionUtils.isNotEmpty(bindIpSet))
            {
                StringBuffer filteredBindIpStr = new StringBuffer();
                for (String bindIp : bindIpSet)
                {
                    filteredBindIpStr.append(bindIp).append(spliter);
                }
                source = filteredBindIpStr.toString();
                if (source.endsWith(spliter))
                {
                    source = source.substring(0, source.lastIndexOf(spliter));
                }
            }
        }
        return source;
    }
}
