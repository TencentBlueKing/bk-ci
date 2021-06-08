package com.tencent.bk.codecc.apiquery.resources

import com.tencent.bk.codecc.apiquery.api.ApigwTaskResource
import com.tencent.bk.codecc.apiquery.service.ITaskService
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.model.BuildIdRelationshipModel
import com.tencent.bk.codecc.apiquery.task.model.CustomProjModel
import com.tencent.bk.codecc.apiquery.task.model.TaskFailRecordModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.vo.pipeline.PipelineTaskVO
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTaskResourceImpl @Autowired constructor(
    private val taskService: ITaskService
) : ApigwTaskResource {

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTaskResourceImpl::class.java)
    }

    override fun getTaskDetailList(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<TaskInfoModel>> {
        return Result(
            taskService.getTaskDetailList(
                taskQueryReq,
                pageNum,
                pageSize,
                sortField,
                sortType
            )
        )
    }

    override fun getTaskDetailListByProjectId(
        projectId: String,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<TaskInfoModel>> {
        return Result(
            taskService.getTaskDetailByProjectId(
                projectId,
                pageNum,
                pageSize,
                sortField,
                sortType
            )
        )
    }

    override fun findCustomProjByTaskIds(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<CustomProjModel>> {
        return Result(
            taskService.findCustomProjByTaskIds(
                taskQueryReq,
                pageNum,
                pageSize,
                sortField,
                sortType
            )
        )
    }

    override fun findToolNameByTaskIds(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<ToolConfigInfoModel>> {
        return Result(
            taskService.findToolListByTaskIds(
                taskQueryReq,
                pageNum,
                pageSize,
                sortField,
                sortType
            )
        )
    }

    override fun getPipelineTask(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<PipelineTaskVO>> {
        return Result(taskService.getTaskInfoByPipelineIdList(taskQueryReq, pageNum, pageSize, sortField, sortType))
    }

    override fun findTaskFailRecord(
        taskQueryReq: TaskQueryReq,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<List<TaskFailRecordModel>> {
        return Result(taskService.findTaskFailRecord(taskQueryReq, pageNum, pageSize, sortField, sortType))
    }

    override fun getBuildIdRelationship(
        taskQueryReq: TaskQueryReq,
        appCode: String
    ): Result<BuildIdRelationshipModel?> {
        return Result(taskService.getbuilIdRelationshipByCodeccBuildId(taskQueryReq))
    }

    override fun getTaskListByToolName(
        toolName: String,
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Result<Page<Long>> {
        return Result(taskService.getTaskListByToolName(toolName, pageNum, pageSize, sortField, sortType))
    }
}
