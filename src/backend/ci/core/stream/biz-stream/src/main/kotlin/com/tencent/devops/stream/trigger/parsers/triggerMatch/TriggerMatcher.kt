package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.yaml.v2.enums.StreamMrEventAction
import com.tencent.devops.process.yaml.v2.enums.StreamPushActionType
import com.tencent.devops.process.yaml.v2.models.PreRepositoryHook
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.parsers.template.models.NoReplaceTemplate
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.stream.dao.StreamPipelineTriggerDao
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
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexCondition")
class TriggerMatcher @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val repoTriggerEventService: RepoTriggerEventService,
    private val streamPipelineTriggerDao: StreamPipelineTriggerDao
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

        val result = if (action.data.context.repoTrigger != null) {
            val repoTriggerPipelineList = action.data.context.repoTrigger!!.repoTriggerPipelineList
            val repoTriggerOn = ScriptYmlUtils.formatRepoHookTriggerOn(
                preTriggerOn = newYaml.triggerOn,
                name = repoTriggerPipelineList.find {
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
                    action = action,
                    repoHooks = newYaml.triggerOn?.repoHook
                )
            )
        }

        // 缓存触发器
        if (confirmProjectUseTriggerCache(action.getProjectCode()) &&
            action.data.context.triggerCache != null &&
            !action.data.context.pipeline?.pipelineId.isNullOrBlank()
        ) {
            streamPipelineTriggerDao.saveOrUpdate(
                dslContext = dslContext,
                projectId = action.getProjectCode(),
                pipelineId = action.data.context.pipeline!!.pipelineId,
                branch = action.data.context.triggerCache!!.pipelineFileBranch,
                ciFileBlobId = action.data.context.triggerCache!!.blobId,
                trigger = if (newYaml.triggerOn == null) {
                    ""
                } else {
                    JsonUtil.getObjectMapper().writeValueAsString(newYaml.triggerOn)
                }
            )
        }

        return result
    }

    fun isMatch(
        action: BaseAction,
        triggerStr: String
    ): Pair<List<Any>?, TriggerResult> {
        val trigger = if (triggerStr.isBlank()) {
            null
        } else {
            try {
                JsonUtil.getObjectMapper().readValue(triggerStr, object : TypeReference<PreTriggerOn>() {})
            } catch (e: Throwable) {
                when (e) {
                    is JsonProcessingException, is TypeCastException -> {
                        throw StreamTriggerException(
                            action,
                            TriggerReason.CI_TRIGGER_FORMAT_ERROR,
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
        }

        // 跨库触发这里跨库触发信息放回空，防止重复注册跨库触发信息
        return if (action.data.context.repoTrigger != null) {
            val repoTriggerPipelineList = action.data.context.repoTrigger!!.repoTriggerPipelineList
            val repoTriggerOn = ScriptYmlUtils.formatRepoHookTriggerOn(
                preTriggerOn = trigger,
                name = repoTriggerPipelineList.find {
                    it.pipelineId == action.data.context.pipeline!!.pipelineId
                }?.sourceGitProjectPath
            )
            if (repoTriggerOn == null) {
                repoTriggerEventService.deleteRepoTriggerEvent(action.data.context.pipeline!!.pipelineId)
                return Pair(
                    null,
                    TriggerResult(
                        trigger = false,
                        timeTrigger = false,
                        startParams = emptyMap(),
                        deleteTrigger = false
                    )
                )
            }
            Pair(null, action.isMatch(repoTriggerOn))
        } else {
            Pair(trigger?.repoHook, action.isMatch(ScriptYmlUtils.formatTriggerOn(trigger)))
        }
    }

    fun checkRepoHook(
        action: BaseAction,
        repoHooks: List<Any>? = null
    ): List<String> {
        logger.info("checkRepoHook|repoHook=$repoHooks")
        if (repoHooks == null) {
            return emptyList()
        }
        val repositoryHookList = try {
            YamlUtil.getObjectMapper().readValue(
                JsonUtil.toJson(repoHooks),
                object : TypeReference<List<PreRepositoryHook>>() {}
            )
        } catch (e: MismatchedInputException) {
            logger.warn("TriggerMatcher|checkRepoHook|failed", e)
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

    fun confirmProjectUseTriggerCache(projectId: String): Boolean {
        return redisOperation.isMember(STREAM_TRIGGER_CACHE_PROJECTS_KEY, projectId)
    }

    companion object {
        const val STREAM_TRIGGER_CACHE_PROJECTS_KEY = "stream:trigger.cache:project:list"

        private val logger = LoggerFactory.getLogger(TriggerMatcher::class.java)

        fun isPushMatch(
            triggerOn: TriggerOn,
            eventBranch: String,
            changeSet: Set<String>?,
            userId: String,
            checkCreateAndUpdate: Boolean?
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
            if (!checkActionMatch(pushRule.action, checkCreateAndUpdate)) {
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
                "Git trigger tags($eventTag) is included path(${tagRule.tags}) is included,and fromBranch($fromBranch)"
            )
            return true
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
