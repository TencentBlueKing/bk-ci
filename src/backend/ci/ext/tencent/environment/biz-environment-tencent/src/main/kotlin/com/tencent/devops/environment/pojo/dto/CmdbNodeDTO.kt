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
    var createdUser: String? = null,
    var operatorStatus: Byte? = null
) {
    /**
     * 是否需要更新DB中的节点信息
     * 判断条件：
     *   1. DB中operator_status为空（未被计算）
     *   2. 主备份负责人变化
     *   3. 服务器ID变化
     *   4. 操作系统名称变化
     *
     * @param cmdbServerDTO 新查询的CMDB服务器信息
     */
    fun needToModifyCmdbNodeInDB(cmdbServerDTO: CmdbServerDTO?): Boolean {
        if (cmdbServerDTO == null) {
            return false
        }
        return operatorStatus == null ||
            operator != cmdbServerDTO.operator ||
            bakOperator != cmdbServerDTO.getBakOperatorStr() ||
            serverId != cmdbServerDTO.serverId ||
            osName != cmdbServerDTO.osName
    }

}
