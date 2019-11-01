package com.tencent.devops.project.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.project.tables.records.TProjectLabelRecord
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectLabelDao
import com.tencent.devops.project.pojo.label.ProjectLabel
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class ProjectLabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectLabelDao: ProjectLabelDao
) {
    private val logger = LoggerFactory.getLogger(ProjectLabelService::class.java)

    fun getAllProjectLabel(): List<ProjectLabel> {
        val projectLabelList = mutableListOf<ProjectLabel>()
        val projectLabels = projectLabelDao.getAllProjectLabel(dslContext)
        projectLabels?.forEach {
            projectLabelList.add(convertProjectLabel(it))
        }
        return projectLabelList
    }

    fun getProjectLabelByProjectId(projectId: String): List<ProjectLabel> {
        logger.info("the request projectId is :{}", projectId)
        val projectLabelList = mutableListOf<ProjectLabel>()
        val projectLabels = projectLabelDao.getProjectLabelByProjectId(dslContext, projectId)
        logger.info("the projectLabels is :{}", projectLabels)
        projectLabels?.forEach {
            projectLabelList.add(convertProjectLabel(it))
        }
        return projectLabelList
    }

    fun getProjectLabel(id: String): ProjectLabel? {
        logger.info("the request id is :{}", id)
        val projectLabel = projectLabelDao.getProjectLabel(dslContext, id)
        logger.info("the projectLabel is :{}", projectLabel)
        return if (projectLabel == null) {
            null
        } else {
            convertProjectLabel(projectLabel)
        }
    }

    fun deleteProjectLabel(id: String): Boolean {
        logger.info("the request id is :{}", id)
        projectLabelDao.delete(dslContext, id)
        return true
    }

    fun saveProjectLabel(labelName: String): Boolean {
        logger.info("the request labelName is :{}", labelName)
        val nameCount = projectLabelDao.countByName(dslContext, labelName)
        if (nameCount > 0) {
            throw CustomException(Response.Status.BAD_REQUEST,
                    MessageCodeUtil.generateResponseDataObject<String>(ProjectMessageCode.LABLE_NAME_EXSIT, arrayOf(labelName)).message!! ) // 前面定义的错误码处理规则写在另外一个分支上，暂时未上线，上线后再统一优化
        }
        projectLabelDao.add(dslContext, labelName)
        return true
    }

    fun updateProjectLabel(id: String, labelName: String): Boolean {
        logger.info("the request id is :{},labelName is :{}", id, labelName)
        val nameCount = projectLabelDao.countByName(dslContext, labelName)
        if (nameCount > 0) {
            val projectLabel = projectLabelDao.getProjectLabel(dslContext, id)
            if (null != projectLabel && !labelName.equals(projectLabel.labelName)) {
                throw CustomException(Response.Status.BAD_REQUEST,
                        MessageCodeUtil.generateResponseDataObject<String>(ProjectMessageCode.LABLE_NAME_EXSIT, arrayOf(labelName)).message!! ) // 前面定义的错误码处理规则写在另外一个分支上，暂时未上线，上线后再统一优化
            }
        }
        projectLabelDao.update(dslContext, id, labelName)
        return true
    }

    fun convertProjectLabel(projectLabelRecord: TProjectLabelRecord): ProjectLabel {
        return ProjectLabel(
                projectLabelRecord.id,
                projectLabelRecord.labelName
        )
    }
}
