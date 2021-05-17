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


import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


public class PathUtils
{
    private static Logger logger = LoggerFactory.getLogger(PathUtils.class);

    private static final int MIN_LENGTH = 6;
    public static final int DIR = 1;
    public static final int FILE = 2;

    public static final String REGEX_GITHUB_SSH = "git@github.com:(.+).git$";
    public static final String REGEX_GITLAB_SSH = "git@gitlab.com:(.+).git$";
    public static final String REGEX_GIT_SSH = "git@git\\.(.+?):(.+).git$";

    public static final String REGEX_GITHUB_HTTP_WITH_CERT = "https://(.+)@github(.+).git$";
    public static final String REGEX_GITLAB_HTTP_WITH_CERT = "https://(.+)@gitlab(.+).git$";
    public static final String REGEX_GIT_HTTP_WITH_CERT = "http://(.+)@git\\.(.+).git$";

    // 没有.git后缀的github地址需要拼上.git
    public static final String REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX = "http.*://github(.+)(?<!\\.git)$";

    // 没有.git后缀的gitlab地址需要拼上.git
    public static final String REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX = "http.*://gitlab(.+)(?<!\\.git)$";

    // 没有.git后缀的git地址需要拼上.git
    public static final String REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX = "http.*://git\\.(.+)(?<!\\.git)$";

    // http的github路径需要把s加上
    public static final String REGEX_GITHUB_HTTP = "http://github(.+).git$";

    // http的gitlab路径需要把s加上
    public static final String REGEX_GITLAB_HTTP = "http://gitlab(.+).git$";

    // https的git路径需要把s去掉
    public static final String REGEX_GIT_HTTPS = "https://git\\.(.+).git$";

    public static final String REGEX_SVN_SSH = "svn\\+ssh\\:\\/\\/\\S+@(.+)";

    public static final String REGEX_WIN_PATH_PREFIX = "^[a-zA-Z]:/(.+)";

    public static final String REGEX_KLOCWORK_WIN_PATH_PREFIX = "^/[a-zA-Z]/(.+)";

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
        if (StringUtils.isBlank(url) || StringUtils.isBlank(relPath))
        {
            return "";
        }

        if (!relPath.startsWith("/"))
        {
            relPath = "/" + relPath;
        }

