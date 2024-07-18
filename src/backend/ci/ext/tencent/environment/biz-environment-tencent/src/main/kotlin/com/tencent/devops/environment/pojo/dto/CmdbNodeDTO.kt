package com.tencent.devops.environment.pojo.dto

import com.tencent.devops.environment.pojo.cmdb.common.CmdbServerDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "类型为CMDB的节点信息")
data class CmdbNodeDTO(
    var nodeId: Long,
    var nodeIp: String,
    var serverId: Long? = null,
    var operator: String? = null,
    var bakOperator: String? = null,
    var osName: String? = null,
    var cloudAreaId: Long? = null,
    var hostId: Long? = null,
    var createdUser: String? = null
) {
    fun operatorOrServerIdOrOsNameChanged(cmdbServerDTO: CmdbServerDTO?): Boolean {
        if (cmdbServerDTO == null) {
            return false
        }
        return operator != cmdbServerDTO.operator ||
            bakOperator != cmdbServerDTO.getBakOperatorStr() ||
            serverId != cmdbServerDTO.serverId ||
            osName != cmdbServerDTO.osName
    }

    private fun hasBakOperator(userId: String): Boolean {
        if (null == bakOperator) {
            return false
        }
        val bakOperatorSet = bakOperator!!.split(";").toSet()
        return bakOperatorSet.contains(userId)
    }

    /**
     * 当前用户是否为该机器的运维负责人或备份负责人
     */
    fun hasOperatorOrBak(userId: String): Boolean {
        return operator == userId || hasBakOperator(userId)
    }
}
