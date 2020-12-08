package com.tencent.devops.websocket.lock

import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.websocket.keys.WebsocketKeys

class WebsocketCronLock(redisOperation: RedisOperation) :
        RedisLock(
                redisOperation = redisOperation,
                lockKey = WebsocketKeys.WEBSOCKET_CRON_LOCK,
                expiredTimeInSeconds = 30
        )