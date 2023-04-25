package com.tencent.devops.common.webhook.service.code.filter

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * stream 事件触发路径过滤
 *
 * stream的路径是可以通过*表示所有路径,匹配方式与ci不同，做下兼容
 */
class PathStreamFilter(
    private val pipelineId: String,
    private val triggerOnPath: List<String>,
    private val includedPaths: List<String>,
    private val excludedPaths: List<String>
) : BasePathFilter(
    pipelineId = pipelineId,
    triggerOnPath = triggerOnPath,
    includedPaths = includedPaths,
    excludedPaths = excludedPaths
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PathStreamFilter::class.java)
    }

    override fun isPathMatch(eventPath: String, userPath: String): Boolean {
        if (userPath.endsWith("*")) {
            logger.info(
                "PathStreamFilter|path_end_with_*|" +
                    "$eventPath|$userPath"
            )
        }
        val fullPathList = eventPath.removePrefix("/").split("/")
        val prefixPathList = userPath.removePrefix("/").split("/")
        if (fullPathList.size < prefixPathList.size) {
            return false
        }

        for (i in prefixPathList.indices) {
            val pattern = Pattern.compile(prefixPathList[i].replace("*", "\\S*"))
            val matcher = pattern.matcher(fullPathList[i])
            if (prefixPathList[i] != "*" && !matcher.matches()) {
                return false
            }
        }

        return true
    }
}
