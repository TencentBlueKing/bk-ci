package com.tencent.devops.auth.service.iam

import com.tencent.devops.common.auth.enums.AuthSystemType

interface MigrateCreatorFixService {
    fun getProjectCreator(
        projectCode: String,
        authSystemType: AuthSystemType,
        projectCreator: String,
        projectUpdator: String?
    ): String?

    fun getResourceCreator(
        projectCreator: String,
        resourceCreator: String
    ): String
}
