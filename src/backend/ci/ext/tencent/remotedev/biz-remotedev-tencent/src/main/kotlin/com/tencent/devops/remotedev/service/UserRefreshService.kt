package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

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
        logger.info("start refreshAllUser")
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

    @Suppress("NestedBlockDepth", "ComplexCondition")
    private fun updateInfoByTof(userInfo: List<WorkspaceRecord>) {
        userInfo.forEach {
            try {
                Thread.sleep(5)
                try {
                    val userInfo = kotlin.runCatching {
                        client.get(ServiceTxUserResource::class).get(it.createUserId)
                    }.onFailure { logger.warn("get user info error") }.getOrElse { null }?.data

                    if (userInfo == null) {
                        logger.info("user ${it.createUserId} not in t_user")
                    } else if (
                        userInfo.bgName != it.creatorBgName ||
                        userInfo.deptName != it.creatorDeptName ||
                        userInfo.centerName != it.creatorCenterName ||
                        userInfo.groupName != it.creatorGroupName) {
                        logger.info(
                            "${it.createUserId} cent id is diff, " +
                                "tof ${userInfo.bgName} ${userInfo.deptName} " +
                                "${userInfo.centerName} ${userInfo.groupName}, " +
                                "local ${it.creatorBgName} ${it.creatorDeptName} " +
                                "${it.creatorCenterName} ${it.creatorGroupName}"
                        )
                        workspaceDao.updateWorkspaceCreatorInfo(
                            dslContext = dslContext,
                            workspaceName = it.workspaceName,
                            creator = it.createUserId,
                            bgName = userInfo.bgName,
                            deptName = userInfo.deptName,
                            centerName = userInfo.centerName,
                            groupName = userInfo.groupName
                        )
                    }
                } catch (oe: OperationException) {
                    logger.warn("getUserDept fail: ${it.createUserId}|$oe")
                }
            } catch (e: Exception) {
                logger.warn("updateInfoByTof ${it.createUserId} fail: $e")
            }
        }
    }
}
