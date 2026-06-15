package com.tencent.devops.process.service.pipeline.task

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import org.jooq.DSLContext

interface PipelineTaskVersionProcessor {
    /**
     * 插件保存前执行
     */
    fun postProcessBeforeSave(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) = Unit

    /**
     * 插件保存后执行
     */
    fun postProcessAfterSave(
        transactionContext: DSLContext,
        context: PipelineVersionCreateContext,
        pipelineResourceVersion: PipelineResourceVersion,
        pipelineSetting: PipelineSetting,
        element: Element,
        variables: Map<String, String>
    ) = Unit

    fun support(element: Element): Boolean
}