package com.tencent.devops.environment.service.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.job.ccreq.CCHostPropertyFilter
import com.tencent.devops.environment.pojo.job.ccreq.CCListHostWithoutBizReq
import com.tencent.devops.environment.pojo.job.ccreq.CCPage
import com.tencent.devops.environment.pojo.job.ccreq.CCRules
import com.tencent.devops.environment.pojo.job.ccres.CCResp
import com.tencent.devops.environment.pojo.job.req.Host
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service("PermissionManageService")
class PermissionManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val jobDao: JobDao,
    private val nodeDao: NodeDao,
    private val environmentPermissionService: EnvironmentPermissionService
) {
    @Value("\${job.bkAppCode:}")
    private val bkAppCode = ""

    @Value("\${job.bkAppSecret:}")
    private val bkAppSecret = ""

    @Value("\${job.bkSupplierAccount:}")
    private val bkSupplierAccount = ""

    @Value("\${job.bkccListHostWithoutBizReq:}")
    private val bkccListHostWithoutBizReq = ""

    companion object {
        private val logger = LoggerFactory.getLogger(PermissionManageService::class.java)
    }

    fun isJobInsBelongToProj(projectId: String, jobInstanceId: Long): Boolean {
        val jobProjRecord = jobDao.getProjIdFromJobInsIdList(dslContext, projectId, jobInstanceId)
        if (logger.isDebugEnabled) logger.debug("[getProjIdFromJob] jobProjRecord: $jobProjRecord")

        return if (!jobProjRecord.isEmpty()) {
            val projectIdFromTable = jobProjRecord.map { it.projectId }
            projectId == projectIdFromTable[0]
        } else {
            if (logger.isDebugEnabled) logger.debug("[getProjIdFromJob] no record.")
            false
        }
    }

    fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        val jobProjInsertResult = jobDao.addJobProjRecord(dslContext, projectId, jobInstanceId, createUser)
        if (logger.isDebugEnabled) logger.debug("[recordJobInsToProj] jobProjInsertResult: $jobProjInsertResult")
    }

    fun isUserHasAllUsePermission(userId: String, projectId: String, allHostList: List<Host>) {
        // 用户有使用该节点的权限
        val nodeRecords = nodeDao.getNodesFromHostList(dslContext, projectId, allHostList) //所有host对应的T_NODE表中的记录
        val nodeIdList: List<Long> = nodeRecords.map { it.nodeId } //所有host对应的T_NODE表中的host_id
        val canUseNodeIds = environmentPermissionService.listNodeByPermission(
            userId, projectId, AuthPermission.USE
        ) // 用户所有有权限使用的节点记录
        val unauthorizedNodeIds = nodeIdList.filterNot {
            canUseNodeIds.contains(it)
        } // 传进来的host 不在用户有权限使用的记录列表中的（用node_id来筛选）
        if (unauthorizedNodeIds.isNotEmpty()) { // unauthorizedNodeIds - 为空：用户有所有传进来的host节点的权限；- 不为空：其中的节点用户没权限，抛出异常。
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NO_USE_PERMISSSION,
                params = arrayOf(unauthorizedNodeIds.joinToString(",") { HashUtil.encodeLongId(it) })
            )
        }

        // 用户or节点导入人 是机器的主备负责人（用户：函数中形参userId；节点导入人：T_NODE表中的createdUser）
//        val nodeCreateUserList: List<String> = nodeRecords.map { it.createdUser } // 所有host对应的节点导入人
        val nodeIpList: List<String> = nodeRecords.map { it.nodeIp } // 所有host对应的ip

        // ext中实现：从cmdb中 用ip查询机器的主备负责人
//        val cmdbNodeList = esbAgentClient.getCmdbNodeByIps(userId, nodeIpList).nodes // 所有ip对应的node节点记录（userId权限？）
//        val cmdbIpToNodeMap: Map<String, RawCmdbNode>= cmdbNodeList.associateBy { it.ip }

        // core中实现：从CC中 用对应T_NODE表中记录的host_id查询机器的主备负责人
        val nodeHostIdList: List<Long> = nodeRecords.map { it.hostId } // 所有host对应的ip
        val ccListHostWithoutBizReq = CCListHostWithoutBizReq(
            bkAppCode = bkAppCode,
            bkAppSecret = bkAppSecret,
            bkUsername = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE,
            bkSupplierAccount = bkSupplierAccount,
            page = CCPage(0, 3),
            fields = listOf("bk_host_id", "bk_cloud_id", "bk_host_innerip", "operator", "bk_bak_operator"),
            hostPropertyFilter = CCHostPropertyFilter(
                condition = "OR",
                rules = nodeHostIdList.map {
                    CCRules(
                        field = "bk_host_id",
                        operator = "equal",
                        value = it,
                    )
                }
            )
        )
        val requestContent = jacksonObjectMapper().writeValueAsString(ccListHostWithoutBizReq)
        val headers = mutableMapOf("accept" to "*/*", "Content-Type" to "application/json")
        val ccListHostWithoutBizRes = OkhttpUtils.doPost(bkccListHostWithoutBizReq, requestContent, headers)
        val responseBody = ccListHostWithoutBizRes.body?.string()
        val ccResp = jacksonObjectMapper().readValue<CCResp>(responseBody!!)
        if (logger.isDebugEnabled) logger.debug("ccResp: $ccResp")

        val nodeIpToNodeMap = nodeRecords.associateBy { it.nodeIp } // 所有host的：ip - 记录 映射
        var invalidIpList = listOf<String>()
        ccResp.data.info.map { value ->
            invalidIpList = nodeIpList.filter {
                val isOperator = value.operator == userId || value.operator == nodeIpToNodeMap[it]!!.createdUser
                val isBakOpertor = value.bkBakOperator?.split(",")?.contains(userId) ?: false ||
                    value.bkBakOperator?.split(",")?.contains(nodeIpToNodeMap[it]!!.createdUser) ?: false
                !isOperator && !isBakOpertor
            }
        }
        if (logger.isDebugEnabled) logger.debug("invalidIpList: $invalidIpList")

        if (invalidIpList.isNotEmpty()) {
            throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_IP_ILLEGAL_USER,
                params = arrayOf(invalidIpList.joinToString(","))
            )
        }
    }
}