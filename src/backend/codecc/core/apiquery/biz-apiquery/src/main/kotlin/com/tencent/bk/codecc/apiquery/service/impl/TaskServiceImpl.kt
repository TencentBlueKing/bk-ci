package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.service.ITaskService
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.bk.codecc.apiquery.task.dao.TaskDao
import com.tencent.bk.codecc.apiquery.task.dao.ToolConfigDao
import com.tencent.bk.codecc.apiquery.task.model.BuildIdRelationshipModel
import com.tencent.bk.codecc.apiquery.task.model.CustomProjModel
import com.tencent.bk.codecc.apiquery.task.model.TaskFailRecordModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.bk.codecc.apiquery.vo.pipeline.PipelineTaskVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TaskServiceImpl @Autowired constructor(
    private val taskDao: TaskDao,
    private val toolConfigDao: ToolConfigDao
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
        val taskList = if (taskQueryReq.bgId != null || taskQueryReq.deptId != null) taskDao.findByBgIdAndDeptId(
            taskQueryReq.bgId, taskQueryReq.deptId, pageNum,
            pageSize,
            sortField,
            sortType
        ) else if (!taskQueryReq.taskIdList.isNullOrEmpty()) {
            if (taskQueryReq.taskIdList!!.size > 5000) {
                logger.info("task id list is too long!")
                throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("task id list"))
            }
            taskDao.findTaskInfoModelListByTaskIds(taskQueryReq.taskIdList!!, pageNum, pageSize, sortField, sortType)
        } else {
            emptyList()
        }
        return Page(pageable.pageNumber + 1, pageable.pageSize, taskList.size.toLong(), taskList)
    }

    override fun getTaskDetailByProjectId(
        projectId: String,
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
        return Page(pageable.pageNumber + 1, pageable.pageSize, customProjList.size.toLong(), customProjList)
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
        return Page(pageable.pageNumber + 1, pageable.pageSize, toolNameList.size.toLong(), toolNameList)
    }

    override fun getTaskInfoByPipelineIdList(
        taskQueryReq: TaskQueryReq,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<PipelineTaskVO> {
        val pipelineTaskVOList = mutableListOf<PipelineTaskVO>()
        // 排序分页
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        // 获取任务列表
        val taskList = taskDao.findByPipelineIdList(taskQueryReq.pipelineIdList!!, pageable)
        if (taskList.isNotEmpty()) {
            taskList.forEach {
                val pipelineTaskVO = PipelineTaskVO()
                BeanUtils.copyProperties(it, pipelineTaskVO)

                val toolNames = it.toolNames
                pipelineTaskVO.tools = if (toolNames.isNullOrBlank()) {
                    emptyList()
                } else {
                    toolNames.split(ComConstants.STRING_SPLIT)
                }
                pipelineTaskVOList.add(pipelineTaskVO)
            }
        }

        return Page(pageable.pageNumber + 1, pageable.pageSize, pipelineTaskVOList.size.toLong(), pipelineTaskVOList)
    }

    override fun getbuilIdRelationshipByCodeccBuildId(
        taskQueryReq: TaskQueryReq
    ): BuildIdRelationshipModel? {
        return taskDao.findByCodeccBuildId(taskQueryReq.codeccbuildId!!)
    }

    override fun findTaskFailRecord(
        taskQueryReq: TaskQueryReq,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): List<TaskFailRecordModel> {
        // 排序分页
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        // 获取失败任务列表
        return taskDao.findTaskFailRecord(
            taskQueryReq.projectId,
            taskQueryReq.taskIdList,
            taskQueryReq.pipelineId,
            taskQueryReq.buildId,
            pageable
        )
    }

    override fun getTaskListByToolName(
        toolName: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Page<Long> {
        // 排序分页
        val pageable = PageUtils.convertPageSizeToPageable(pageNum, pageSize, sortField, sortType)
        val toolConfigInfoModel = toolConfigDao.findByToolName(toolName, pageable)
        val taskList = if (toolConfigInfoModel.isNullOrEmpty()) {
            emptyList()
        } else {
            toolConfigInfoModel.map { it.taskId }
        }
        return Page(pageable.pageNumber + 1, pageable.pageSize, taskList.size.toLong(), taskList)
    }
}
