package com.tencent.devops.environment.pojo.cmdb.common

import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import com.tencent.devops.environment.pojo.cmdb.resp.RawCmdbNode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "公司CMDB机器信息")
data class CmdbServerDTO(
    val serverId: Long,
    val ip: String,
    val operator: String,
    val bakOperatorList: List<String>?,
    val lanIpList: List<String>?,
    val deptId: Int?,
    val hostName: String?,
    val osName: String?
) {
    companion object {

        fun fromRawCmdbNode(rawCmdbNode: RawCmdbNode): CmdbServerDTO {
            val bakOperatorList = rawCmdbNode.bakOperator.split(";").toList()
            val lanIpList = rawCmdbNode.displayIp.split(";").toList()
            return CmdbServerDTO(
                serverId = rawCmdbNode.serverId,
                ip = rawCmdbNode.ip,
                operator = rawCmdbNode.operator,
                bakOperatorList = bakOperatorList,
                lanIpList = lanIpList,
                deptId = rawCmdbNode.deptId,
                hostName = rawCmdbNode.name,
                osName = rawCmdbNode.osName
            )
        }

        fun fromNewCmdbServer(newCmdbServer: NewCmdbServer): CmdbServerDTO {
            val bakOperatorList = newCmdbServer.maintainerBak?.split(";")?.toList()
            val lanIpList = newCmdbServer.innerServerIpv4?.map { it.ip }?.toList()
            val firstIp = newCmdbServer.getFirstIp()
            return CmdbServerDTO(
                serverId = newCmdbServer.serverId,
                ip = firstIp ?: "",
                operator = newCmdbServer.maintainer ?: "",
                bakOperatorList = bakOperatorList,
                lanIpList = lanIpList,
                deptId = newCmdbServer.departmentId,
                hostName = newCmdbServer.hostName,
                osName = newCmdbServer.osName
            )
        }
    }

    private fun hasBakOperator(userId: String): Boolean {
        return bakOperatorList != null && bakOperatorList.contains(userId)
    }

    /**
     * 当前用户是否为该机器的运维负责人或备份负责人
     */
    fun hasOperatorOrBak(userId: String): Boolean {
        return operator == userId || hasBakOperator(userId)
    }

    /**
     * 获取首个IP
     */
    fun getFirstIp(): String {
        if (ip.contains(",")) {

            return ip.split(",")[0]
        }
        return ip
    }

    fun getBakOperatorStr(): String {
        if (bakOperatorList.isNullOrEmpty()) {
            return ""
        }
        return bakOperatorList.joinToString(";")
    }
}
