package com.tencent.devops.openapi.resources.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceNodeResource
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

	override fun getNodeStatus(appCode: String?, apigwType: String?, userId: String, projectId: String, nodeHashIds: List<String>?): Result<List<NodeWithPermission>> {
		logger.info("getNodeStatus userId:$userId, projectId: $projectId, nodeHashIds: $nodeHashIds")
		if(nodeHashIds == null || nodeHashIds.isEmpty()) {
			logger.warn("nodeHashIds is empty")
			return Result(emptyList())
		}
		return client.get(ServiceNodeResource::class).listByHashIds(userId, projectId, nodeHashIds!!)
	}

	companion object {
		val logger = LoggerFactory.getLogger(this:: class.java)
	}
}