package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource
import com.tencent.bk.codecc.defect.api.ServicePkgDefectRestResource
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectRespVO
import com.tencent.bk.codecc.defect.vo.openapi.CheckerPkgDefectVO
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.bk.codecc.openapi.v2.ApigwDefectResourceV2
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineOldRsp
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.bk.codecc.task.vo.pipeline.PipelineTaskVO
import com.tencent.bk.codecc.task.vo.tianyi.QueryMyTasksReqVO
import com.tencent.bk.codecc.task.vo.tianyi.TaskInfoVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.web.RestResource
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import javax.ws.rs.HeaderParam

@RestResource
open class ApigwDefectResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwDefectResourceV2 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDefectResourceV2Impl::class.java)
    }

    override fun getTasksByAuthor(
        reqVO: QueryMyTasksReqVO
    ): Result<Page<TaskInfoVO>> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getTasksByAuthor(
            reqVO
        )
    }

    override fun queryToolDefectList(
        taskId: Long,
        reqVO: DefectQueryReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: Sort.Direction?
    ): Result<ToolDefectRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryToolDefectList(
            taskId, reqVO,
            pageNum, pageSize, sortField, sortType
        )
    }

    override fun queryCodeLineInfo(
        taskId: Long,
        toolName: String
    ): Result<ToolClocRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryCodeLine(taskId, toolName)
    }

    override fun queryDeptIdByBgId(bgId: Int): Result<Set<Int>> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).queryDeptIdByBgId(bgId)
    }

    override fun getPipelineTask(pipelineId: String, user: String?): Result<PipelineTaskVO> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getPipelineTask(pipelineId, user)
    }

    override fun triggerCustomPipeline(
        triggerPipelineReq: TriggerPipelineOldReq,
        userId: String
    ): Result<TriggerPipelineOldRsp> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).triggerCustomPipeline(triggerPipelineReq, userId)
    }

    override fun triggerCustomPipelineNew(
        triggerPipelineReq: TriggerPipelineReq,
        appCode: String,
        userId: String
    ): Result<TriggerPipelineRsp> {
        return client.getWithoutRetry(ServiceTaskRestResource::class)
            .triggerCustomPipelineNew(triggerPipelineReq, appCode, userId)
    }

    override fun stopRunningApiTask(
        codeccBuildId: String,
        appCode: String,
        userId: String
    ): Result<Boolean> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).stopRunningApiTask(codeccBuildId, appCode, userId)
    }

    override fun queryTaskOverview(
        reqVO: DeptTaskDefectReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortType: Sort.Direction?
    ): Result<TaskOverviewDetailRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryTaskOverview(
            reqVO, pageNum, pageSize,
            sortType
        )
    }

    override fun getCustomTaskList(
        customProjSource: String,
        pageNum: Int?,
        pageSize: Int?,
        sortType: Sort.Direction?
    ): Result<TaskOverviewDetailRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryCustomTaskOverview(
            customProjSource,
            pageNum, pageSize, sortType
        )
    }

    override fun authorTransfer(
        apigw: String,
        taskId: Long,
        projectId: String,
        appCode: String,
        batchDefectProcessReqVO: BatchDefectProcessReqVO,
        userId: String
    ): Result<Boolean> {
        logger.info("start to author transfer!! task id: $taskId, project id: $projectId")
        batchDefectProcessReqVO.bizType = ComConstants.BusinessType.ASSIGN_DEFECT.value()
        return client.getWithoutRetry(ServiceDefectRestResource::class)
            .batchDefectProcess(taskId, userId, batchDefectProcessReqVO)
    }
}
