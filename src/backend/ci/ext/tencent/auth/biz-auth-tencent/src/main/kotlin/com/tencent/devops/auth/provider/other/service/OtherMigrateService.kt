package com.tencent.devops.auth.provider.other.service

import com.tencent.devops.auth.service.TxMigrateService
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

class OtherMigrateService : TxMigrateService {
    override fun migrateRemoteDevManager(projectConditionDTO: ProjectConditionDTO) {}

    override fun migrateRemoteDevManager(projectCode: String): Int {
        return 0
    }
}
