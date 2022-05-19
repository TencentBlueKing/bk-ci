package com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils

import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object PathMatchUtils {

    private val logger = LoggerFactory.getLogger(PathMatchUtils::class.java)

    fun isIgnorePathMatch(pathIgnoreList: List<String>?, fileChangeSet: Set<String>?): Boolean {
        if (pathIgnoreList.isNullOrEmpty()) {
            return false
        }

        logger.info("Exclude path set ($pathIgnoreList)")

        fileChangeSet?.forEach eventPath@{ path ->
            pathIgnoreList.forEach userPath@{ excludePath ->
                if (isPathMatch(path, excludePath)) {
                    return@eventPath
                }
            }
            logger.info("excluded event path($fileChangeSet) not match the user path($pathIgnoreList)")
            return false
        }
        return true
    }

    fun isIncludePathMatch(pathList: List<String>?, fileChangeSet: Set<String>?): Boolean {
        if (pathList.isNullOrEmpty()) {
            logger.info("trigger path include is empty.")
            return true
        }

        logger.info("Include path set($pathList)")
        fileChangeSet?.forEach { path ->
            pathList.forEach { includePath ->
                if (isPathMatch(path, includePath)) {
                    logger.info("The include path($includePath) include the git update one($path)")
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
        logger.info("fullPath: $fullPath, prefixPath: $prefixPath")
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
