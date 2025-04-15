package com.tencent.devops.common.web.annotation

/**
 * 蓝盾API校验
 *
 * 使用场景: metrics服务部分接口需要完善用户和项目的权限校验，用户必须属于该项目才可以
 * 使用方式: 在API的实现方法上加上此注解
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BkProjectMemberCheck
