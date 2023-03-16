package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.dao.WorkspaceDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
@Service
class UserRefreshService @Autowired constructor(
    private val workspaceDao: WorkspaceDao,
    private val dslContext: DSLContext,
    private val client: Client
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }
    private val executorService = Executors.newSingleThreadExecutor()

    fun refreshAllUser(): Boolean {
        executorService.execute {
            val startTime = System.currentTimeMillis()
            // 开始同步数据
            var page = 1
            val pageSize = 1000
            var continueFlag = true
            while (continueFlag) {
                val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
                logger.info(
                    "refreshAllUser page: $page , pageSize: $pageSize, " +
                        "limit: ${pageLimit.limit}, offset: ${pageLimit.offset}"
                )
                val workspaceList = workspaceDao.limitFetchWorkspace(dslContext, pageLimit)
                if (workspaceList == null) {
                    continueFlag = false
                    continue
                }
                updateInfoByTof(workspaceList)

                if (workspaceList.size < pageSize) {
                    continueFlag = false
                    continue
                }
                Thread.sleep(5000)
                page++
            }
            logger.info("Syn all userInfo ${System.currentTimeMillis() - startTime}ms")
        }
        return true
    }

    private fun updateInfoByTof(userInfo: List<TWorkspaceRecord>) {
        userInfo.forEach {
            try {
                Thread.sleep(5)
                try {
                    val userInfo = kotlin.runCatching {
                        client.get(ServiceTxUserResource::class).get(it.creator)
                    }.onFailure { logger.warn("get user info error") }.getOrElse { null }?.data

                    if (userInfo == null) {
                        logger.info("user ${it.creator} not in t_user")
                    } else if (
                        userInfo.bgName != it.creatorBgName ||
                        userInfo.deptName != it.creatorDeptName ||
                        userInfo.centerName != it.creatorCenterName ||
                        userInfo.groupName != it.creatorGroupName) {
                        logger.info(
                            "${it.creator} cent id is diff, " +
                                "tof ${userInfo.bgName} ${userInfo.deptName} ${userInfo.centerName} ${userInfo.groupName}, " +
                                "local ${it.creatorBgName} ${it.creatorDeptName} ${it.creatorCenterName} ${it.creatorGroupName}"
                        )
                        workspaceDao.updateWorkspaceCreatorInfo(
                            dslContext = dslContext,
                            workspaceName = it.name,
                            creator = it.creator,
                            bgName = userInfo.bgName,
                            deptName = userInfo.deptName,
                            centerName = userInfo.centerName,
                            groupName = userInfo.groupName
                        )
                    }
                } catch (oe: OperationException) {
                    logger.warn("getUserDept fail: ${it.creator}|$oe")
                }
            } catch (e: Exception) {
                logger.warn("updateInfoByTof ${it.creator} fail: $e")
            }
        }
    }
}
