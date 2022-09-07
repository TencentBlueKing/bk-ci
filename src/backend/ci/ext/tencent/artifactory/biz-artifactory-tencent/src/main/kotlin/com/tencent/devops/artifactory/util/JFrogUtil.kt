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
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import java.nio.file.Paths

object JFrogUtil {
    private val regex = Regex("/([0-9]|[a-z]|[A-Z]|-|_|/|\\.)*")

    fun getRepoPath() = "generic-local/"

    fun getPipelinePath(projectId: String, relativePath: String): String {
        return "generic-local/bk-archive/$projectId/${removePrefix(relativePath)}"
    }

    fun getPipelinePathPrefix(projectId: String): String {
        return "generic-local/bk-archive/$projectId/"
    }

    fun getCustomDirPath(projectId: String, relativePath: String): String {
        return "generic-local/bk-custom/$projectId/${removePrefix(relativePath)}"
    }

    fun getReportPath(projectId: String, pipelineId: String, buildId: String, elementId: String, path: String): String {
        return "generic-local/bk-report/$projectId/$pipelineId/$buildId/$elementId/${removePrefix(path)}"
    }

    fun getCustomDirPathPrefix(projectId: String): String {
        return "generic-local/bk-custom/$projectId/"
    }

    fun getPipelineToCustomPath(projectId: String, pipelineName: String, buildNo: String): String {
        return "generic-local/bk-custom/$projectId/_from_pipeline/$pipelineName/$buildNo"
    }

    fun getPipelineBuildPath(projectId: String, pipelineId: String, buildId: String): String {
        return "generic-local/bk-archive/$projectId/$pipelineId/$buildId"
    }

    private fun removePrefix(relativePath: String): String {
        return relativePath.removePrefix("/")
    }

    fun normalize(relativePath: String): String {
        return Paths.get(relativePath).normalize().toString()
    }

    fun getDockerRealPath(projectId: String, path: String): String {
        return "docker-local/${removePrefix(path)}/manifest.json"
    }

    fun getRealPath(projectId: String, artifactoryType: ArtifactoryType, path: String): String {
        return when (artifactoryType) {
            ArtifactoryType.PIPELINE -> {
                getPipelinePath(projectId, path)
            }
            ArtifactoryType.CUSTOM_DIR -> {
                getCustomDirPath(projectId, path)
            }
            // TODO #6302
            else -> throw UnsupportedOperationException()
        }
    }

    fun isValid(relativePath: String): Boolean {
        if (!relativePath.startsWith("/")) {
            return false
        }
        return true
    }

    fun isRoot(relativePath: String): Boolean {
        return relativePath == "/"
    }

    fun compose(dir: String, name: String): String {
        return "${dir.removeSuffix("/")}/${name.removePrefix("/")}"
    }

    fun compose(dir: String, name: String, folder: Boolean): String {
        val tmp = compose(dir, name)
        return if (folder) {
            "${tmp.removeSuffix("/")}/"
        } else {
            tmp.removeSuffix("/")
        }
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

    fun isCompressed(path: String): Boolean {
        return path.endsWith(".tar") ||
            path.endsWith(".gz") ||
            path.endsWith(".tgz") ||
            path.endsWith(".jar") ||
            path.endsWith(".zip")
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
}
