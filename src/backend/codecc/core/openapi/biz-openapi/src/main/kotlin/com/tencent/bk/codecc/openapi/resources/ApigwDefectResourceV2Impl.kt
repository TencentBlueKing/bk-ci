package com.tencent.bk.codecc.openapi.resources

import com.tencent.bk.codecc.defect.api.ServiceDefectRestResource
import com.tencent.bk.codecc.defect.api.ServicePkgDefectRestResource
import com.tencent.bk.codecc.defect.vo.BatchDefectProcessReqVO
import com.tencent.bk.codecc.defect.vo.ToolClocRspVO
import com.tencent.bk.codecc.defect.vo.ToolDefectRspVO
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO
import com.tencent.bk.codecc.defect.vo.common.DefectQueryReqVO
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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort

@RestResource
open class ApigwDefectResourceV2Impl @Autowired constructor(
    private val client: Client
) : ApigwDefectResourceV2 {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwDefectResourceV2Impl::class.java)
    }

    override fun getTasksByAuthor(
        reqVO: QueryMyTasksReqVO
    ): CodeCCResult<Page<TaskInfoVO>> {
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
    ): CodeCCResult<ToolDefectRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryToolDefectList(
            taskId, reqVO,
            pageNum, pageSize, sortField, sortType
        )
    }

    override fun queryCodeLineInfo(
        taskId: Long
    ): CodeCCResult<ToolClocRspVO> {
        return client.getWithoutRetry(ServicePkgDefectRestResource::class).queryCodeLine(taskId)
    }

    override fun queryDeptIdByBgId(bgId: Int): CodeCCResult<Set<Int>> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).queryDeptIdByBgId(bgId)
    }

    override fun getPipelineTask(pipelineId: String, user: String): CodeCCResult<PipelineTaskVO> {
        return client.getWithoutRetry(ServiceTaskRestResource::class).getPipelineTask(pipelineId, user)
    }

    override fun authorTransfer(
        apigw : String,
        taskId : Long,
        projectId : String,
        appCode : String,
        batchDefectProcessReqVO : BatchDefectProcessReqVO,
        userId : String
    ) : CodeCCResult<Boolean> {
        logger.info("start to author transfer!! task id: $taskId, project id: $projectId")
        batchDefectProcessReqVO.bizType = ComConstants.BusinessType.ASSIGN_DEFECT.value()
        return client.getWithoutRetry(ServiceDefectRestResource::class).batchDefectProcess(taskId, userId, batchDefectProcessReqVO)
    }
}