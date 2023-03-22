package com.tencent.devops.project.constant

object ProjectCode {
    const val BK_PROJECT_NOT_EXIST = "BkProjectNotExist" // 项目不存在
    const val BK_DUPLICATE_PROJECT_NAME = "BkDuplicateProjectName" // 项目名或英文名重复
    const val BK_FAILED_CREATE_PROJECT = "BkFailedCreateProject" // 调用权限中心创建项目失败
    const val BK_PROJECT_ID_INVALID = "BkProjectIdInvalid" // 权限中心创建的项目ID无效
    const val BK_FAILED_CREATE_PROJECT_V0 = "BkFailedCreateProjectV0" // 调用权限中心V0创建项目失败
    const val BK_PROJECT_ID_INVALID_V0 = "BkProjectIdInvalidV0" // 调用权限中心V0创建项目失败
    const val BK_CONTAINER_SERVICE = "BkContainerService" // 容器服务
    const val BK_ASSOCIATED_SYSTEM_NOT_BOUND = "BkAssociatedSystemNotBound" // 关联系统未绑定
    const val BK_NUMBER_AUTHORIZED_USERS_EXCEEDS_LIMIT = "BkNumberAuthorizedUsersExceedsLimit" // 授权用户数越界
    const val BK_FAILED_BSC_CREATE_PROJECT = "BkFailedBscCreateProject" // 调用BSC接口创建项目失败
    const val BK_FAILED_SYNCHRONIZE_PROJECT = "BkFailedSynchronizeProject" // 同步项目到BCS失败
    const val BK_FAILED_UPDATE_PROJECT_INFORMATION = "BkFailedUpdateProjectInformation" // 更新bcs的项目信息失败
    const val BK_FAILED_UPDATE_LOGO_INFORMATION = "BkFailedUpdateLogoInformation" // 更新bcs的项目LOGO信息失败
    const val BK_FAILED_GET_PAASCC_INFORMATION = "BkFailedGetPaasccInformation" // 获取PAASCC项目信息失败
    const val BK_USER_RESIGNED = "BkUserResigned" // 用户{0} 已离职
    const val BK_FAILED_USER_INFORMATION = "BkFailedUserInformation" // 获取用户{0} 信息失败

}