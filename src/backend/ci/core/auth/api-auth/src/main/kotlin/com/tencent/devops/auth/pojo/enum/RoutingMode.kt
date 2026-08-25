package com.tencent.devops.auth.pojo.enum

/**
 * 权限校验的路由模式
 */
enum class RoutingMode {
    /**
     * 正常模式: 直接调用外部第三方权限服务 (IAM)，并由其内部触发异步对账。
     */
    NORMAL,

    /**
     * 本地缓存模式
     */
    INTERNAL,

    /**
     * 熔断模式: 使用断路器包装对外部服务的调用，在故障时自动降级到内部缓存服务。
     */
    CIRCUIT_BREAKER
}
