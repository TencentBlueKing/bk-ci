package com.tencent.devops.websocket.keys

object WebsocketKeys {
    // session超时 redis桶数量
    const val REDIS_MO: Long = 1000
    // 记录登录态超时hash redis桶， BK:webSocket:hash:sessionId:timeOut:key:1-1000,所有的登录态被打散到1000个桶内
    const val HASH_USER_TIMEOUT_REDIS_KEY = "BK:wsSessionId:timeOut:hashBucket:key:"
    // 用户项目redis key
    const val PROJECT_USER_REDIS_KEY = "BK:websocket:project:user:key:"
    // 定时任务实例锁
    const val WEBSOCKET_CRON_LOCK = "BK:websocket:cron:lock"
}