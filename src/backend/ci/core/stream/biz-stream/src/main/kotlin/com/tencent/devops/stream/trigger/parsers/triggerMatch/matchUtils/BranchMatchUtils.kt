package com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils

import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBody
import org.slf4j.LoggerFactory
import org.springframework.util.AntPathMatcher

object BranchMatchUtils {

    private val logger = LoggerFactory.getLogger(BranchMatchUtils::class.java)
    private val matcher = AntPathMatcher()
    lateinit var trigger: TriggerBody
    lateinit var path: String

    fun set(triggerBody: TriggerBody, pathBody: String): BranchMatchUtils {
        trigger = triggerBody
        path = pathBody
        return this
    }

    private fun triggerFail(message: String) {
        if (this::trigger.isInitialized) {
            trigger.triggerFail(path, message)
        }
    }

    fun isIgnoreBranchMatch(branchList: List<String>?, eventBranch: String): Boolean {
        if (branchList.isNullOrEmpty()) {
            return false
        }

        branchList.forEach {
            if (branchMatch(it, eventBranch)) {
                logger.info("The branchesIgnore($it) exclude the git branch ($eventBranch)")
                triggerFail("current branch ($eventBranch) match")
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
        triggerFail("current branch ($eventBranch) not match")
        return false
    }

    private fun branchMatch(branchName: String, ref: String): Boolean {
        return matcher.match(branchName.replace("*", "**"), ref)
    }
}
