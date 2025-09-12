package com.tencent.devops.common.auth.rbac

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("auth.circuit.breaker")
data class RbacCircuitBreakerProperties(
    /**
     * 当熔断后等待多久放开熔断 (单位: 秒)
     * 保持 5 秒。过短可能导致频繁试探，过长会影响恢复时间。
     */
    val waitDurationInOpenState: Long = 5,

    /**
     * 熔断放开后，运行通过的请求数，如果达到熔断条件，继续熔断
     */
    val permittedNumberOfCallsInHalfOpenState: Int = 50,

    /**
     * 当错误率达到多少时开启熔断 (0.0F - 100.0F)
     */
    val failureRateThreshold: Float = 50.0F,

    /**
     * 慢请求超过多少时开启熔断 (0.0F - 100.0F)
     */
    val slowCallRateThreshold: Float = 40.0F,

    /**
     * 请求超过多少毫秒就是慢请求
     */
    val slowCallDurationThreshold: Long = 100,

    /**
     * 滑动窗口大小
     */
    val slidingWindowSize: Int = 1000,

    /**
     * 至少需要多少次次调用才开始熔断，这样能避免因调用次数过少导致误判
     */
    val minimumNumberOfCalls: Int = 1000
) {
    /**
     * 构建 CircuitBreakerConfig
     */
    fun toCircuitBreakerConfig(): CircuitBreakerConfig {
        val builder = CircuitBreakerConfig.custom()
        builder.enableAutomaticTransitionFromOpenToHalfOpen()
        builder.writableStackTraceEnabled(false) // 默认false，可根据需要移除
        builder.waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenState))
        builder.permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
        builder.failureRateThreshold(failureRateThreshold)
        builder.slowCallRateThreshold(slowCallRateThreshold)
        builder.slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold))
        builder.slidingWindowSize(slidingWindowSize)
        builder.minimumNumberOfCalls(minimumNumberOfCalls)
        return builder.build()
    }
}
