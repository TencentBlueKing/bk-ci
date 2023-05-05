package com.tencent.devops.stream.trigger.actions

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.stream.pojo.ChangeYamlList
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.trigger.actions.data.ActionData
import com.tencent.devops.stream.trigger.actions.data.ActionMetaData
import com.tencent.devops.stream.trigger.actions.data.StreamTriggerPipeline
import com.tencent.devops.stream.trigger.actions.streamActions.StreamRepoTriggerAction
import com.tencent.devops.stream.trigger.exception.StreamTriggerException
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.service.StreamGitApiService
import com.tencent.devops.stream.trigger.parsers.triggerMatch.TriggerResult
import com.tencent.devops.stream.trigger.pojo.YamlContent
import com.tencent.devops.stream.trigger.pojo.YamlPathListEntry
import com.tencent.devops.stream.trigger.pojo.enums.StreamCommitCheckState

/**
 * 所有action的父类，只提供抽象方法
 */
interface BaseAction {
    // action元数据
    val metaData: ActionMetaData

    // 当前事件源相关的数据
    var data: ActionData

    // 当前事件源绑定的git api
    val api: StreamGitApiService

    // 方便日志打印
    fun format() = "${this::class.qualifiedName}|${data.format()}|${api::class.qualifiedName}"

    /**
     * 填充一些初始化数据
     * 因为可能存在需要上下文参数的情况，所以和load分开
     */
    fun init(): BaseAction?

    /**
     * 初始化一些用于缓存的数据，主要是为了减少接口调用
     */
    fun initCacheData() = Unit

    /**
     * 通过GIT项目唯一ID获取蓝盾项目ID
     * @param gitProjectId git项目唯一标识，为空时取action的执行项目
     */
    fun getProjectCode(gitProjectId: String? = null): String

    /**
     *  由于API接口所需参数不同,所以区分
     *  TGIT -> 接口需要 git project id
     *  Github -> 接口需要 git project name
     */
    fun getGitProjectIdOrName(gitProjectId: String? = null): String

    /**
     * 获取调用当前git平台信息的cred，可能会请求Git api,所以放到action
     * @param personToken yaml语法中会直接填写的accessToken或tickId转换的token
     */
    fun getGitCred(
        personToken: String? = null
    ): StreamGitCred

    /**
     * 获取前端展示相关的requestEvent
     * @param eventStr event 原文
     */
    fun buildRequestEvent(
        eventStr: String
    ): GitRequestEvent?

    /**
     * 判断是否跳过这次stream触发
     */
    fun skipStream(): Boolean

    /**
     * 校验是否触发的一些配置
     */
    @Throws(StreamTriggerException::class)
    fun checkProjectConfig()

    /**
     * 校验Mr是否存在冲突
     * @param path2PipelineExists 目前项目已经存在的流水线列表
     * @return true 不冲突 false 冲突
     */
    fun checkMrConflict(path2PipelineExists: Map<String, StreamTriggerPipeline>): Boolean

    /**
     * 校验是否需要删除流水线
     * @param path2PipelineExists 目前项目已经存在的流水线列表
     */
    fun checkAndDeletePipeline(path2PipelineExists: Map<String, StreamTriggerPipeline>)

    /**
     * 获取流水线yaml文件列表
     */
    fun getYamlPathList(): List<YamlPathListEntry>

    /**
     * 获取yaml文件具体内容
     * @param fileName 文件名称
     */
    fun getYamlContent(fileName: String): YamlContent

    /**
     * 获取本次触发变更的文件列表
     */
    fun getChangeSet(): Set<String>?

    /**
     * 判断当前action是否可以触发，或者创建触发任务，类似定时/删除之类的
     * @param triggerOn 触发器
     */
    fun isMatch(triggerOn: TriggerOn): TriggerResult

    /**
     * 获取用户通过commit options之类的传入的自定变量
     * @param yamlVariables yaml中填写的variables
     */
    fun getUserVariables(yamlVariables: Map<String, Variable>?): Map<String, Variable>?

    /**
     * 是否保存或者更新流水线触发记录
     */
    fun needSaveOrUpdateBranch(): Boolean

    /**
     * 判断当前action是否发送commit check
     */
    fun needSendCommitCheck(): Boolean

    /**
     * 判断是否需要更新流水线最近修改人
     */
    fun needUpdateLastModifyUser(filePath: String): Boolean

    /**
     * 发送commit check
     */
    fun sendCommitCheck(
        buildId: String,
        gitProjectName: String,
        state: StreamCommitCheckState,
        block: Boolean,
        context: String,
        targetUrl: String,
        description: String,
        reportData: Pair<List<String>, MutableMap<String, MutableList<List<String>>>> = Pair(listOf(), mutableMapOf())
    )

    fun sendUnlockWebhook() = Unit

    /**
     * 远程仓库校验凭据信息
     */
    fun registerCheckRepoTriggerCredentials(repoHook: RepositoryHook)

    fun needAddWebhookParams() = false

    fun updatePipelineLastBranchAndDisplayName(
        pipelineId: String,
        branch: String?,
        displayName: String?
    )

    /**
     * 启动类型
     */
    fun getStartType(): StartType

    /**
     *  fork 库触发需要审核，提供审核人
     *  返回空表示不属于fork库触发或是有权限触发
     */
    fun forkMrNeedReviewers() = emptyList<String>()

    /**
     *  fork 库触发审核时，提供yaml文件跳转链接
     */
    fun forkMrYamlList() = emptyList<ChangeYamlList>()

    /**
     * 判断是否是远程仓库触发
     */
    fun checkRepoHookTrigger() = this is StreamRepoTriggerAction || this.data.context.repoTrigger != null

    /*
    * 判断是否if-modify
    */
    fun checkIfModify() = false

    /*
    * 格式化 StreamTriggerContext
    */
    fun parseStreamTriggerContext(cred: StreamGitCred? = null) = Unit
}
