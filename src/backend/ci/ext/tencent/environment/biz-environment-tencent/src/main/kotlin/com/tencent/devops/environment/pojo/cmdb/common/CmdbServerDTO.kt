package com.tencent.devops.environment.pojo.cmdb.common

import com.tencent.devops.environment.pojo.cmdb.resp.NewCmdbServer
import com.tencent.devops.environment.pojo.cmdb.resp.RawCmdbNode
import io.swagger.v3.oas.annotations.media.Schema
import org.slf4j.LoggerFactory

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
        private val logger = LoggerFactory.getLogger(CmdbServerDTO::class.java)

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

    private fun hasOperator(userId: String): Boolean {
        return operator == userId
    }

    private fun hasBakOperator(userId: String): Boolean {
        return bakOperatorList != null && bakOperatorList.contains(userId)
    }

    /**
     * 当前用户是否为该机器的运维负责人或备份负责人
     */
    fun hasOperatorOrBak(userId: String): Boolean {
        return hasOperator(userId) || hasBakOperator(userId)
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

    /**
     * 获取备份负责人字符串，如果超过256个字符，则截取拼接后不超过256个字符的前N个备份负责人组成字符串
     */
    fun getBakOperatorStrLessThanMaxLength(): String {
        if (bakOperatorList.isNullOrEmpty()) {
            return ""
        }
        val maxLength = 256
        var bakOperatorStr = bakOperatorList.joinToString(";")
        if (bakOperatorStr.length > maxLength) {
            logger.info("bakOperatorTruncated|maxLength=$maxLength|rawBakOperatorStr=$bakOperatorStr")
            val firstCharAfterMaxLength = bakOperatorStr[maxLength]
            bakOperatorStr = bakOperatorStr.substring(0, maxLength)
            if (firstCharAfterMaxLength != ';') {
                val lastIndex = bakOperatorStr.lastIndexOf(";")
                bakOperatorStr = bakOperatorStr.substring(0, lastIndex)
            }
        }
        return bakOperatorStr
    }

    /**
     * 获取操作系统名称，如果超过128个字符，则截取前128个字符
     */
    fun getOsNameLessThanMaxLength(): String? {
        if (osName.isNullOrEmpty()) {
            return osName
        }
        val maxLength = 128
        if (osName.length > maxLength) {
            logger.info("osNameTruncated|maxLength=$maxLength|rawOsName=$osName")
            return osName.substring(0, maxLength)
        }
        return osName
    }
}
