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

package com.tencent.devops.common.archive.util

import okhttp3.Credentials
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

    fun getCustomDirPathPrefix(projectId: String): String {
        return "generic-local/bk-custom/$projectId/"
    }

    fun getReportPath(projectId: String, pipelineId: String, buildId: String, elementId: String, path: String): String {
        return "generic-local/bk-report/$projectId/$pipelineId/$buildId/$elementId/${removePrefix(path)}"
    }

    fun getPluginTaskPath(relativePath: String): String {
        return "generic-local/bk-plugin/task/${removePrefix(relativePath)}"
    }

    fun getAtomFrontendFileBasePath(): String {
        return "generic-local/bk-plugin-fe/"
    }

    private fun removePrefix(relativePath: String): String {
        return relativePath.removePrefix("/")
    }

    fun normalize(relativePath: String): String {
        return Paths.get(relativePath).normalize().toString()
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

    fun makeCredential(username: String, password: String): String = Credentials.basic(username, password)
}
