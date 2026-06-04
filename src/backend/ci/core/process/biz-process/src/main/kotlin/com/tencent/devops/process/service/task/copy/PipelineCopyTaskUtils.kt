package com.tencent.devops.process.service.task.copy

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType

object PipelineCopyTaskUtils {
    fun resourceKey(
        resourceType: PipelineDependentResourceType,
        resourceId: String
    ): String {
        return "${resourceType.name}_$resourceId"
    }
}
