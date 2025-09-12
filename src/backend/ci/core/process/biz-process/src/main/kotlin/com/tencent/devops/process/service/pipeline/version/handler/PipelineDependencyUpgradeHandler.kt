package com.tencent.devops.process.service.pipeline.version.handler

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.control.lock.PipelineModelLock
import com.tencent.devops.process.engine.dao.PipelineResourceVersionDao
import com.tencent.devops.process.pojo.pipeline.DeployPipelineResult
import com.tencent.devops.process.pojo.pipeline.PipelineResourceOnlyVersion
import com.tencent.devops.process.pojo.pipeline.PipelineResourceVersion
import com.tencent.devops.process.service.pipeline.version.PipelineVersionCreateContext
import com.tencent.devops.process.service.pipeline.version.PipelineVersionGenerator
import com.tencent.devops.process.service.pipeline.version.PipelineVersionPersistenceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线版本依赖升级
 */
@Service
class PipelineDependencyUpgradeHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext,
    private val pipelineResourceVersionDao: PipelineResourceVersionDao,
    private val pipelineVersionGenerator: PipelineVersionGenerator,
    private val pipelineVersionPersistenceService: PipelineVersionPersistenceService
) : PipelineVersionCreateHandler {
    override fun support(context: PipelineVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.DEPENDENCY_UPGRADE
    }

    override fun handle(context: PipelineVersionCreateContext): DeployPipelineResult {
        with(context) {
            logger.info(
                "handle pipeline dependency upgrade|$projectId|$pipelineId|" +
                    "$versionAction$|${pipelineResourceWithoutVersion.status}"
            )
            val lock = PipelineModelLock(redisOperation, pipelineId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineVersionCreateContext.doHandle(): DeployPipelineResult {
        val resourceOnlyVersion = if (pipelineResourceWithoutVersion.status == VersionStatus.RELEASED) {
            val releaseResource = pipelineResourceVersionDao.getReleaseVersionRecord(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NO_RELEASE_PIPELINE_VERSION
            )
            val resourceOnlyVersion = pipelineVersionGenerator.generateReleaseVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                newModel = pipelineResourceWithoutVersion.model
            )
            // 依赖版本升级,只更新model和版本,其他的保持不变
            val pipelineResourceVersion = releaseResource.copy(
                version = resourceOnlyVersion.version,
                settingVersion = resourceOnlyVersion.settingVersion,
                model = pipelineResourceWithoutVersion.model
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            pipelineVersionPersistenceService.upgradeDependencyForRelease(
                context = this,
                upgradeVersion = releaseResource.version,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
            PipelineResourceOnlyVersion(pipelineResourceVersion)
        } else {
            val branchResource = pipelineResourceVersionDao.getBranchVersionResource(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                branchName = branchName!!
            )
            val resourceOnlyVersion = pipelineVersionGenerator.generateBranchVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                branchName = branchName
            )
            // 如果当前版本分支存在,则只更新model和版本,其他的保持不变,否则创建新版本并且分支版本设置不活跃,不展示出来
            val pipelineResourceVersion = branchResource?.copy(
                version = resourceOnlyVersion.version,
                settingVersion = resourceOnlyVersion.settingVersion,
                model = pipelineResourceWithoutVersion.model
            ) ?: PipelineResourceVersion(
                pipelineResourceWithoutVersion = pipelineResourceWithoutVersion.copy(
                    branchAction = BranchVersionAction.INACTIVE
                ),
                pipelineResourceOnlyVersion = resourceOnlyVersion
            )
            val pipelineSetting = pipelineSettingWithoutVersion.copy(
                version = resourceOnlyVersion.settingVersion!!
            )
            pipelineVersionPersistenceService.upgradeDependencyForBranch(
                context = this,
                pipelineResourceVersion = pipelineResourceVersion,
                pipelineSetting = pipelineSetting
            )
            resourceOnlyVersion
        }
        return DeployPipelineResult(
            pipelineId = pipelineId,
            pipelineName = pipelineBasicInfo.pipelineName,
            version = resourceOnlyVersion.version,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDependencyUpgradeHandler::class.java)
    }
}
