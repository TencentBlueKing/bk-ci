package com.tencent.devops.process.yaml.actions

import com.tencent.devops.process.yaml.actions.data.ActionData
import com.tencent.devops.process.yaml.actions.data.ActionMetaData
import com.tencent.devops.process.yaml.git.pojo.PacGitCred
import com.tencent.devops.process.yaml.git.service.PacGitApiService
import com.tencent.devops.process.yaml.pojo.YamlContent
import com.tencent.devops.process.yaml.pojo.YamlPathListEntry

/**
 * 所有action的父类，只提供抽象方法
 */
interface BaseAction {
    // action元数据
    val metaData: ActionMetaData

    // 当前事件源相关的数据
    var data: ActionData

    // 当前事件源绑定的git api
    val api: PacGitApiService

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
    ): PacGitCred

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
}
