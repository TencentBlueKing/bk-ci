package com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils

import com.tencent.devops.common.webhook.pojo.code.MATCH_PATHS
import com.tencent.devops.common.webhook.service.code.filter.PathStreamFilter
import com.tencent.devops.common.webhook.service.code.filter.WebhookFilterResponse
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object PathMatchUtils {

    private val logger = LoggerFactory.getLogger(PathMatchUtils::class.java)

    fun isPathMatch(
        fileChangeSet: Set<String>,
        pathList: List<String>,
        pathIgnoreList: List<String>
    ): TriggerBody {
        val response = WebhookFilterResponse()
        if (!PathStreamFilter(
                pipelineId = "",
                triggerOnPath = fileChangeSet.toList(),
                includedPaths = pathList,
                excludedPaths = pathIgnoreList
            ).doFilter(response)
        ) {
            // 包含匹配失败
            val includePathsMatch = pathIgnoreList.isEmpty() &&
                pathList.isNotEmpty() &&
                response.getParam()[MATCH_PATHS] == null
            return if (includePathsMatch) {
                TriggerBody().triggerFail("on.push.paths", "change path($pathList) not match")
            } else {
                TriggerBody().triggerFail("on.push.paths-ignore", "change path($pathIgnoreList) match")
            }
        }
        return TriggerBody(true)
    }

    fun isIgnorePathMatch(pathIgnoreList: List<String>?, fileChangeSet: Set<String>?): Boolean {
        if (pathIgnoreList.isNullOrEmpty()) {
            return false
        }

        logger.info("PathMatchUtils|isIgnorePathMatch|Exclude path set ($pathIgnoreList)")

        fileChangeSet?.forEach eventPath@{ path ->
            pathIgnoreList.forEach userPath@{ excludePath ->
                if (isPathMatch(path, excludePath)) {
                    return@eventPath
                }
            }
            logger.info(
                "PathMatchUtils|isIgnorePathMatch" +
                    "|excluded event path($fileChangeSet) not match the user path($pathIgnoreList)"
            )
            return false
        }
        return true
    }

    fun isIncludePathMatch(pathList: List<String>?, fileChangeSet: Set<String>?): Boolean {
        if (pathList.isNullOrEmpty()) {
            logger.info("PathMatchUtils|isIncludePathMatch|trigger path include is empty")
            return true
        }

        logger.info("PathMatchUtils|isIncludePathMatch|Include path set($pathList)")
        fileChangeSet?.forEach { path ->
            pathList.forEach { includePath ->
                if (isPathMatch(path, includePath)) {
                    logger.info(
                        "PathMatchUtils|isIncludePathMatch" +
                            "|The include path($includePath) include the git update one($path)"
                    )
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if the path match
     * example:
     * fullPath: a/1.txt
     * prefixPath: a/
     */
    private fun isPathMatch(fullPath: String, prefixPath: String): Boolean {
        logger.info("PathMatchUtils|isPathMatch|fullPath|$fullPath|prefixPath|$prefixPath")
        if (prefixPath.endsWith("*")) {
            logger.info(
                "PathStreamFilter|path_end_with_*|" +
                    "$fullPath|$prefixPath"
            )
        }
        val fullPathList = fullPath.removePrefix("/").split("/")
        val prefixPathList = prefixPath.removePrefix("/").split("/")
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
