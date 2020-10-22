package com.tencent.bk.codecc.openapi.resource

import com.tencent.bk.codecc.defect.api.ServiceOpenSourcePkgDefectRestResource
import com.tencent.bk.codecc.defect.api.ServicePkgDefectRestResource
import com.tencent.bk.codecc.defect.vo.admin.DeptTaskDefectReqVO
import com.tencent.bk.codecc.defect.vo.openapi.TaskOverviewDetailRspVO
import com.tencent.bk.codecc.openapi.v2.ApigwOpenSoureceDefectResourceV2
import com.tencent.bk.codecc.task.api.ServiceGongfengTaskRestResource
import com.tencent.bk.codecc.task.pojo.TriggerPipelineReq
import com.tencent.bk.codecc.task.pojo.TriggerPipelineRsp
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class ApigwOpenSoureceDefectResourceV2Impl @Autowired constructor(
    private val client: Client
): ApigwOpenSoureceDefectResourceV2 {

    override fun triggerCustomPipelineNew(
        triggerPipelineReq: TriggerPipelineReq,
        appCode : String,
        userId: String
    ): CodeCCResult<TriggerPipelineRsp> {
        return client.getWithoutRetry(ServiceGongfengTaskRestResource::class)
            .triggerCustomPipelineNew(triggerPipelineReq, appCode, userId)
    }


    override fun queryTaskOverview(
        reqVO: DeptTaskDefectReqVO,
        pageNum: Int?,
        pageSize: Int?,
        sortType: Sort.Direction?
    ): CodeCCResult<TaskOverviewDetailRspVO> {
        return client.getWithoutRetry(ServiceOpenSourcePkgDefectRestResource::class).queryTaskOverview(
            reqVO, pageNum, pageSize,
            sortType
        )
    }

    override fun getCustomTaskList(
        customProjSource: String,
        pageNum: Int?,
        pageSize: Int?,
        sortType: Sort.Direction?
    ): CodeCCResult<TaskOverviewDetailRspVO> {
        return client.getWithoutRetry(ServiceOpenSourcePkgDefectRestResource::class).queryCustomTaskOverview(
            customProjSource,
            pageNum, pageSize, sortType
        )
    }
}