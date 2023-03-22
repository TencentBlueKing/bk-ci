package com.tencent.devops.statistics.pojo.openapi.constant

object APICode {
    const val BK_NO_APIGW_API = "BkNoApigwApi"//Openapi非apigw接口，不需要鉴权。
    const val BK_PRE_ENHANCEMENT = "BkPreEnhancement"//【前置增强】the method
    const val BK_PARAMETER_NAME = "BkParameterName"//参数名
    const val BK_PARAMETER_VALUE = "BkParameterValue"//参数值
    const val BK_REQUEST_TYPE_APIGWTYPE = "BkRequestTypeApigwtype"//请求类型apigwType[{0}],appCode[{1}],项目[{2}]
    const val BK_PERMISSION_FOR_PROJECT = "BkPermissionForProject"//判断！！！！请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限.
    const val BK_PERMISSION_FOR_PROJECT_VERIFIED = "BkPermissionForProjectVerified"//请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证通过】
    const val BK_VERIFICATION_FAILED = "BkVerificationFailed"//请求类型apigwType[{0}],appCode[{1}],是否有项目[{2}]的权限【验证失败】
    const val BK_PROJECT_LIST = "BkProjectList"//项目列表

}