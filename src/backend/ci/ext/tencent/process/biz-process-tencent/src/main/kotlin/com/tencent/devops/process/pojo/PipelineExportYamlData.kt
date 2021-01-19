package com.tencent.devops.process.pojo

import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.task.AbstractTask
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.ci.yaml.Stage

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
