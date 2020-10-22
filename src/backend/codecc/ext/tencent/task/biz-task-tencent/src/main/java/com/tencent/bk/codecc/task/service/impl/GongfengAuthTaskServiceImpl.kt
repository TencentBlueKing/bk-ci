package com.tencent.bk.codecc.task.service.impl

import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengPublicProjRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.CustomProjRepository
import com.tencent.bk.codecc.task.dao.mongodbrepository.GongfengStatProjRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.CustomProjEntity
import com.tencent.bk.codecc.task.model.GongfengPublicProjEntity
import com.tencent.bk.codecc.task.model.GongfengStatProjEntity
import com.tencent.devops.common.auth.pojo.GongfengBaseInfo
import com.tencent.devops.common.auth.service.GongfengAuthTaskService
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class GongfengAuthTaskServiceImpl @Autowired constructor(
    private val gongfengPublicProjRepository: GongfengPublicProjRepository,
    private val customProjRepository: CustomProjRepository,
    private val gongfengStatProjRepository: GongfengStatProjRepository,
    private val taskRepository: TaskRepository
): GongfengAuthTaskService {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthTaskServiceImpl::class.java)
    }

    override fun getGongfengProjInfo(taskId: Long): GongfengBaseInfo? {
        logger.info("auth task get gongfeng project info, taskId: {}", taskId)
        val taskInfoEntity = taskRepository.findByTaskId(taskId) ?: return null
        val gongfengPublicProjEntity: GongfengPublicProjEntity? = gongfengPublicProjRepository.findById(taskInfoEntity.gongfengProjectId)
        if (gongfengPublicProjEntity != null) {
            logger.info("auth task gongfeng project, taskId: {} | gongfengProjId: {}", taskId, gongfengPublicProjEntity.id)
            return GongfengBaseInfo(gongfengPublicProjEntity.id, gongfengPublicProjEntity.nameSpace.id, gongfengPublicProjEntity.name, gongfengPublicProjEntity.httpUrlToRepo)
        }

        // 如果是个性化工蜂项目
        val customProjEntity: CustomProjEntity? = if (StringUtils.isBlank(taskInfoEntity.pipelineId)) {
            logger.info("auth task custom project by taskId | taskId: {}", taskId)
            customProjRepository.findFirstByTaskId(taskId)
        } else {
            logger.info("auth task custom project by pipelineId | taskId: {} ｜ pipelineId: {}", taskId, taskInfoEntity.pipelineId)
            customProjRepository.findFirstByPipelineId(taskInfoEntity.pipelineId)
        }
        if (customProjEntity != null) {
            return GongfengBaseInfo(customProjEntity.gongfengProjectId ?: -1, -1, customProjEntity.projectId, customProjEntity.url)
        }

        // 找不到对应的项目信息
        return null
    }


    /**
     * 查询工蜂CI项目信息
     * @param gongfengId
     */
    override fun getGongfengCIProjInfo(gongfengId: Int): GongfengBaseInfo? {
        logger.info("auth gongfeng ci project, gongfengId: {}", gongfengId)
        val gongfengStatProjEntity: GongfengStatProjEntity? = gongfengStatProjRepository.findFirstById(gongfengId)
        if (gongfengStatProjEntity != null) {
            return GongfengBaseInfo(gongfengId, -1, gongfengStatProjEntity.path
                ?: "", "${gongfengStatProjEntity.url}.git")
        }

        //找不到对应的项目信息
        return null
    }
}