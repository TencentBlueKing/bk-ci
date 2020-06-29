package com.tencent.devops.auth.service

interface AuthRemoteService {

    /**
     * 授权用户
     * @param user user
     * @param serviceCode 服务模块
     * @param resourceType 资源类型
     * @param projectCode 项目英文id
     * @param resourceCode 资源Code唯一标识
     * @param resourceName 资源在权限中心的名称
     * @param authGroupList 用户组，可为空
     */
//    fun grantResource(
//        user: String,
//        serviceCode: AuthServiceCode,
//        resourceType: AuthResourceType,
//        projectCode: String,
//        resourceCode: String,
//        resourceName: String
//    ): String

    // 校验用户权限

    // 判断是否为项目组成员

    // 获取指定资源下指定动作指定实例用户列表
}