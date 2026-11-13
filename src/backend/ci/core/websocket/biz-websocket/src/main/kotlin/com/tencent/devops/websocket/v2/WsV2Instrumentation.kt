package com.tencent.devops.websocket.v2

/**
 * v2 链路上的饱和信号。默认实现什么都不做，单测不需要 MeterRegistry。
 * 标签只允许固定枚举，避免把项目、流水线或用户写进 Prometheus。
 */
interface WsV2Instrumentation {
    fun connectionRejected(reason: String) = Unit

    fun connectionClosed(reason: String) = Unit

    fun overload(reason: String) = Unit

    fun invalidationsSent(count: Int) = Unit

    fun sendCompleted(elapsedNs: Long, success: Boolean) = Unit

    fun handshakeRejected(reason: String) = Unit

    fun brokerConsumed(result: String, elapsedNs: Long) = Unit

    fun brokerRecovered(success: Boolean) = Unit

    companion object {
        val NONE: WsV2Instrumentation = object : WsV2Instrumentation {}
    }
}