        String root;
        int index = url.lastIndexOf(relPath);
        if (index < 0)
        {
            root = url.substring(url.lastIndexOf("/"));
        }
        else
        {
            String tmpPath = url.substring(0, index);
            root = tmpPath.substring(tmpPath.lastIndexOf("/"));
        }
        return root + relPath;
    }

    /**
     * 根据工具侧上报的url和rel_path，获取文件的完整url
     * 返回格式：http://github.com/xxx/website/blob/branch/xxx/xxx.java
     *
     * @param url
     * @param branch
     * @param relPath
     * @return
     */
    public static String getFileUrl(String url, String branch, String relPath)
    {
        if (StringUtils.isBlank(url) || StringUtils.isBlank(branch) || StringUtils.isBlank(relPath))
        {
            return "";
        }

        if (url.endsWith(".git"))
        {
            if (!relPath.startsWith("/"))
            {
                relPath = "/" + relPath;
            }

            url = url + relPath;
        }

        return formatFileRepoUrlToHttp(url).replace(".git", "/blob/"+branch);
    }

    /**
     * 将路径转成全小写
     *
     * @param paths
     */
    public static Set<String> convertPathsToLowerCase(Set<String> paths)
    {
        Set<String> lowerCasePathSet = new HashSet<>(paths.size());
        paths.forEach(
                path -> lowerCasePathSet.add(path.toLowerCase())
        );
        return lowerCasePathSet;
    }

    /**
     * 检测路径是否匹配某个过滤路径
     *
     * @param path
     * @param filterPaths
     * @return
     */
    public static boolean checkIfMaskByPath(String path, Set<String> filterPaths) {
        if (StringUtils.isEmpty(path)) {
            return false;
        }
        try
        {
            for (String regrex : filterPaths)
            {
                if (path.contains(regrex) || path.matches(regrex))
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            logger.info("invalid regex expression: {}, {}", path, filterPaths);
        }
        return false;
    }

    /**
     * 将代码仓库地址转换成http规范地址
     * @param url
     * @return
     */
    public static String formatRepoUrlToHttp(String url)
    {
        if (StringUtils.isNotEmpty(url))
        {
            if (Pattern.matches(REGEX_GITHUB_SSH, url))
            {
                url = url.replaceAll(REGEX_GITHUB_SSH, "https://github.com/$1.git");
            }
            else if (Pattern.matches(REGEX_GITLAB_SSH, url))
            {
                url = url.replaceAll(REGEX_GITLAB_SSH, "https://gitlab.com/$1.git");
            }
            else if (Pattern.matches(REGEX_GIT_SSH, url))
            {
                url = url.replaceAll(REGEX_GIT_SSH, "http://git\\.$1/$2.git");
            }
            else if (Pattern.matches(REGEX_GITHUB_HTTP_WITH_CERT, url))
            {
                url = url.replaceAll(REGEX_GITHUB_HTTP_WITH_CERT, "https://github$2.git");
            }
            else if (Pattern.matches(REGEX_GITLAB_HTTP_WITH_CERT, url))
            {
                url = url.replaceAll(REGEX_GITLAB_HTTP_WITH_CERT, "https://gitlab$2.git");
            }
            else if (Pattern.matches(REGEX_GIT_HTTP_WITH_CERT, url))
            {
                url = url.replaceAll(REGEX_GIT_HTTP_WITH_CERT, "http://git\\.$2.git");
            }
            else if (Pattern.matches(REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX, url))
            {
                url = url.replaceAll(REGEX_GITHUB_HTTP_WITHOUT_GIT_SUFFIX, "https://github$1.git");
            }
            else if (Pattern.matches(REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX, url))
            {
                url = url.replaceAll(REGEX_GITLAB_HTTP_WITHOUT_GIT_SUFFIX, "https://gitlab$1.git");
            }
            else if (Pattern.matches(REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX, url))
            {
                url = url.replaceAll(REGEX_GIT_HTTP_WITHOUT_GIT_SUFFIX, "http://git\\.$1.git");
            }
            else if (Pattern.matches(REGEX_GITHUB_HTTP, url))
            {
                url = url.replaceAll("http://github", "https://github");
            }
            else if (Pattern.matches(REGEX_GITLAB_HTTP, url))
            {
                url = url.replaceAll("http://gitlab", "https://gitlab");
            }
            else if (Pattern.matches(REGEX_GIT_HTTPS, url))
            {
                url = url.replaceAll(REGEX_GIT_HTTPS, "http://git\\.$1.git");
            }
            else if (Pattern.matches(REGEX_SVN_SSH, url))
            {
                url = url.replaceAll(REGEX_SVN_SSH, "http://$1");
            }
        }
        return url;
    }

    /**
     * 将代码文件的url地址转换成http规范地址
     * @param fileUrl
     * @return
     */
    public static String formatFileRepoUrlToHttp(String fileUrl)
    {
        if (StringUtils.isNotEmpty(fileUrl))
        {
            if (Pattern.matches("git@github.com:(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("git@github.com:(.+).git(.+)", "https://github.com/$1.git$2");
            }
            else if (Pattern.matches("git@gitlab.com:(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("git@gitlab.com:(.+).git(.+)", "https://gitlab.com/$1.git$2");
            }
            else if (Pattern.matches("git@git\\.(.+?):(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("git@git\\.(.+?):(.+).git(.+)", "http://git\\.$1/$2.git$3");
            }
            else if (Pattern.matches("https://(.+)@github(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("https://(.+)@github(.+).git(.+)", "https://github$2.git$3");
            }
            else if (Pattern.matches("https://(.+)@gitlab(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("https://(.+)@gitlab(.+).git(.+)", "https://gitlab$2.git$3");
            }
            else if (Pattern.matches("http://(.+)@git\\.(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("http://(.+)@git\\.(.+).git(.+)", "http://git\\.$2.git$3");
            }
            else if (Pattern.matches("http://github(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("http://github(.+).git(.+)", "http://github\\.$1.git$2");
            }
            else if (Pattern.matches("http://gitlab(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("http://gitlab(.+).git(.+)", "http://gitlab\\.$1.git$2");
            }
            else if (Pattern.matches("https://git\\.(.+).git(.+)", fileUrl))
            {
                fileUrl = fileUrl.replaceAll("https://git\\.(.+).git(.+)", "http://git\\.$1.git$2");
            }
            else if (Pattern.matches(REGEX_SVN_SSH, fileUrl))
            {
                fileUrl = fileUrl.replaceAll(REGEX_SVN_SSH, "http://$1");
            }
        }
        return fileUrl;
    }

    /**
     * 去掉windows路径的盘号和冒号
     * 比如：D:/workspace/svnauth_svr/app/exception/CodeCCException.java
     * 转换为：/workspace/svnauth_svr/app/exception/CodeCCException.java
     * @param filePath
     * @return
     */
    public static String trimWinPathPrefix(String filePath)
    {
        if (StringUtils.isNotEmpty(filePath))
        {
            if (Pattern.matches(REGEX_WIN_PATH_PREFIX, filePath))
            {
                filePath = filePath.replaceAll(REGEX_WIN_PATH_PREFIX, "/$1");
            }
        }
        return filePath;
    }

    /**
     * 去掉klocwork告警的路径的windows盘号
     * 比如：/d/workspace/svnauth_svr/app/controllers/application.java
     * 转换为：/workspace/svnauth_svr/app/controllers/application.java
     * @param filePath
     * @return
     */
    public static String trimKlocworkWinPathPrefix(String filePath)
    {
        if (StringUtils.isNotEmpty(filePath))
        {
            if (Pattern.matches(REGEX_KLOCWORK_WIN_PATH_PREFIX, filePath))
            {
                filePath = filePath.replaceAll(REGEX_KLOCWORK_WIN_PATH_PREFIX, "/$1");
            }
        }
        return filePath;
    }

    public static String trimWinDifferentPath(String filePath)
    {
        if (StringUtils.isBlank(filePath)) return filePath;
        // d:/xxxx这种路径
        if (filePath.length() > 1 && filePath.charAt(1) == ':')
        {
            return trimWinPathPrefix(filePath);
        }
        else
        {
            return trimKlocworkWinPathPrefix(filePath);
        }
    }
}
