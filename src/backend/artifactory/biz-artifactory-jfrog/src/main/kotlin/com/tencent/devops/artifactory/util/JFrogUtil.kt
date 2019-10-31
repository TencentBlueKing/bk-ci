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

    private fun removePrefix(relativePath: String): String {
        return relativePath.removePrefix("/")
    }

    fun normalize(relativePath: String): String {
        return Paths.get(relativePath).normalize().toString()
    }

    fun getDockerRealPath(projectId: String, path: String): String {
        //   docker-local/devcloud/ijobs/tlinux-1.2-base/latest/manifest.json
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
    fun sort(bkFileInfoList: List<FileInfo>, asc: Boolean = true): List<FileInfo> {
        val ascInt = if (asc) 1 else -1
        return bkFileInfoList.sortedWith(Comparator {
            file1, file2 -> when {
                // 文件夹排在文件之上
                file1.folder && !file2.folder -> -1
                !file1.folder && file2.folder -> 1

                // 文件名短在文件名长之上
                file1.name.length < file2.name.length -> -ascInt
                file1.name.length > file2.name.length -> ascInt

                // 类型相同长度相同，字母序排列
                else -> {
                    file1.name.compareTo(file2.name) * ascInt
                }
            }
        })
    }
}