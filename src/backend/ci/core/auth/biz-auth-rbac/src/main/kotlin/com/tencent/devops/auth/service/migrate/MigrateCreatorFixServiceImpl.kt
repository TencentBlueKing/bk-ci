package com.tencent.devops.auth.service.migrate

import com.tencent.devops.auth.service.iam.MigrateCreatorFixService
import com.tencent.devops.common.auth.enums.AuthSystemType

class MigrateCreatorFixServiceImpl : MigrateCreatorFixService {
    override fun getProjectCreator(
        projectCode: String,
        authSystemType: AuthSystemType,
        projectCreator: String,
        projectUpdator: String?
    ): String? {
        return projectCreator
    }

    override fun getResourceCreator(
        projectCreator: String,
        resourceCreator: String
    ): String {
        return resourceCreator
    }
}
