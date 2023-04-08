/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.util

import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.common.api.constant.I18NConstant.BK_ILLEGAL_PATH
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import java.net.URLEncoder
import java.nio.file.Paths
import java.util.regex.Pattern
import javax.ws.rs.BadRequestException

object PathUtils {
    fun checkAndNormalizeAbsPath(path: String): String {
        val normalizePath = Paths.get(path).normalize().toString()
        if (!normalizePath.startsWith("/")) {
            throw BadRequestException(I18nUtil.getCodeLanMessage(
                messageCode = BK_ILLEGAL_PATH
            ))
        }
        return normalizePath
    }

    fun normalize(relativePath: String): String {
        return Paths.get(relativePath).normalize().toString()
    }

    fun buildArchiveLink(projectId: String, pipelineId: String, buildId: String): String {
        return "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive" +
                "&projectId=$projectId&pipelineId=$pipelineId&buildId=$buildId"
    }

    fun buildDetailLink(projectId: String, artifactoryType: String, path: String): String {
        return "${HomeHostUtil.outerServerHost()}/share/artifactoryDetail/?flag=artifactoryDetail" +
                "&projectId=$projectId&artifactoryType=$artifactoryType" +
                "&x-devops-project-id=$projectId&artifactoryPath=${
                    URLEncoder.encode(
                        path,
                        "utf-8"
                    )
                }"
    }

    fun isFolder(path: String): Boolean {
        return path.endsWith("/")
    }

    fun getParentFolder(path: String): String {
        return if (isFolder(path)) path.removeSuffix("${getFileName(path)}/")
        else path.removeSuffix(getFileName(path))
    }

    fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    /**
     * 文件排序
     * 文件夹在文件之上
     * asc = true 文件名短在文件名长之上
     * asc = false 文件名短在文件名长之下
     */
    fun sort(fileInfoList: List<FileInfo>, asc: Boolean = true): List<FileInfo> {
        val ascInt = if (asc) 1 else -1
        return fileInfoList.sortedWith(Comparator { file1, file2 ->
            when {
                // 文件夹排在文件之上
                file1.folder && !file2.folder -> -1
                !file1.folder && file2.folder -> 1

                // 文件名短在文件名长之上
//                file1.name.length < file2.name.length -> -ascInt
//                file1.name.length > file2.name.length -> ascInt

                // 根据最后修改时间倒叙
                file1.modifiedTime < file2.modifiedTime -> ascInt
                file1.modifiedTime > file2.modifiedTime -> -ascInt

                // 类型相同长度相同，字母序排列
                else -> {
                    file1.name.compareTo(file2.name) * ascInt
                }
            }
        })
    }

    /**
     * 根据manifest文件路径解析镜像名称和版本
     * eg: /jq/manifest/1.0.0/manifest.json
     */
    fun getImageNameAndVersion(manifestPath: String): Pair<String, String> {
        val pattern = Pattern.compile("/(.*)/manifest/(.*)/manifest.json")
        val matcher = pattern.matcher(manifestPath)
        if (!matcher.find()) {
            throw IllegalArgumentException("illegal manifestPath: $manifestPath")
        }
        return Pair(matcher.group(1), matcher.group(2))
    }
}
