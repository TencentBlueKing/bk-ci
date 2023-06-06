package com.tencent.devops.auth.service.iam

import com.tencent.devops.common.auth.enums.AuthSystemType

interface MigrateCreatorFixService {
    /**
     * 权限迁移时，获取项目创建者
     */
    fun getProjectCreator(
        projectCode: String,
        authSystemType: AuthSystemType,
        projectCreator: String,
        projectUpdator: String?
    ): String?

    /**
     * 权限迁移时，获取资源创建者
     */
    fun getResourceCreator(
        projectCreator: String,
        resourceCreator: String
    ): String
}
