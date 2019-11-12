/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.util;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class PathUtils
{
    private static Logger logger = LoggerFactory.getLogger(PathUtils.class);

    private static final int MIN_LENGTH = 6;
    public static final int DIR = 1;
    public static final int FILE = 2;

    public static void convertPaths(List<String> paths, ArrayList<String> storeArray, int type)
    {
        if (storeArray == null || paths == null)
        {
            return;
        }

        String postfix = "";
        if (type == DIR)
        {
            postfix = ".*";
        }

        for (String path : paths)
        {
            int wordCount = getWordCount(path, "/");
            int startWord = 0;
            int startIndex = 0;
            if (wordCount >= MIN_LENGTH)
            {
                startWord = wordCount - (wordCount * 2 / 3) + 1;
                char[] words = path.toCharArray();
                startIndex = getStartIndex(startWord, words, '/');
            }
            String regexPath = ".*" + path.substring(startIndex) + postfix;
            if (regexPath.contains("("))
            {
                regexPath = regexPath.replace("(", "\\(").replace(")", "\\)");
            }
            logger.debug("will add new ignore path:%s", regexPath);
            storeArray.add(regexPath);
        }
    }

    private static int getWordCount(String string, String splitChar)
    {
        return string.split(splitChar).length;
    }

    private static int getStartIndex(int startWord, char[] words, char splitChar)
    {
        if (words == null || words.length <= 0)
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < words.length; i++)
        {
            if (words[i] == splitChar)
            {
                if (++count == startWord)
                {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * 将文件的绝对路径转换成相对路径
     *
     * @param streamName
     * @param toolName
     * @param defectPaths
     * @return
     */
    public static Set<String> convertAbsolutePath2RelativePath(String streamName, String toolName, Set<String> defectPaths)
    {
        Set<String> relativePaths = new HashSet<String>(defectPaths.size());

        String splitRegex = streamName + "_" + toolName.toLowerCase();

        for (String absolutePath : defectPaths)
        {
            int i = absolutePath.lastIndexOf(splitRegex) + splitRegex.length();

            String relativePath = absolutePath.substring(i);
            relativePaths.add(relativePath);
        }

        return relativePaths;
    }

    /**
     * 将文件的绝对路径转换成相对路径
     *
     * @param streamName
     * @param toolName
     * @return
     */
    public static String convertAbsolutePath2RelativePath(String streamName, String toolName, String defectPath)
    {
        String splitRegex = streamName + "_" + toolName.toLowerCase();
        int i = defectPath.lastIndexOf(splitRegex) + splitRegex.length();

        String relativePath = defectPath.substring(i, defectPath.length());

        return relativePath;
    }

    /**
     * 根据工具侧上报的url和rel_path，截取url得到前面的值，然后拿最后一个/下的值
     *
     * @param url
     * @param relPath
     * @return
     */
    public static String getRelativePath(String url, String relPath)
    {
        if (org.apache.commons.lang.StringUtils.isEmpty(url) || org.apache.commons.lang.StringUtils.isEmpty(relPath))
        {
            return "";
        }

        if (!relPath.startsWith("/"))
        {
            relPath = "/" + relPath;
        }

        int index = url.lastIndexOf(relPath);
        if (index < 0)
        {
            return "";
        }
        //String tmpPath = url.substring(0, index);
        //return tmpPath.substring(tmpPath.lastIndexOf("/")) + relPath;

        return relPath;
    }

    /**
     * 检测路径是否匹配某个过滤路径
     *
     * @param path
     * @param ignorePaths
     * @return
     */
    public static boolean checkIfMaskByPath(String path, List<String> ignorePaths)
    {
        for (String regrex : ignorePaths)
        {
            if (path.matches(regrex))
            {
                return true;
            }
        }
        return false;
    }
}
