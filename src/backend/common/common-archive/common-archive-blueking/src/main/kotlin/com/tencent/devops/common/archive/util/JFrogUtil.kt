package com.tencent.devops.common.archive.util

import okhttp3.Credentials
import java.nio.file.Paths

/**
 * Created by Aaron Sheng on 2017/12/28.
 */
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
