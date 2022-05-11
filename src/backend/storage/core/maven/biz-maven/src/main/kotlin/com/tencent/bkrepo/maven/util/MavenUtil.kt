package com.tencent.bkrepo.maven.util

import com.google.common.io.ByteStreams
import com.google.common.io.CharStreams
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import org.apache.commons.lang.StringUtils
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object MavenUtil {
    private const val MAX_DIGEST_CHARS_NEEDED = 128

    /**
     * 从流中导出摘要
     * */
    fun extractDigest(inputStream: InputStream): String {
        inputStream.use {
            val reader = InputStreamReader(
                ByteStreams
                    .limit(inputStream, MAX_DIGEST_CHARS_NEEDED.toLong()),
                StandardCharsets.UTF_8
            )
            return CharStreams.toString(reader)
        }
    }


    /**
     * 提取出对应的artifactId和groupId
     */
    fun extractGrounpIdAndArtifactId(packageKey: String): Pair<String, String> {
        val params = PackageKeys.resolveGav(packageKey)
        val artifactId = params.split(":").last()
        val groupId = params.split(":").first()
        return Pair(artifactId, groupId)
    }

    /**
     * 获取对应package存储的节点路径
     */
    fun extractPath(packageKey: String): String {
        val (artifactId, groupId) = extractGrounpIdAndArtifactId(packageKey)
        return StringUtils.join(groupId.split("."), "/") + "/$artifactId"
    }
}
