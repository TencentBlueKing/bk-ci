package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectServiceImpl @Autowired constructor(
		private val client: Client
):ProjectService {
	override fun checkProject(projectId: String, userId: String): Boolean {
		try {
			val projectList = client.get(ServiceProjectResource::class).getProjectByUser(userId).data
			val privilegeProjectCodeList = mutableListOf<String>()
			projectList?.map {
				privilegeProjectCodeList.add(it.projectCode)
			}
			if (privilegeProjectCodeList.contains(projectId)) {
				return true
			} else {
				logger.warn("changePage checkProject fail, user:$userId,projectId:$projectId,projectList:$projectList")
				return false
			}
		} catch (e: Exception) {
			logger.error("checkProject fail,message:{}", e)
			// 此处为了解耦，假设调用超时，默认还是做changePage的操作
			return true
		}
	}

	companion object {
		val logger = LoggerFactory.getLogger(this::class.java)
	}
}