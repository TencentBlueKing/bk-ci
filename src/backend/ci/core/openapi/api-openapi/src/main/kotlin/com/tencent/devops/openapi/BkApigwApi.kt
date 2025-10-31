package com.tencent.devops.openapi

import java.lang.annotation.Inherited

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Inherited
annotation class BkApigwApi(
    val version: String = "",

    /*
    * 特殊指定该接口由蓝鲸网关暴露的路径
    * */
    val apigwPathTail: String = "",
)
