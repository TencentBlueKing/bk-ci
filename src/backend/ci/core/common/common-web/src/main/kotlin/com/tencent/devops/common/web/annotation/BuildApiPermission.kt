package com.tencent.devops.common.web.annotation

import com.tencent.devops.common.web.constant.BuildApiHandleType
import java.lang.annotation.Inherited

/**
 * BuildAPI校验
 *
 * 使用场景1: 插件调用蓝盾buildAPI接口,如果接口存在越权风险，则需要加上此注解进行校验
 * 使用方式: 在BuildAPI的实现方法上加上此注解
 */
@Target(AnnotationTarget.FUNCTION)
@Inherited
@Retention(AnnotationRetention.RUNTIME)
annotation class BuildApiPermission(
    val types: Array<BuildApiHandleType>
)
