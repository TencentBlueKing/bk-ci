package com.tencent.devops.artifactory.util

import com.tencent.devops.common.api.util.UUIDUtil
import java.io.File
import java.nio.file.Files

object DefaultPathUtils {
    private const val DEFAULT_EXTENSION = "temp"

    fun isFolder(path: String): Boolean {
        return path.endsWith("/")
    }

    fun getParentFolder(path: String): String {
        val tmpPath = path.removeSuffix("/")
        return tmpPath.removeSuffix(getFileName(tmpPath))
    }

    fun getFileName(path: String): String {
        return path.removeSuffix("/").split("/").last()
    }

    fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf(".")
        return if (lastDotIndex == -1) {
            ""
        } else {
            fileName.substring(lastDotIndex + 1)
        }
    }

    fun randomFile(fileExtension: String = DEFAULT_EXTENSION): File {
        val suffix = if (fileExtension.isNullOrBlank()) "" else ".$fileExtension"
        return Files.createTempFile(UUIDUtil.generate(), suffix).toFile()
    }

    fun randomFileName(fileExtension: String = DEFAULT_EXTENSION): String {
        val suffix = if (fileExtension.isNullOrBlank()) "" else ".$fileExtension"
        return "${UUIDUtil.generate()}$suffix"
    }

    fun resolvePipelineId(path: String): String {
        val roads = path.removePrefix("/").split("/")
        if (roads.size < 2) throw RuntimeException("Path $path doesn't contain pipelineId")
        return roads[0]
    }

    fun resolveBuildId(path: String): String {
        val roads = path.removePrefix("/").split("/")
        if (roads.size < 3) throw RuntimeException("Path $path doesn't contain buildId")
        return roads[1]
    }
}