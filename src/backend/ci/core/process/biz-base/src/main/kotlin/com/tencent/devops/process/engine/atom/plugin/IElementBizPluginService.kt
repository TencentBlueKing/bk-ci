package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementCheckResult

/**
 * 插件扩展点服务类
 */
interface IElementBizPluginService {

    /**
     * 支持的插件
     */
    fun supportElement(element: Element): Boolean

    /**
     * 创建插件[element]后,根据项目ID[projectId]，流水线ID[pipelineId]
     * 流水线名称[pipelineName],操作人[userId],还有渠道[channelCode]，和是否初次新建[create]标识
     * 进行创建后的处理
     */
    fun afterCreate(
        element: Element,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode = ChannelCode.BS,
        create: Boolean,
        container: Container
    )

    /**
     * 在删除[element]插件之前，根据[param]参数调用删除前的预处理
     */
    fun beforeDelete(element: Element, param: BeforeDeleteParam)

    /**
     * 检查[element]插件是否符合要求
     */
    fun check(
        projectId: String?,
        userId: String,
        stage: Stage,
        container: Container,
        element: Element,
        contextMap: Map<String, String>,
        appearedCnt: Int,
        isTemplate: Boolean,
        pipelineId: String
    ): ElementCheckResult
}
