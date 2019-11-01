package com.tencent.devops.process.api.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.service.ProjectPipelineService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectPipelineResourceImpl @Autowired constructor(
    private val projectPipelineService: ProjectPipelineService
) : ServiceProjectPipelineResource {
    override fun listPipelinesByProjectIds(
        userId: String,
        page: Int,
        pageSize: Int,
        channelCode: ChannelCode?,
        checkPermission: Boolean?,
        projectIds: Set<String>
    ): Result<Page<Pipeline>> {
        checkUserId(userId)
        return Result(
            projectPipelineService.listPipelinesByProjectIds(
                projectIds = projectIds,
                page = page,
                pageSize = pageSize,
                channelCode = channelCode
            )
        )
    }

    private fun checkUserId(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
