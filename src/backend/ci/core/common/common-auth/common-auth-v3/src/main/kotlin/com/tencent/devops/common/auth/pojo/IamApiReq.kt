package com.tencent.devops.common.auth.pojo

data class IamApiReq(
		val system: String,
		val type: String,
		val id: String,
		val name: String,
		val creator: String,
		val ancestors: List<AncestorsApiReq>?
)