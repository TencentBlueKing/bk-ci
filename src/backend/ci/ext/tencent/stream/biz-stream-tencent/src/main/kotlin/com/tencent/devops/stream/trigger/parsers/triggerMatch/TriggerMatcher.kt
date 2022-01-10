package com.tencent.devops.stream.trigger.parsers.triggerMatch

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.TriggerOn
import com.tencent.devops.common.webhook.enums.code.tgit.TGitObjectKind
import com.tencent.devops.common.webhook.enums.code.tgit.TGitPushOperationKind
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.common.exception.CommitCheck
import com.tencent.devops.stream.common.exception.TriggerBaseException
import com.tencent.devops.stream.common.exception.TriggerException
import com.tencent.devops.stream.common.exception.Yamls
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCICommitCheckState
import com.tencent.devops.stream.pojo.enums.StreamMrEventAction
import com.tencent.devops.stream.pojo.enums.TriggerReason
import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitMergeRequestEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitPushEvent
import com.tencent.devops.common.webhook.pojo.code.git.GitTagPushEvent
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.stream.trigger.StreamTriggerContext
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.BranchMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.UserMatchUtils
import com.tencent.devops.stream.trigger.template.pojo.NoReplaceTemplate
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import com.tencent.devops.stream.v2.service.StreamScmService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
@Suppress("ComplexCondition")
class TriggerMatcher @Autowired constructor(
    private val streamScmService: StreamScmService,
    private val streamTimerService: StreamTimerService
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

        return match(
            context = context,
            gitProjectInfo = gitProjectInfo,
            triggerOn = ScriptYmlUtils.formatTriggerOn(newYaml.triggerOn),
            changeSet = getChangeSet(context)
        )
    }

    fun match(
        context: StreamTriggerContext,
        gitProjectInfo: GitCIProjectInfo,
        triggerOn: TriggerOn,
        changeSet: Set<String>?
    ): TriggerResult {
        val (sourceBranch, targetBranch) = getBranch(context.gitEvent)

        val gitRequestEvent = context.requestEvent

        // 判断是否是默认分支上的push，来判断是否注册定时任务
        val isTime = if (gitRequestEvent.objectKind == TGitObjectKind.PUSH.value &&
            gitRequestEvent.branch == gitProjectInfo.defaultBranch
        ) {
            isSchedulesMatch(triggerOn, targetBranch, gitRequestEvent, context.pipeline)
        } else {
            false
        }

        val isTrigger = when (context.gitEvent) {
            is GitPushEvent -> {
                isPushMatch(triggerOn, targetBranch, changeSet, gitRequestEvent.userId)
            }
            is GitMergeRequestEvent -> {
                val mrAction = StreamMrEventAction.getActionValue(context.gitEvent) ?: false
                isMrMatch(
                    triggerOn = triggerOn,
                    sourceBranch = sourceBranch,
                    targetBranch = targetBranch,
                    changeSet = changeSet,
                    userId = gitRequestEvent.userId,
                    mrAction = mrAction
                )
            }
            is GitTagPushEvent -> {
                isTagPushMatch(
                    triggerOn,
                    getTag(context.gitEvent),
                    gitRequestEvent.userId,
                    context.gitEvent.create_from
                )
            }
            else -> {
                false
            }
        }

        val startParams = getStartParams(
            context = context,
            triggerOn = triggerOn,
            isTrigger = isTrigger
        ).map { entry -> entry.key to entry.value.toString() }.toMap()
        return TriggerResult(isTrigger, isTime, startParams)
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

    fun isPushMatch(
        triggerOn: TriggerOn,
        eventBranch: String,
        changeSet: Set<String>?,
        userId: String
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

    fun getStartParams(
        context: StreamTriggerContext,
        triggerOn: TriggerOn,
        isTrigger: Boolean
    ): Map<String, Any> {
        if (!isTrigger) {
            return emptyMap()
        }
        with(context) {
            val element = TriggerBuilder.buildCodeGitWebHookTriggerElement(
                gitEvent = gitEvent,
                triggerOn = triggerOn
            ) ?: return emptyMap()
            val webHookParams = WebhookElementParamsRegistrar.getService(element = element).getWebhookElementParams(
                element = element,
                variables = mapOf()
            ) ?: return emptyMap()
            logger.info("get start params, element:$element, webHookParams:$webHookParams")
            val matcher = TriggerBuilder.buildGitWebHookMatcher(gitEvent)
            val repository = TriggerBuilder.buildCodeGitRepository(streamSetting)
            return WebhookStartParamsRegistrar.getService(element = element).getStartParams(
                projectId = streamSetting.projectCode ?: "",
                element = element,
                repo = repository,
                matcher = matcher,
                variables = mapOf(),
                params = webHookParams,
                matchResult = ScmWebhookMatcher.MatchResult(isMatch = isTrigger)
            )
        }
    }

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

    private fun getTag(event: GitTagPushEvent) = event.ref.removePrefix("refs/tags/")
}
