package com.tencent.devops.remotedev.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.service.client.StartCloudClient
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * remotedev依赖的project，所以关于project与remotedev相关的接口应该放到remotedev
 */
@Service
class RemotedevProjectService @Autowired constructor(
    private val client: Client,
    private val startCloudClient: StartCloudClient,
    private val dslContext: DSLContext,
    private val projectStartAppLinkDao: ProjectStartAppLinkDao
) {
    fun enableRemotedev(userId: String, projectId: String, enable: Boolean): Boolean {
        // 调用project逻辑
        val ok = client.get(ServiceTxProjectResource::class).updateRemotedev(
            userId = userId,
            projectCode = projectId,
            addcloudDesktopNum = null,
            enable = enable
        ).data

        if (ok != true) {
            return false
        }

        // 如果是来关闭的就不用往下走了
        if (!enable) {
            return true
        }

        // 注册start gameid
        val project = client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectId)).data?.firstOrNull()
        if (project == null) {
            logger.error("enableRemotedev get $projectId project null")
            return false
        }

        val appid = startCloudClient.appCreate(
            appName = projectId,
            detail = project.projectName
        )
        if (appid == null) {
            logger.error("enableRemotedev appcreate $projectId|${project.projectName} appid null")
            return false
        }

        return projectStartAppLinkDao.addLink(dslContext, projectId, project.projectName, appid)
    }

    fun migrateOldData(projectId: String?) {
        val projectCodeAndNames =
            client.get(ServiceTxProjectResource::class).projectEnableRemotedev(projectId).data?.toMutableMap()
        if (projectCodeAndNames.isNullOrEmpty()) {
            logger.warn("migrateOldData projectCodeAndNames empty")
            return
        }
        val existProjects = projectStartAppLinkDao.fetchAll(dslContext).map { it.appname }.toSet()
        existProjects.forEach {
            projectCodeAndNames.remove(it)
        }
        projectCodeAndNames.forEach { (appName, detail) ->
            val appid = try {
                val t = startCloudClient.appCreate(
                    appName = appName,
                    detail = detail
                )
                if (t == null) {
                    logger.error("migrateOldData $appName $detail appid null")
                    return@forEach
                }
                t
            } catch (e: Exception) {
                logger.error("migrateOldData $appName $detail error", e)
                return@forEach
            }
            projectStartAppLinkDao.addLink(dslContext, appName, detail, appid)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemotedevProjectService::class.java)
    }
}
