package com.tencent.devops.process.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserCallBackResource
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackService
import com.tencent.devops.process.pojo.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import com.tencent.devops.process.pojo.pipeline.enums.CallBackNetWorkRegionType
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCallBackResourceImpl @Autowired constructor(
    val projectPipelineCallBackService: ProjectPipelineCallBackService
) : UserCallBackResource {

    override fun create(
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType?,
        event: CallBackEvent,
        secretToken: String?
    ): Result<Boolean> {
        projectPipelineCallBackService.createCallBack(
            userId = userId,
            projectId = projectId,
            url = url,
            region = region,
            event = event,
            secretToken = secretToken
        )
        return Result(true)
    }

    override fun list(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = projectPipelineCallBackService.listByPage(projectId, limit.offset, limit.limit)
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun remove(userId: String, projectId: String, id: Long): Result<Boolean> {
        projectPipelineCallBackService.delete(
            userId = userId,
            projectId = projectId,
            id = id
        )
        return Result(true)
    }

    override fun listHistory(
        userId: String,
        projectId: String,
        url: String,
        event: CallBackEvent,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: 20
        val limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        val result = projectPipelineCallBackService.listHistory(
            userId = userId,
            projectId = projectId,
            callBackUrl = url,
            events = event.name,
            startTime = startTime,
            endTime = endTime,
            offset = limit.offset,
            limit = limit.limit
        )
        return Result(Page(pageNotNull, pageSizeNotNull, result.count, result.records))
    }

    override fun retry(userId: String, projectId: String, id: Long): Result<Boolean> {
        projectPipelineCallBackService.retry(
            userId = userId,
            projectId = projectId,
            id = id
        )
        return Result(true)
    }
}
