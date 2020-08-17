package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.service.ITaskService
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.task.model.CustomProjModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskServiceImpl @Autowired constructor(
    private val taskDao: TaskDao
) : ITaskService {

    companion object {
        private val logger = LoggerFactory.getLogger(TaskServiceImpl::class.java)
    }

    override fun getTaskList(taskQueryReq: TaskQueryReq): List<Long> {
        return if (!taskQueryReq.taskIdList.isNullOrEmpty()) {
            taskQueryReq.taskIdList!!
        } else {
            val taskInfoModel = taskDao.findTegSecurityByBgId(taskQueryReq.bgId!!)
            taskInfoModel.map { it.taskId }
        }
    }

    override fun getTaskDetailList(
        taskQueryReq: TaskQueryReq,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<TaskInfoModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val taskList = if (taskQueryReq.bgId != null) taskDao.findByBgId(
            taskQueryReq.bgId!!, pageNum,
            pageSize,
            sortField,
            sortType
        ) else if (!taskQueryReq.taskIdList.isNullOrEmpty()) {
            taskDao.findTaskInfoModelListByTaskIds(taskQueryReq.taskIdList!!, pageNum, pageSize, sortType, sortField)
        } else {
            emptyList()
        }
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, taskList)
    }

    override fun getTaskDetailByProjectId(
        projectId : String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<TaskInfoModel> {
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val taskList = taskDao.findByProjectId(
            projectId, pageNum,
            pageSize,
            sortField,
            sortType
        )
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, taskList)
    }

    override fun findCustomProjByTaskIds(
        taskQueryReq: TaskQueryReq,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<CustomProjModel> {

        if (taskQueryReq.taskIdList.isNullOrEmpty()) {
            logger.error("empty task id list!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val customProjList =
            taskDao.findCustomProjByTaskIds(taskQueryReq.taskIdList!!, pageNum, pageSize, sortType, sortField)
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, customProjList)
    }

    override fun findToolListByTaskIds(
        taskQueryReq: TaskQueryReq,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<ToolConfigInfoModel> {
        if (taskQueryReq.taskIdList.isNullOrEmpty()) {
            logger.error("empty task id list!")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID)
        }
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val toolNameList =
            taskDao.findToolListByTaskIds(taskQueryReq.taskIdList!!, taskQueryReq.status, pageable)
        return Page(pageable.pageNumber + 1, pageable.pageSize, 0L, toolNameList)
    }
}