package com.tencent.devops.artifactory.util

import java.nio.file.Paths
import javax.ws.rs.BadRequestException

object PathUtils {
    fun checkAndNormalizeAbsPath(path: String): String {
        val normalizePath = Paths.get(path).normalize().toString()
        if (!normalizePath.startsWith("/")) {
            throw BadRequestException("非法路径")
        }
        return normalizePath
    }
}