package com.tencent.devops.process.init

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "resilience4j.circuitbreaker.callback")
data class CallbackCircuitBreakerProperties(
    // 熔断器闭合状态下的错误率阈值，50表示50%，如果错误率达到这个阈值，那么熔断器将进入全熔断状态
    val failureRateThreshold: Float? = null,
    // 当调用执行的时长大于slowCallDurationThreshold时，CircuitBreaker会认为调用为慢调用。
    // 当慢调用占比大于等于此阈值时，CircuitBreaker转变为打开状态，并使调用短路。
    val slowCallRateThreshold: Float? = null,
    // 配置调用执行的时长阈值。当超过这个阈值时，调用会被认为是慢调用，并增加慢调用率。
    val slowCallDurationThreshold: Long? = null,
    // 熔断器全熔断状态持续的时间，全熔断后经过该时间后进入半熔断状态
    val waitDurationInOpenState: Long? = null,
    // 当CircuitBreaker是半开状态时，配置被允许的调用次数。
    val permittedNumberOfCallsInHalfOpenState: Int? = null,
    // 配置滑动窗口的大小。当CircuitBreaker关闭后用于记录调用结果
    val slidingWindow: Int? = null,
    // 配置最小调用次数。在CircuitBreaker计算错误率前，要求（在每滑动窗口周期）用到这个值
    val minimumNumberOfCalls: Int? = null
)
