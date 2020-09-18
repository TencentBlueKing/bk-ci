package com.tencent.devops.environment.service

import org.junit.Test

class NodeWebsocketServiceTest {
	@Test
	fun emptyJson() {
		val emptyJson = emptyMap<String, String>()
		val emptyStr = emptyJson.toString()
		println(emptyJson)
		println(emptyStr)
	}
}