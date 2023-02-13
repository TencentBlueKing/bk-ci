package com.tencent.devops.common.web.annotation

/**
 * BuildAPI权限校验
 *
 * 使用场景: 插件调用蓝盾buildAPI接口,如果接口存在越权风险，则需要加上此注解进行校验
 * 使用方式: 在BuildAPI的实现方法上加上此注解
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BuildApiPermission
