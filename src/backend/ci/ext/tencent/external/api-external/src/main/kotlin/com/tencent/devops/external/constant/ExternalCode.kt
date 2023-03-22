package com.tencent.devops.external.constant

object ExternalCode {
    const val BK_FAILED_GET_GITHUB_ACCESS_TOKEN = "BkFailedGetGithubAccessToken"//获取Github access_token失败
    const val BK_PARAMETER_ERROR = "BkParameterError"//参数错误
    const val BK_GITHUB_AUTHENTICATION_FAILED = "BkGithubAuthenticationFailed"//GitHub认证失败
    const val BK_ACCOUNT_NOT_PERMISSIO = "BkAccountNotPermissio"//账户没有{0}的权限
    const val BK_GITHUB_WAREHOUSE_NOT_EXIST = "BkGithubWarehouseNotExist"//GitHub仓库不存在或者是账户没有该项目{0}的权限
    const val BK_GITHUB_PLATFORM_FAILED = "BkGithubPlatformFailed"//GitHub平台{0}失败


    const val BK_ADD_DETECTION_TASK = "BkAddDetectionTask"//添加检测任务
    const val BK_UPDATE_DETECTION_TASK = "BkUpdateDetectionTask"//更新检测任务
    const val BK_GET_WAREHOUSE_LIST = "BkGetWarehouseList"//获取仓库列表
    const val BK_GET_SPECIFIED_BRANCH = "BkGetSpecifiedBranch"//获取指定分支
    const val BK_GET_SPECIFIED_TAG = "BkGetSpecifiedTag"//获取指定Tag
    const val BK_GET_LIST_OF_BRANCHES = "BkGetListOfBranches"//获取分支列表
    const val BK_GET_TAG_LIST = "BkGetTagList"//获取Tag列表


}