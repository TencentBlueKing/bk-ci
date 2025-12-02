package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.IPreStep
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.job.IJob
import com.tencent.devops.process.yaml.v3.models.job.IPreJob
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.IPreStage
import com.tencent.devops.process.yaml.v3.models.stage.IStage
import com.tencent.devops.process.yaml.v3.models.step.IStep

interface IPipelineTransferAspect {
    fun before(jp: PipelineTransferJoinPoint): Any? = null

    fun after(jp: PipelineTransferJoinPoint) = Unit
}

interface IPipelineTransferAspectTrigger : IPipelineTransferAspect
interface IPipelineTransferAspectElement : IPipelineTransferAspect
interface IPipelineTransferAspectJob : IPipelineTransferAspect
interface IPipelineTransferAspectStage : IPipelineTransferAspect

interface IPipelineTransferAspectModel : IPipelineTransferAspect

interface PipelineTransferJoinPoint {
    fun modelStage(): Stage?
    fun modelJob(): Container?
    fun modelElement(): Element?
    fun model(): Model?
    fun yamlStage(): IStage?
    fun yamlPreStage(): IPreStage?
    fun yamlJob(): IJob?
    fun yamlPreJob(): IPreJob?
    fun yamlStep(): IStep?
    fun yamlPreStep(): IPreStep?
    fun yamlTriggerOn(): TriggerOn?
    fun yaml(): IPreTemplateScriptBuildYamlParser?
}
