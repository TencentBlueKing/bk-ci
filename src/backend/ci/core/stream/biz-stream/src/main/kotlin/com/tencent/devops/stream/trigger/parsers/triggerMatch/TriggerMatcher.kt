package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushActionType
import com.tencent.devops.process.yaml.v2.enums.StreamMrEventAction
import com.tencent.devops.process.yaml.v2.enums.StreamPushActionType
import com.tencent.devops.process.yaml.v2.models.PreRepositoryHook
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.actions.data.isStreamMr
import com.tencent.devops.stream.trigger.exception.CommitCheck
import com.tencent.devops.stream.trigger.exception.StreamTriggerBaseException
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.BranchMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.UserMatchUtils
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexCondition")
class TriggerMatcher @Autowired constructor(
    private val repoTriggerEventService: RepoTriggerEventService
) {
    @Throws(StreamTriggerBaseException::class)
    fun isMatch(
        action: BaseAction
    ): TriggerResult {
        val newYaml = try {
            // 触发器需要将 on: 转为 TriggerOn:
            val realYaml = ScriptYmlUtils.formatYaml(action.data.context.originYaml!!)
            YamlUtil.getObjectMapper().readValue(realYaml, object : TypeReference<NoReplaceTemplate>() {})
        } catch (e: Throwable) {
            when (e) {
                is JsonProcessingException, is TypeCastException -> {
                    throw StreamTriggerException(
                        action,
                        TriggerReason.CI_YAML_INVALID,
                        reasonParams = listOf(e.message ?: ""),
                        commitCheck = CommitCheck(
                            block = action.metaData.isStreamMr(),
                            state = StreamCommitCheckState.FAILURE
                        )
                    )
                }
                else -> {
                    throw e
                }
            }
        }

        return if (action.data.context.repoTrigger != null) {
            val repoTriggerPipelineList = action.data.context.repoTrigger!!.repoTriggerPipelineList
            val repoTriggerOn = ScriptYmlUtils.formatRepoHookTriggerOn(
                newYaml.triggerOn,
                repoTriggerPipelineList.find {
                    it.pipelineId == action.data.context.pipeline!!.pipelineId
                }?.sourceGitProjectPath
            )
            if (repoTriggerOn == null) {
                repoTriggerEventService.deleteRepoTriggerEvent(action.data.context.pipeline!!.pipelineId)
                return TriggerResult(
                    trigger = TriggerBody(false, "repo trigger delete yet"),
                    timeTrigger = false,
                    startParams = emptyMap(),
                    deleteTrigger = false
                )
            }
            action.isMatch(repoTriggerOn)
        } else {
            action.isMatch(ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn)).copy(
                repoHookName = checkRepoHook(
                    action = action,
                    preTriggerOn = newYaml.triggerOn
                )
            )
        }
    }

    private fun checkRepoHook(
        action: BaseAction,
        preTriggerOn: PreTriggerOn?
    ): List<String> {
        logger.info("checkRepoHook|preTriggerOn=$preTriggerOn")
        if (preTriggerOn?.repoHook == null) {
            return emptyList()
        }
        val repositoryHookList = try {
            YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(preTriggerOn.repoHook!!),
                object : TypeReference<List<PreRepositoryHook>>() {}
            )
        } catch (e: MismatchedInputException) {
            logger.error("Format triggerOn repoHook failed.", e)
            return emptyList()
        }
        val repoHookList = mutableListOf<String>()
        repositoryHookList.forEach { repoHook ->
            repoHook.name?.let { name ->
                action.registerCheckRepoTriggerCredentials(
                    repoHook = ScriptYmlUtils.repoHookRule(repoHook)
                )
                // 表示路径至少为2级，不支持只填一级路径进行模糊匹配
                if (name.contains("/") && !name.startsWith("/")) {
                    repoHookList.add(name)
                }
            }
        }
        return repoHookList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerMatcher::class.java)

        fun isPushMatch(
            triggerOn: TriggerOn,
            eventBranch: String,
            changeSet: Set<String>?,
            userId: String,
            checkCreateAndUpdate: Boolean?
        ): TriggerBody {
            // 如果没有配置push，默认未匹配
            if (triggerOn.push == null) {
                return TriggerBody().triggerFail("on.push", "does not currently exist")
            }

            val pushRule = triggerOn.push!!
            // 1、check branchIgnore，满足屏蔽条件直接返回不匹配
            if (BranchMatchUtils.isIgnoreBranchMatch(pushRule.branchesIgnore, eventBranch)
            ) {
                return TriggerBody().triggerFail("on.push.branches-ignore", "current branch($eventBranch) ignore match")
            }

            // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
            if (PathMatchUtils.isIgnorePathMatch(pushRule.pathsIgnore, changeSet)) {
                val path = changeSet?.find { it in (pushRule.pathsIgnore ?: emptyList()) }
                return TriggerBody().triggerFail("on.push.paths-ignore", "current change path($path) ignore match")
            }

            // 3、check userIgnore,满足屏蔽条件直接返回不匹配
            if (UserMatchUtils.isIgnoreUserMatch(pushRule.usersIgnore, userId)) {
                return TriggerBody().triggerFail("on.push.users-ignore", "current trigger user($userId) ignore match")
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(pushRule.branches, eventBranch)) {
                return TriggerBody().triggerFail("on.push.branches", "current branch($eventBranch) not match")
            }
            // include
            if (!PathMatchUtils.isIncludePathMatch(pushRule.paths, changeSet)) {
                return TriggerBody().triggerFail("on.push.paths", "current change path($changeSet) not match")
            }
            // include
            if (!UserMatchUtils.isUserMatch(pushRule.users, userId)) {
                return TriggerBody().triggerFail("on.push.users", "current trigger user($userId) not match")
            }
            // action
            if (!checkActionMatch(pushRule.action, checkCreateAndUpdate)) {
                val action = when (checkCreateAndUpdate) {
                    null -> TGitPushActionType.PUSH_FILE.value
                    false -> TGitPushActionType.NEW_BRANCH.value
                    true -> TGitPushActionType.NEW_BRANCH_AND_PUSH_FILE.value
                }
                return TriggerBody().triggerFail("on.push.action", "current trigger action($action) not match")
            }
            logger.info("Git trigger branch($eventBranch) is included and path(${pushRule.paths}) is included")
            return TriggerBody(true)
        }

        fun isMrMatch(
            triggerOn: TriggerOn,
            sourceBranch: String,
            targetBranch: String,
            changeSet: Set<String>?,
            userId: String,
            mrAction: Any
        ): TriggerBody {
            if (triggerOn.mr == null) {
                return TriggerBody().triggerFail("on.mr", "does not currently exist")
            }

            val mrRule = triggerOn.mr!!
            // 1、check sourceBranchIgnore，满足屏蔽条件直接返回不匹配
            if (BranchMatchUtils.isIgnoreBranchMatch(mrRule.sourceBranchesIgnore, sourceBranch)) {
                return TriggerBody().triggerFail(
                    "on.mr.source-branches-ignore",
                    "current source branch($sourceBranch) ignore match"
                )
            }

            // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
            if (PathMatchUtils.isIgnorePathMatch(mrRule.pathsIgnore, changeSet)) {
                val path = changeSet?.find { it in (mrRule.pathsIgnore ?: emptyList()) }
                return TriggerBody().triggerFail("on.mr.paths-ignore", "current change path($path) ignore match")
            }

            // 3、check userIgnore,满足屏蔽条件直接返回不匹配
            if (UserMatchUtils.isIgnoreUserMatch(mrRule.usersIgnore, userId)) {
                return TriggerBody().triggerFail("on.mr.users-ignore", "current trigger user($userId) ignore match")
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(mrRule.targetBranches, targetBranch)) {
                return TriggerBody().triggerFail(
                    "on.mr.target-branches",
                    "current target branch($targetBranch) not match"
                )
            }
            // include
            if (!PathMatchUtils.isIncludePathMatch(mrRule.paths, changeSet)) {
                return TriggerBody().triggerFail("on.mr.paths", "current change path($changeSet) not match")
            }
            // include
            if (!UserMatchUtils.isUserMatch(mrRule.users, userId)) {
                return TriggerBody().triggerFail("on.mr.users", "current trigger user($userId) not match")
            }
            // include
            if (!isMrActionMatch(mrRule.action, mrAction)) {
                return TriggerBody().triggerFail("on.mr.action", "current action($mrAction) not match")
            }
            return TriggerBody(true)
        }

        fun isTagPushMatch(
            triggerOn: TriggerOn,
            eventTag: String,
            userId: String,
            fromBranch: String?
        ): TriggerBody {
            if (triggerOn.tag == null) {
                return TriggerBody().triggerFail("on.tag", "does not currently exist")
            }

            val tagRule = triggerOn.tag!!
            // ignore
            if (BranchMatchUtils.isIgnoreBranchMatch(tagRule.tagsIgnore, eventTag)) {
                return TriggerBody().triggerFail("on.tag.tags-ignore", "current tag($eventTag) ignore match")
            }

            if (UserMatchUtils.isIgnoreUserMatch(tagRule.usersIgnore, userId)) {
                return TriggerBody().triggerFail("on.tag.users-ignore", "current trigger user($userId) ignore match")
            }

            if (fromBranch != null && !BranchMatchUtils.isBranchMatch(tagRule.fromBranches, fromBranch)) {
                return TriggerBody().triggerFail(
                    "on.tag.from-branches",
                    "current tag from branch($fromBranch) not match"
                )
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(tagRule.tags, eventTag)) {
                return TriggerBody().triggerFail("on.tag.tags", "current tag($eventTag) not match")
            }
            if (!UserMatchUtils.isUserMatch(tagRule.users, userId)) {
                return TriggerBody().triggerFail("on.tag.users", "current trigger user($userId) not match")
            }
            logger.info(
                "Git trigger tags($eventTag) is included path(${tagRule.tags}) is included,and fromBranch($fromBranch)"
            )
            return TriggerBody(true)
        }

        private fun checkActionMatch(actionList: List<String>?, checkCreateAndUpdate: Boolean?): Boolean {
            if (actionList.isNullOrEmpty()) {
                return true
            }
            actionList.forEach {
                if (it == StreamPushActionType.NEW_BRANCH.value && checkCreateAndUpdate != null) {
                    return true
                }
                if (it == StreamPushActionType.PUSH_FILE.value && checkCreateAndUpdate != false) {
                    return true
                }
            }

            return false
        }

        private fun isMrActionMatch(actionList: List<String>?, mrAction: Any): Boolean {
            val realActionList = if (actionList.isNullOrEmpty()) {
                // 缺省时使用默认值
                listOf(
                    StreamMrEventAction.OPEN.value,
                    StreamMrEventAction.REOPEN.value,
                    StreamMrEventAction.PUSH_UPDATE.value
                )
            } else {
                actionList
            }
            realActionList.forEach {
                if (it == mrAction) {
                    return true
                }
            }

            return false
        }
    }
}
