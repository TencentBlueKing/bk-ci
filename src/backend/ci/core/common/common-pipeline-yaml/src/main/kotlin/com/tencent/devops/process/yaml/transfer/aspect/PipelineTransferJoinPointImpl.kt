package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.process.yaml.v3.models.IPreTemplateScriptBuildYamlParser
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.PreStage
import com.tencent.devops.process.yaml.v3.models.job.Job as YamlV3Job
import com.tencent.devops.process.yaml.v3.models.stage.Stage as YamlV3Stage
import com.tencent.devops.process.yaml.v3.models.step.Step as YamlV3Step

data class PipelineTransferJoinPointImpl(
    var modelStage: Stage? = null,
    var modelJob: Container? = null,
    var modelElement: Element? = null,
    var model: Model? = null,
    var yamlStage: YamlV3Stage? = null,
    var yamlPreStage: PreStage? = null,
    var yamlJob: YamlV3Job? = null,
    var yamlPreJob: PreJob? = null,
    var yamlStep: YamlV3Step? = null,
    var yamlPreStep: PreStep? = null,
    var yamlTriggerOn: TriggerOn? = null,
    var yaml: IPreTemplateScriptBuildYamlParser? = null
) : PipelineTransferJoinPoint {

    override fun modelStage(): Stage? = modelStage

    override fun modelJob(): Container? = modelJob

    override fun modelElement(): Element? = modelElement

    override fun model(): Model? = model

    override fun yamlStage(): YamlV3Stage? = yamlStage

    override fun yamlPreStage(): PreStage? = yamlPreStage

    override fun yamlJob(): YamlV3Job? = yamlJob

    override fun yamlPreJob(): PreJob? = yamlPreJob

    override fun yamlStep(): YamlV3Step? = yamlStep

    override fun yamlPreStep(): PreStep? = yamlPreStep

    override fun yamlTriggerOn(): TriggerOn? = yamlTriggerOn

    override fun yaml(): IPreTemplateScriptBuildYamlParser? = yaml
}
