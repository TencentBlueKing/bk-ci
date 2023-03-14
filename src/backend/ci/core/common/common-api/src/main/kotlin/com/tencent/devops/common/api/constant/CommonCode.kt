package com.tencent.devops.common.api.constant

object CommonCode {
    const val BK_FAILED_TO_QUERY_GSE_AGENT_STATUS = "FailedToQueryGseAgentStatus"//查询 Gse Agent 状态失败
    const val BK_FAILED_TO_GET_AGENT_STATUS = "FailedToGetAgentStatus"//获取agent状态失败
    const val BK_FAILED_TO_GET_CMDB_NODE = "FailedToGetCmdbNode"//获取 CMDB 节点失败
    const val BK_FAILED_TO_GET_CMDB_LIST = "FailedToGetCmdbList"//获取CMDB列表失败
    const val BK_STAGES_AND_STEPS_CANNOT_EXIST_BY_SIDE = "StagesAndStepsCannotExistBySide"//stages和steps不能并列存在!
    const val BK_ILLEGAL_JOB_TYPE = "IllegalJobType"//非法的job类型!
    const val BK_ILLEGAL_GITCI_SERVICE_IMAGE_FORMAT = "IllegalGitciServiceImageFormat"//GITCI Service镜像格式非法
    const val BK_USERS_EXCEEDS_THE_LIMIT = "BkUsersExceedsTheLimit"//授权用户数越界:{0}
    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES = "BlueShieldPublicBuildResources"//蓝盾公共构建资源
    const val BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES_NEW = "BlueShieldPublicBuildResourcesNew"//蓝盾公共构建资源(NEW)
    const val BK_PUBLIC_DOCKER_ON_DEVNET_PHYSICAL = "PublicDockerOnDevnetPhysical"//公共：Docker on Devnet 物理机
    const val BK_PUBLIC_DOCKER_ON_DEVCLOUD = "PublicDockerOnDevcloud"//公共：Docker on DevCloud
    const val BK_PUBLIC_DOCKER_ON_BCS = "BkPublicDockerOnBcs"//公共：Docker on Bcs
    const val BK_PRIVATE_SINGLE_BUIL_MACHINE = "PrivateSingleBuilMachine"//私有：单构建机
    const val BK_PRIVATE_BUILD_A_CLUSTER = "PrivateBuildACluster"//私有：构建集群
    const val BK_PCG_PUBLIC_BUILD_RESOURCES = "PcgPublicBuildResources"//PCG公共构建资源
    const val BK_TENCENT_SELF_DEVELOPED_CLOUD= "TencentSelfDevelopedCloud"//腾讯自研云（云devnet资源）
    const val BK_CLOUD_HOSTING_WINDOWS_ON_DEVCLOUD = "CloudHostingWindowsOnDevcloud"//云托管：Windows on DevCloud



}