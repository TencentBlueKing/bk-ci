package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.job.req.Host
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("PermissionManageService")
class PermissionManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val jobDao: JobDao,
    private val nodeDao: NodeDao,
    private val environmentPermissionService: EnvironmentPermissionService,
    private val queryOperatorService: QueryOperatorService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PermissionManageService::class.java)
    }

    fun isJobInsBelongToProj(projectId: String, jobInstanceId: Long): Boolean {
        val jobProjRecordExist = jobDao.isJobInsExist(dslContext, projectId, jobInstanceId)
        if (logger.isDebugEnabled) logger.debug("[isJobInsBelongToProj] jobProjRecordExist: $jobProjRecordExist")
        return jobProjRecordExist
    }

    fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        val jobProjInsertResult = jobDao.addJobProjRecord(dslContext, projectId, jobInstanceId, createUser)
        if (logger.isDebugEnabled) logger.debug("[recordJobInsToProj] jobProjInsertResult: $jobProjInsertResult")
    }

    fun isUserHasAllPermission(userId: String, projectId: String, allHostList: List<Host>) {
        // 用户有使用该节点的权限
        val nodeRecords = getNodesFromHostList(dslContext, projectId, allHostList).toSet() // 所有host对应的T_NODE表中的记录
        val getRecordByHostIdList = mutableListOf<Host>()
        val getRecordByIpAndBkCloudId = mutableListOf<Host>()
        allHostList.map {
            if (null != it.bkHostId) getRecordByHostIdList.add(it)
            else getRecordByIpAndBkCloudId.add(it)
        }
        if (logger.isDebugEnabled) logger.debug("[isUserHasAllPermission] allHostList: $allHostList")
        if (logger.isDebugEnabled) logger.debug("[isUserHasAllPermission] nodeRecords: $nodeRecords")
        val nodeIdList: List<Long> = nodeRecords.map { it.nodeId } // 所有host对应的T_NODE表中的host_id
        val canUseNodeIds = environmentPermissionService.listNodeByPermission(
            userId, projectId, AuthPermission.USE
        ) // 用户所有有权限使用的节点记录
        val unauthorizedNodeIds = nodeIdList.filterNot {
            canUseNodeIds.contains(it)
        } // 传进来的host 不在用户有权限使用的记录列表中的（用node_id来筛选）
        if (logger.isDebugEnabled) logger.debug("[isUserHasAllPermission] unauthorizedNodeIds: $unauthorizedNodeIds")
        if (unauthorizedNodeIds.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_USE_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        } // unauthorizedNodeIds - 为空：用户有所有传进来的host节点的权限；- 不为空：其中的节点用户没权限，抛出异常。
        // 判断：用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
        queryOperatorService.isOperatorOrBakOperator(userId, nodeRecords)
    }

    private fun getNodesFromHostList(
        dslContext: DSLContext,
        projectId: String,
        hostList: List<Host>
    ): List<TNodeRecord> {
        val getRecordByHostIdList = mutableListOf<Host>()
        val getRecordByIpAndBkCloudId = mutableListOf<Host>()
        hostList.map {
            if (null != it.bkHostId) getRecordByHostIdList.add(it)
            else getRecordByIpAndBkCloudId.add(it)
        }
        return nodeDao.getNodesFromHostListByBkHostId(dslContext, projectId, getRecordByHostIdList).plus(
            nodeDao.getNodesFromHostListByIpAndBkCloudId(dslContext, projectId, getRecordByIpAndBkCloudId)
        )
    }
}