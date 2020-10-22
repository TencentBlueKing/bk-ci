package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.constant.EnvironmentMessageCode
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.openapi.api.apigw.v3.ApigwEnvironmentResourceV3
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwEnvironmentResourceV3Impl @Autowired constructor(
    val client: Client
) : ApigwEnvironmentResourceV3 {

    override fun thirdPartAgentList(appCode: String?, apigwType: String?, userId: String, projectId: String): Result<List<NodeBaseInfo>> {
        logger.info("thirdPartAgentList userId $userId, project $projectId")
        return client.get(ServiceNodeResource::class).listNodeByNodeType(projectId, NodeType.THIRDPARTY)
    }

    override fun getNodeStatus(appCode: String?, apigwType: String?, userId: String, projectId: String, nodeHashId: String): Result<NodeWithPermission?> {
        logger.info("getNodeStatus userId:$userId, projectId: $projectId, nodeHashId: $nodeHashId")
        val nodeList = client.get(ServiceNodeResource::class).listByHashIds(userId, projectId, arrayListOf(nodeHashId)).data
        if (nodeList != null && nodeList.isNotEmpty()) {
            return Result(nodeList[0])
        }
        throw ErrorCodeException(
                errorCode = EnvironmentMessageCode.ERROR_NODE_NOT_EXISTS,
                params = arrayOf(nodeHashId)
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this:: class.java)
    }
}