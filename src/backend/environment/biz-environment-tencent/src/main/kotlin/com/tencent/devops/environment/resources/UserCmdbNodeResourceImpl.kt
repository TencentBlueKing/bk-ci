package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserCmdbNodeResource
import com.tencent.devops.environment.pojo.CcNode
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.service.CmdbNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCmdbNodeResourceImpl @Autowired constructor(private val cmdbNodeService: CmdbNodeService) : UserCmdbNodeResource {

    override fun listUserCmdbNodes(userId: String, offset: Int, limit: Int): Result<List<CmdbNode>> {
        return Result(cmdbNodeService.getUserCmdbNodes(userId, offset, limit))
    }

    override fun listUserCmdbNodesNew(
        userId: String,
        bakOperator: Boolean,
        page: Int?,
        pageSize: Int?,
        ips: List<String>?
    ): Result<Page<CmdbNode>> {
        return Result(cmdbNodeService.getUserCmdbNodesNew(userId, bakOperator, page, pageSize, ips ?: listOf()))
    }

    override fun listUserCcNodes(userId: String): Result<List<CcNode>> {
        return Result(cmdbNodeService.getUserCcNodes(userId))
    }

    override fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>): Result<Boolean> {
        cmdbNodeService.addCmdbNodes(userId, projectId, nodeIps)
        return Result(true)
    }

    override fun addCcNodes(userId: String, projectId: String, nodeIps: List<String>): Result<Boolean> {
        cmdbNodeService.addCcNodes(userId, projectId, nodeIps)
        return Result(true)
    }
}