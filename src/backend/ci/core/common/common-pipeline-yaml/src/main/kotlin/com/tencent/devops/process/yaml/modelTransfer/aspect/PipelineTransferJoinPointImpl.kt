package com.tencent.devops.process.yaml.modelTransfer.aspect

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.job.Job as YamlV3Job
import com.tencent.devops.process.yaml.v3.models.stage.Stage as YamlV3Stage
import com.tencent.devops.process.yaml.v3.models.step.Step as YamlV3Step

data class PipelineTransferJoinPointImpl(
    var modelStage: Stage? = null,
    var modelJob: Container? = null,
    var modelElement: Element? = null,
    var yamlStage: YamlV3Stage? = null,
    var yamlJob: YamlV3Job? = null,
    var yamlStep: YamlV3Step? = null,
    var yamlTriggerOn: TriggerOn? = null
) : PipelineTransferJoinPoint {

    override fun modelStage(): Stage? = modelStage

    override fun modelJob(): Container? = modelJob

    override fun modelElement(): Element? = modelElement

    override fun yamlStage(): YamlV3Stage? = yamlStage

    override fun yamlJob(): YamlV3Job? = yamlJob

    override fun yamlStep(): YamlV3Step? = yamlStep

    override fun yamlTriggerOn(): TriggerOn? = yamlTriggerOn
}
