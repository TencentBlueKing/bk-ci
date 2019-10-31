package com.tencent.devops.websocket.servcie

interface ProjectService {

	fun checkProject(projectId: String, userId: String): Boolean
}