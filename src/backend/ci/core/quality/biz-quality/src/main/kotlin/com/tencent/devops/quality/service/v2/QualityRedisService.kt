package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.redis.RedisOperation

class QualityRedisService constructor(
		val redisOperation: RedisOperation
): Runnable {
	override fun run() {
		TODO("Not yet implemented")
	}
}