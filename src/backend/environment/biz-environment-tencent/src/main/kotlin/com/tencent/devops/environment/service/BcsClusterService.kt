package com.tencent.devops.environment.service

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.environment.dao.BcsClusterDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.StaticData
import com.tencent.devops.environment.pojo.BcsCluster
import com.tencent.devops.environment.pojo.BcsImageInfo
import com.tencent.devops.environment.pojo.BcsVmModel
import com.tencent.devops.environment.pojo.ProjectConfig
import com.tencent.devops.environment.pojo.ProjectConfigParam
import com.tencent.devops.environment.pojo.ProjectInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BcsClusterService @Autowired constructor(
    private val dslContext: DSLContext,
    private val bcsClusterDao: BcsClusterDao,
    private val nodeDao: NodeDao,
    private val projectConfigDao: ProjectConfigDao
) {
    fun listBcsCluster(): List<BcsCluster> {
        return bcsClusterDao.list().map { BcsCluster(it.clusterId, it.clusterName) }
    }

    fun listBcsVmModel(): List<BcsVmModel> {
        return StaticData.getBcsVmModelList()
    }

    fun listBcsImageList(): List<BcsImageInfo> {
        return StaticData.getBcsImageList()
    }

    fun getProjectInfo(userId: String, projectId: String): ProjectInfo {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        val bcsVmEnabled = projectConfig.bcsvmEnalbed
        val bcsVmQuota = projectConfig.bcsvmQuota
        val bcsVmUsedCount = nodeDao.countBcsVm(dslContext, projectId)
        val bcsVmRestCount = bcsVmQuota - bcsVmUsedCount
        val importQuota = projectConfig.importQuota
        val devCloudEnable = projectConfig.devCloudEnalbed
        val devCloudQuota = projectConfig.devCloudQuota
        val devCloudUsedCount = nodeDao.countDevCloudVm(dslContext, projectId)

        return ProjectInfo(bcsVmEnabled, bcsVmQuota, bcsVmUsedCount, bcsVmRestCount, importQuota, devCloudEnable, devCloudQuota, devCloudUsedCount)
    }

    fun saveProjectConfig(projectConfigParam: ProjectConfigParam) {
        projectConfigDao.saveProjectConfig(dslContext,
                projectConfigParam.projectId,
                projectConfigParam.updatedUser,
                projectConfigParam.bcsVmEnabled,
                projectConfigParam.bcsVmQuota,
                projectConfigParam.importQuota,
                projectConfigParam.devCloudEnable,
                projectConfigParam.devCloudQuota
                )
    }

    fun listProjectConfig(): List<ProjectConfig> {
        return projectConfigDao.listProjectConfig(dslContext).map {
            ProjectConfig(
                    it.projectId,
                    it.updatedUser,
                    it.updatedTime.timestamp(),
                    it.bcsvmEnalbed,
                    it.bcsvmQuota,
                    it.importQuota,
                    it.devCloudEnalbed,
                    it.devCloudQuota
            )
        }
    }

    fun list(page: Int, pageSize: Int, projectId: String?): List<ProjectConfig> {
        return projectConfigDao.list(dslContext, page, pageSize, projectId).map {
            ProjectConfig(
                    it.projectId,
                    it.updatedUser,
                    it.updatedTime.timestamp(),
                    it.bcsvmEnalbed,
                    it.bcsvmQuota,
                    it.importQuota,
                    it.devCloudEnalbed,
                    it.devCloudQuota
            )
        }
    }

    fun countProjectConfig(projectId: String?): Int {
        return projectConfigDao.count(dslContext, projectId)
    }
}