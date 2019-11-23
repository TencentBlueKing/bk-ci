package com.tencent.devops.project.service

import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.enums.ProjectChannelCode
import com.tencent.devops.project.service.s3.S3Service
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.util.ImageUtil.drawImage
import com.tencent.devops.project.util.ProjectUtils.packagingBean
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectS3Service @Autowired constructor(
    private val s3Service: S3Service,
    private val projectDao: ProjectDao,
    private val tofService: TOFService,
    private val dslContext: DSLContext
){

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectS3Service::class.java)
    }

    fun createCodeCCScanProject(userId: String, projectCreateInfo: ProjectCreateInfo): ProjectVO {
        logger.info("start to create public scan project!")
        var publicScanProject = projectDao.getByEnglishName(dslContext, projectCreateInfo.englishName)
        if (null != publicScanProject) {
            return packagingBean(publicScanProject, setOf())
        }

        try {
            val logoFile = drawImage(projectCreateInfo.englishName.substring(0, 1).toUpperCase())
            try {
                // 发送服务器
                val logoAddress = s3Service.saveLogo(logoFile, projectCreateInfo.englishName)
                val userDeptDetail = tofService.getUserDeptDetail(userId, "")
                logger.info("get user dept info successfully!")
                projectDao.create(
                    dslContext, userId, logoAddress, projectCreateInfo, userDeptDetail,
                    projectCreateInfo.englishName, ProjectChannelCode.BS
                )
            } finally {
                if (logoFile.exists()) {
                    logoFile.delete()
                }
            }
        } catch (e: Throwable) {
            logger.error("Create project failed,", e)
            throw e
        }

        publicScanProject = projectDao.getByEnglishName(dslContext, projectCreateInfo.englishName)
        logger.info("create public scan project successfully!")
        return packagingBean(publicScanProject!!, setOf())
    }

}