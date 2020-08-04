package com.tencent.devops.common.auth.pojo

data class IamApiRes (
		val result: Boolean,
		val code: Int,
		val message: String,
		val data: List<ActionPolicyRes>
)