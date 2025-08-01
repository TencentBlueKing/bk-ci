package com.tencent.devops.common.web.annotation

/**
 * 忽略检查用户API权限
 *
 * 使用场景: 用户态接口,不需要校验权限
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreUserApiPermission
