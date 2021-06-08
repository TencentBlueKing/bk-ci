package com.tencent.devops.process.pojo

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.AbstractTask
import com.tencent.devops.common.ci.task.MarketBuildInput
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.ci.yaml.Stage
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement

/**
 * 目前仅为导出Yaml函数使用
 * 封装原本的model对象，在Yaml中为其添加相应位置的注释
 */
data class PipelineExportYamlData(
    var stage: Stage,
    var jobDataList: List<JobData>
)

data class JobData(
    var job: Job,
    var poolData: PoolData,
    var taskDataList: List<TaskData>
)

data class PoolData(
    var pool: Pool?,
    var tip: String?,
    var replaceYamlStr: String?
)

data class TaskData(
    var task: AbstractTask,
    var tip: String?,
    var replaceYamlStr: String?
)

data class OldVersionTask(
    override var displayName: String?,
    override val inputs: MarketBuildInput?,
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    companion object {
        var taskType = "OldVersionTask"
        const val taskVersion = "@latest"
    }

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
            name = displayName ?: "研发商店插件(${inputs?.atomCode})",
            id = null,
            status = null,
            atomCode = inputs!!.atomCode,
            version = inputs.version,
            data = inputs.data
        )
    }
}
