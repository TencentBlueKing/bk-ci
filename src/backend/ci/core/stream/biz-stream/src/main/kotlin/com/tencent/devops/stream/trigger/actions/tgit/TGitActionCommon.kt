package com.tencent.devops.stream.trigger.actions.tgit

import com.tencent.devops.common.webhook.pojo.code.git.GitEvent
import com.tencent.devops.common.webhook.service.code.loader.WebhookElementParamsRegistrar
import com.tencent.devops.common.webhook.service.code.loader.WebhookStartParamsRegistrar
import com.tencent.devops.common.webhook.service.code.matcher.ScmWebhookMatcher
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.repository.pojo.CodeGitRepository
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.stream.common.Constansts
import com.tencent.devops.stream.trigger.actions.BaseAction
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.StreamGitTreeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitTreeFileInfoType
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerBuilder
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object TGitActionCommon {

    private val logger = LoggerFactory.getLogger(TGitActionCommon::class.java)

    fun getStartParams(
        action: BaseAction,
        triggerOn: TriggerOn?
    ): Map<String, String> {
        return matchAndStartParams(
            action = action,
            triggerOn = triggerOn,
            needMatch = false
        ).second
    }

    fun matchAndStartParams(
        action: BaseAction,
        triggerOn: TriggerOn?,
        needMatch: Boolean = true
    ): Pair<Boolean, Map<String, String>> {
        logger.info("TGitActionCommon|matchAndStartParams|match and start params|triggerOn|$triggerOn")

        val gitEvent = action.data.event as GitEvent

        val element = TriggerBuilder.buildCodeGitWebHookTriggerElement(
            gitEvent = gitEvent,
            triggerOn = triggerOn
        ) ?: return Pair(false, emptyMap())
        val webHookParams = WebhookElementParamsRegistrar.getService(element = element).getWebhookElementParams(
            element = element,
            variables = mapOf()
        )!!
        logger.info(
            "TGitActionCommon|matchAndStartParams" +
                "|match and start params|element|$element|webHookParams|$webHookParams"
        )

        val matcher = TriggerBuilder.buildGitWebHookMatcher(gitEvent)
        val repository = if (action.data.context.repoTrigger != null) {
            val projectName = action.data.eventCommon.gitProjectName ?: ""
            CodeGitRepository(
                aliasName = projectName,
                url = action.data.context.repoTrigger?.triggerGitHttpUrl ?: "",
                credentialId = "",
                projectName = projectName,
                userName = action.data.getUserId(),
                authType = RepoAuthType.OAUTH,
                projectId = action.getProjectCode(action.data.eventCommon.gitProjectId),
                repoHashId = null
            )
        } else TriggerBuilder.buildCodeGitRepository(action.data.setting)
        val isMatch = if (needMatch) {
            matcher.isMatch(
                projectId = action.data.setting.projectCode ?: "",
                // 如果是新的流水线,pipelineId还是为空,使用displayName
                pipelineId = action.data.context.pipeline!!.pipelineId.ifEmpty {
                    action.data.context.pipeline!!.displayName
                },
                repository = repository,
                webHookParams = webHookParams
            ).isMatch
        } else {
            true
        }
        val startParam = if (isMatch) {
            WebhookStartParamsRegistrar.getService(element = element).getStartParams(
                projectId = action.data.eventCommon.gitProjectId,
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

    /**
     * 拿到所有的ci文件的文件列表
     * @return file,blobId
     */
    fun getYamlPathList(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: StreamGitCred? = null
    ): MutableList<Pair<String, String?>> {
        // 获取指定目录下所有yml文件
        val yamlPathList = getCIYamlList(action, gitProjectId, ref, cred).toMutableList()

        // 兼容旧的根目录yml文件
        val (isCIYamlExist, blobId) = isCIYamlExist(action, gitProjectId, ref, cred)

        if (isCIYamlExist) {
            yamlPathList.add(Pair(Constansts.ciFileName, blobId))
        }
        return yamlPathList
    }

    /**
     * @return name,blobId
     */
    private fun getCIYamlList(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: StreamGitCred?
    ): List<Pair<String, String?>> {
        val ciFileList = action.api.getFileTree(
            gitProjectId = gitProjectId,
            cred = cred ?: action.getGitCred(),
            path = Constansts.ciFileDirectoryName,
            ref = ref?.let { getTriggerBranch(it) },
            recursive = true,
            retry = ApiRequestRetryInfo(true)
        ).filter { checkStreamYamlFile(it) }
        return ciFileList.map {
            Pair(Constansts.ciFileDirectoryName + File.separator + it.name, getBlobId(it))
        }.toList()
    }

    private fun checkStreamYamlFile(gitFileInfo: StreamGitTreeFileInfo): Boolean =
        (
            gitFileInfo.name.endsWith(Constansts.ciFileExtensionYml) ||
                gitFileInfo.name.endsWith(Constansts.ciFileExtensionYaml)
            ) &&
            (gitFileInfo.type == "blob") &&
            !gitFileInfo.name.startsWith("templates/") &&
            // 加以限制：最多仅限一级子目录
            (gitFileInfo.name.count { it == '/' } <= 1)

    private fun getBlobId(f: StreamGitTreeFileInfo?): String? {
        return if (f != null && f.type == StreamGitTreeFileInfoType.BLOB.value && !f.id.isNullOrBlank()) {
            f.id
        } else {
            null
        }
    }

    /**
     * @return isExist,blobId
     */
    private fun isCIYamlExist(
        action: BaseAction,
        gitProjectId: String,
        ref: String?,
        cred: StreamGitCred?
    ): Pair<Boolean, String?> {
        val ciFileList = action.api.getFileTree(
            gitProjectId = gitProjectId,
            cred = cred ?: action.getGitCred(),
            path = "",
            ref = ref?.let { getTriggerBranch(it) },
            recursive = false,
            retry = ApiRequestRetryInfo(true)
        ).filter { it.name == Constansts.ciFileName }
        return Pair(ciFileList.isNotEmpty(), getBlobId(ciFileList.ifEmpty { null }?.first()))
    }

    fun getTriggerBranch(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    fun getCommitTimeStamp(commitTimeStamp: String?): String {
        return if (commitTimeStamp.isNullOrBlank()) {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            formatter.format(Date())
        } else {
            val time = DateTime.parse(commitTimeStamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            sdf.format(time.toDate())
        }
    }
}
