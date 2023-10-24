package com.tencent.devops.process.yaml.modelTransfer.aspect

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.job.Job as YamlV3Job
import com.tencent.devops.process.yaml.v3.models.stage.Stage as YamlV3Stage
import com.tencent.devops.process.yaml.v3.models.step.Step as YamlV3Step

interface IPipelineTransferAspect {
    fun before(jp: PipelineTransferJoinPoint): Any? = null

    fun after(jp: PipelineTransferJoinPoint) = Unit
}

interface IPipelineTransferAspectTrigger : IPipelineTransferAspect
interface IPipelineTransferAspectElement : IPipelineTransferAspect
interface IPipelineTransferAspectJob : IPipelineTransferAspect
interface IPipelineTransferAspectStage : IPipelineTransferAspect

interface PipelineTransferJoinPoint {
    fun modelStage(): Stage?
    fun modelJob(): Container?
    fun modelElement(): Element?
    fun yamlStage(): YamlV3Stage?
    fun yamlJob(): YamlV3Job?
    fun yamlStep(): YamlV3Step?
    fun yamlTriggerOn(): TriggerOn?
}
