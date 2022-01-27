package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.DeleteRule
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.ci.v2.check
import com.tencent.devops.common.ci.v2.getTypesObjectKind
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.isDeleteEvent
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.stream.pojo.isMr
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import com.tencent.devops.stream.trigger.pojo.StreamTriggerContext
import com.tencent.devops.stream.trigger.template.pojo.NoReplaceTemplate
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.v2.service.DeleteEventService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexCondition")
class TriggerMatcher @Autowired constructor(
    private val streamScmService: StreamScmService,
    private val streamTimerService: StreamTimerService,
    private val streamDeleteEventService: DeleteEventService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TriggerMatcher::class.java)
    }

    @Throws(TriggerBaseException::class)
    fun isMatch(
        context: StreamTriggerContext,
        gitProjectInfo: GitCIProjectInfo
    ): TriggerResult {
        val newYaml = try {
            // 触发器需要将 on: 转为 TriggerOn:
            val realYaml = ScriptYmlUtils.formatYaml(context.originYaml)
            YamlUtil.getObjectMapper().readValue(realYaml, object : TypeReference<NoReplaceTemplate>() {})
        } catch (e: Throwable) {
            when (e) {
                is JsonProcessingException, is TypeCastException -> {
                    TriggerException.triggerError(
                        request = context.requestEvent,
                        event = context.gitEvent,
                        pipeline = context.pipeline,
                        reason = TriggerReason.CI_YAML_INVALID,
                        reasonParams = listOf(e.message ?: ""),
                        yamls = Yamls(context.originYaml, null, null),
                        commitCheck = CommitCheck(
                            block = context.requestEvent.isMr(),
                            state = GitCICommitCheckState.FAILURE
                        )
                    )
                }
                else -> {
                    throw e
                }
            }
        }

        return if (context.gitEvent.isDeleteEvent()) {
            deleteEventMatch(
                context = context,
                triggerOn = ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn),
                objectKind = context.requestEvent.objectKind
            )
        } else {
            match(
                context = context,
                gitProjectInfo = gitProjectInfo,
                triggerOn = ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn),
                changeSet = getChangeSet(context),
                pipelineFilePath = context.pipeline.filePath
            )
        }
    }

    fun match(
        context: StreamTriggerContext,
        gitProjectInfo: GitCIProjectInfo,
        triggerOn: TriggerOn,
        changeSet: Set<String>?,
        pipelineFilePath: String
    ): TriggerResult {
        val (sourceBranch, targetBranch) = getBranch(context.gitEvent)

        val gitRequestEvent = context.requestEvent

        // 判断是否是默认分支上的push，来判断是否注册定时任务
        val isTime = if (gitRequestEvent.isDefaultBranchTrigger(gitProjectInfo.defaultBranch)) {
            isSchedulesMatch(triggerOn, targetBranch, gitRequestEvent, context.pipeline)
        } else {
            false
        }

        val isDelete = if (gitRequestEvent.isDefaultBranchTrigger(gitProjectInfo.defaultBranch)) {
            // 只有更改了delete相关流水线才做更新
            PathMatchUtils.isIncludePathMatch(listOf(pipelineFilePath), changeSet) &&
                isDeleteMatch(triggerOn.delete, context.requestEvent, context.pipeline)
        } else {
            false
        }

        val (isTrigger, startParams) = matchAndStartParams(
            context = context,
            triggerOn = triggerOn
        )
        return TriggerResult(isTrigger, isTime, startParams, isDelete)
    }

    fun deleteEventMatch(
        context: StreamTriggerContext,
        triggerOn: TriggerOn,
        objectKind: String
    ): TriggerResult {

        val deleteObjectKinds = triggerOn.delete?.getTypesObjectKind()?.map { it.value }?.toSet()
            ?: return TriggerResult(
                trigger = false,
                timeTrigger = false,
                startParams = emptyMap(),
                deleteTrigger = false
            )
        return if (objectKind in deleteObjectKinds) {
            val startParams = getStartParams(
                context = context,
                triggerOn = triggerOn
            )
            TriggerResult(trigger = true, timeTrigger = false, startParams = startParams, deleteTrigger = true)
        } else {
            TriggerResult(trigger = false, timeTrigger = false, startParams = emptyMap(), deleteTrigger = false)
        }
    }

    // 判断是否注册定时任务来看是修改还是删除
    private fun isSchedulesMatch(
        triggerOn: TriggerOn,
        eventBranch: String,
        gitRequestEvent: GitRequestEvent,
        pipeline: GitProjectPipeline
    ): Boolean {
        if (triggerOn.schedules == null) {
            // 新流水线没有定时任务就没注册过定时任务
            if (pipeline.pipelineId.isBlank()) {
                return false
            } else {
                // 不是新流水线的可能注册过了要删除
                streamTimerService.get(pipeline.pipelineId) ?: return false
                streamTimerService.deleteTimer(pipeline.pipelineId, gitRequestEvent.userId)
                return false
            }
        } else {
            if (triggerOn.schedules?.cron.isNullOrBlank()) {
                logger.info("The schedules cron is invalid($eventBranch)")
                return false
            }
        }
        return true
    }

    // 判断是否注册默认分支的删除任务
    private fun isDeleteMatch(
        deleteRule: DeleteRule?,
        gitRequestEvent: GitRequestEvent,
        pipeline: GitProjectPipeline
    ): Boolean {
        if (deleteRule == null) {
            if (pipeline.pipelineId.isBlank()) {
                return false
            } else {
                streamDeleteEventService.getDeleteEvent(pipeline.pipelineId) ?: return false
                streamDeleteEventService.deleteDeleteEvent(pipeline.pipelineId)
                return false
            }
        } else {
            if (deleteRule.types.isEmpty() || !deleteRule.check()) {
                logger.warn("${gitRequestEvent.gitProjectId} delete event ${gitRequestEvent.id} error: format error")
                return false
            }
        }
        return true
    }

    fun matchAndStartParams(
        context: StreamTriggerContext,
        triggerOn: TriggerOn?,
        needMatch: Boolean = true
    ): Pair<Boolean, Map<String, String>> {
        with(context) {
            logger.info("match and start params|triggerOn:$triggerOn")
            val element = TriggerBuilder.buildCodeGitWebHookTriggerElement(
                gitEvent = gitEvent,
                triggerOn = triggerOn
            ) ?: return Pair(false, emptyMap())
            val webHookParams = WebhookElementParamsRegistrar.getService(element = element).getWebhookElementParams(
                element = element,
                variables = mapOf()
            )!!
            logger.info("match and start params, element:$element, webHookParams:$webHookParams")
            val matcher = TriggerBuilder.buildGitWebHookMatcher(gitEvent)
            val repository = TriggerBuilder.buildCodeGitRepository(streamSetting)
            val isMatch = if (needMatch) {
                matcher.isMatch(
                    projectId = context.streamSetting.projectCode ?: "",
                    // 如果是新的流水线,pipelineId还是为空,使用displayName
                    pipelineId = if (context.pipeline.pipelineId.isEmpty()) {
                        context.pipeline.displayName
                    } else {
                        context.pipeline.pipelineId
                    },
                    repository = repository,
                    webHookParams = webHookParams
                ).isMatch
            } else {
                true
            }
            val startParam = if (isMatch) {
                WebhookStartParamsRegistrar.getService(element = element).getStartParams(
                    projectId = streamSetting.projectCode ?: "",
                    element = element,
                    repo = repository,
                    matcher = matcher,
                    variables = mapOf(),
                    params = webHookParams,
                    matchResult = ScmWebhookMatcher.MatchResult(isMatch = isMatch)
                ).map { entry -> entry.key to entry.value.toString() }.toMap()
            } else {
                emptyMap()
            }
            return Pair(isMatch, startParam)
        }
    }

    fun getStartParams(
        context: StreamTriggerContext,
        triggerOn: TriggerOn?
    ): Map<String, String> {
        return matchAndStartParams(
            context = context,
            triggerOn = triggerOn,
            needMatch = false
        ).second
    }

    private fun GitRequestEvent.isDefaultBranchTrigger(defaultBranch: String?) =
        objectKind == TGitObjectKind.PUSH.value && branch == defaultBranch

    fun getChangeSet(context: StreamTriggerContext): Set<String>? {
        return when (context.gitEvent) {
            is GitPushEvent -> {
                getCommitChangeSet(context)
            }
            is GitMergeRequestEvent -> {
                context.mrChangeSet
            }
            else -> {
                null
            }
        }
    }

    private fun getCommitChangeSet(context: StreamTriggerContext): Set<String> {
        val gitEvent = context.gitEvent as GitPushEvent
        val changeSet = mutableSetOf<String>()
        if (gitEvent.operation_kind == TGitPushOperationKind.UPDATE_NONFASTFORWORD.value) {
            for (i in 1..10) {
                // 反向进行三点比较可以比较出rebase的真实提交
                val result = streamScmService.getCommitChangeFileListRetry(
                    token = null,
                    userId = context.streamSetting.enableUserId,
                    gitProjectId = context.streamSetting.gitProjectId,
                    from = gitEvent.after,
                    to = gitEvent.before,
                    straight = false,
                    page = i,
                    pageSize = 100
                )
                changeSet.addAll(result.map {
                    if (it.deletedFile) {
                        it.oldPath
                    } else {
                        it.newPath
                    }
                }
                )
                if (result.size < 100) {
                    break
                }
            }
            return changeSet
        }

        gitEvent.commits?.forEach { commit ->
            changeSet.addAll(commit.added?.map { it }?.toSet() ?: emptySet())
            changeSet.addAll(commit.modified?.map { it }?.toSet() ?: emptySet())
            changeSet.addAll(commit.removed?.map { it }?.toSet() ?: emptySet())
        }
        return changeSet
    }

    // 返回源分支和目标分支
    private fun getBranch(event: GitEvent): Pair<String, String> {
        return when (event) {
            is GitPushEvent -> {
                Pair(event.ref.removePrefix("refs/heads/"), event.ref.removePrefix("refs/heads/"))
            }
            is GitMergeRequestEvent -> {
                Pair(
                    event.object_attributes.source_branch.removePrefix("refs/heads/"),
                    event.object_attributes.target_branch.removePrefix("refs/heads/")
                )
            }
            else -> Pair("", "")
        }
    }
}
