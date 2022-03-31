package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.devops.process.yaml.v2.enums.StreamMrEventAction
import com.devops.process.yaml.v2.enums.StreamPushActionType
import com.devops.process.yaml.v2.models.PreRepositoryHook
import com.devops.process.yaml.v2.models.on.PreTriggerOn
import com.devops.process.yaml.v2.models.on.TriggerOn
import com.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate
import com.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
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
                    trigger = false,
                    timeTrigger = false,
                    startParams = emptyMap(),
                    deleteTrigger = false
                )
            }
            action.isMatch(repoTriggerOn)
        } else {
            action.isMatch(ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn)).copy(
                repoHookName = checkRepoHook(
                    preTriggerOn = newYaml.triggerOn
                )
            )
        }
    }

    private fun checkRepoHook(
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
        repositoryHookList.forEach {
            it.name?.let { name ->
                // TODO 可以添加其他条件，做预判断
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
            isCreateBranch: Boolean
        ): Boolean {
            // 如果没有配置push，默认未匹配
            if (triggerOn.push == null) {
                return false
            }

            val pushRule = triggerOn.push!!
            // 1、check branchIgnore，满足屏蔽条件直接返回不匹配
            if (BranchMatchUtils.isIgnoreBranchMatch(pushRule.branchesIgnore, eventBranch)) {
                return false
            }

            // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
            if (PathMatchUtils.isIgnorePathMatch(pushRule.pathsIgnore, changeSet)) {
                return false
            }

            // 3、check userIgnore,满足屏蔽条件直接返回不匹配
            if (UserMatchUtils.isIgnoreUserMatch(pushRule.usersIgnore, userId)) {
                return false
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(pushRule.branches, eventBranch) ||
                !PathMatchUtils.isIncludePathMatch(pushRule.paths, changeSet) ||
                !UserMatchUtils.isUserMatch(pushRule.users, userId)
            ) {
                return false
            }
            // action
            if (!checkActionMatch(pushRule.action, isCreateBranch)) {
                return false
            }
            logger.info("Git trigger branch($eventBranch) is included and path(${pushRule.paths}) is included")
            return true
        }

        fun isMrMatch(
            triggerOn: TriggerOn,
            sourceBranch: String,
            targetBranch: String,
            changeSet: Set<String>?,
            userId: String,
            mrAction: Any
        ): Boolean {
            if (triggerOn.mr == null) {
                return false
            }

            val mrRule = triggerOn.mr!!
            // 1、check sourceBranchIgnore，满足屏蔽条件直接返回不匹配
            if (BranchMatchUtils.isIgnoreBranchMatch(mrRule.sourceBranchesIgnore, sourceBranch)) {
                return false
            }

            // 2、check pathIgnore，满足屏蔽条件直接返回不匹配
            if (PathMatchUtils.isIgnorePathMatch(mrRule.pathsIgnore, changeSet)) {
                return false
            }

            // 3、check userIgnore,满足屏蔽条件直接返回不匹配
            if (UserMatchUtils.isIgnoreUserMatch(mrRule.usersIgnore, userId)) {
                return false
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(mrRule.targetBranches, targetBranch) ||
                !PathMatchUtils.isIncludePathMatch(mrRule.paths, changeSet) ||
                !UserMatchUtils.isUserMatch(mrRule.users, userId) ||
                !isMrActionMatch(mrRule.action, mrAction)
            ) {
                return false
            }
            return true
        }

        fun isTagPushMatch(
            triggerOn: TriggerOn,
            eventTag: String,
            userId: String,
            fromBranch: String?
        ): Boolean {
            if (triggerOn.tag == null) {
                return false
            }

            val tagRule = triggerOn.tag!!
            // ignore
            if (BranchMatchUtils.isIgnoreBranchMatch(tagRule.tagsIgnore, eventTag)) {
                return false
            }

            if (UserMatchUtils.isIgnoreUserMatch(tagRule.usersIgnore, userId)) {
                return false
            }

            if (fromBranch != null && !BranchMatchUtils.isBranchMatch(tagRule.fromBranches, fromBranch)) {
                return false
            }

            // include
            if (!BranchMatchUtils.isBranchMatch(tagRule.tags, eventTag) ||
                !UserMatchUtils.isUserMatch(tagRule.users, userId)
            ) {
                return false
            }
            logger.info(
                "Git trigger tags($eventTag) is included and path(${tagRule.tags}) is included,and fromBranch($fromBranch)"
            )
            return true
        }

        private fun checkActionMatch(actionList: List<String>?, isCreateBranch: Boolean): Boolean {
            if (actionList.isNullOrEmpty()) {
                return true
            }
            actionList.forEach {
                if (it == StreamPushActionType.NEW_BRANCH.value && isCreateBranch) {
                    return true
                }
                if (it == StreamPushActionType.PUSH_FILE.value && !isCreateBranch) {
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
