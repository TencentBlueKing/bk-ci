package com.tencent.devops.common.web.annotation

import com.tencent.devops.common.web.constant.BkApiHandleType

/**
 * 蓝盾API校验
 *
 * 使用场景: 插件调用蓝盾API接口,根据具体业务场景（支持的业务场景见BkApiHandleType枚举类）加上此注解进行校验
 * 使用方式: 在API的实现方法上加上此注解
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BkApiPermission(
    val types: Array<BkApiHandleType>
)
