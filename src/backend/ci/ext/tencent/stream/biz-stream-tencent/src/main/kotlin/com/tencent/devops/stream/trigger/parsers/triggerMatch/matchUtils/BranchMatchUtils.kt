package com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils

import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

object BranchMatchUtils {

    private val logger = LoggerFactory.getLogger(BranchMatchUtils::class.java)
    private val matcher = AntPathMatcher()

    fun isIgnoreBranchMatch(branchList: List<String>?, eventBranch: String): Boolean {
        if (branchList.isNullOrEmpty()) {
            return false
        }

        branchList.forEach {
            if (branchMatch(it, eventBranch)) {
                logger.info("The branchesIgnore($it) exclude the git branch ($eventBranch)")
                return true
            }
        }
        return false
    }

    fun isBranchMatch(branchList: List<String>?, eventBranch: String): Boolean {
        if (branchList.isNullOrEmpty()) {
            return true
        }
        logger.info("Include branch set($branchList)")

        if (branchList.size == 1 && branchList[0] == "*") {
            return true
        }

        branchList.forEach {
            if (branchMatch(it, eventBranch)) {
                logger.info("The include branch($it) include the git update one($eventBranch)")
                return true
            }
        }
        return false
    }

    private fun branchMatch(branchName: String, ref: String): Boolean {
        return matcher.match(branchName.replace("*", "**"), ref)
    }
}
