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

data class PipelineTransferJoinPointImpl(
    var modelStage: Stage? = null,
    var modelJob: Container? = null,
    var modelElement: Element? = null,
    var model: Model? = null,
    var yamlStage: IStage? = null,
    var yamlPreStage: IPreStage? = null,
    var yamlJob: IJob? = null,
    var yamlPreJob: IPreJob? = null,
    var yamlStep: IStep? = null,
    var yamlPreStep: IPreStep? = null,
    var yamlTriggerOn: TriggerOn? = null,
    var yaml: IPreTemplateScriptBuildYamlParser? = null
) : PipelineTransferJoinPoint {

    override fun modelStage(): Stage? = modelStage

    override fun modelJob(): Container? = modelJob

    override fun modelElement(): Element? = modelElement

    override fun model(): Model? = model

    override fun yamlStage(): IStage? = yamlStage

    override fun yamlPreStage(): IPreStage? = yamlPreStage

    override fun yamlJob(): IJob? = yamlJob

    override fun yamlPreJob(): IPreJob? = yamlPreJob

    override fun yamlStep(): IStep? = yamlStep

    override fun yamlPreStep(): IPreStep? = yamlPreStep

    override fun yamlTriggerOn(): TriggerOn? = yamlTriggerOn

    override fun yaml(): IPreTemplateScriptBuildYamlParser? = yaml
}
