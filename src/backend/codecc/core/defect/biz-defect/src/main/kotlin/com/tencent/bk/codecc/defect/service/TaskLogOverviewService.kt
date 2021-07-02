package com.tencent.bk.codecc.defect.service

import com.tencent.bk.codecc.defect.vo.TaskLogOverviewVO
import com.tencent.bk.codecc.defect.vo.UploadTaskLogStepVO
import org.springframework.data.domain.PageImpl

interface TaskLogOverviewService {
    fun saveActualExeTools (taskLogOverviewVO: TaskLogOverviewVO): Boolean

    fun getActualExeTools (taskId: Long, buildId: String): List<String>?

    fun calTaskStatus(uploadTaskLogStepVO: UploadTaskLogStepVO)

    fun getTaskLogOverview(taskId: Long, buildId: String?, status: Int?): TaskLogOverviewVO?

    fun getTaskLogOverviewList(taskId: Long, page: Int?, pageSize: Int?): PageImpl<TaskLogOverviewVO>

    fun getAnalyzeResult(taskId: Long, buildId: String?, buildNum: String?, status: Int?): TaskLogOverviewVO?

    fun statTaskAnalyzeCount(taskIds: Collection<Long>, status: Int?, startTime: Long?, endTime: Long?): Int
}
