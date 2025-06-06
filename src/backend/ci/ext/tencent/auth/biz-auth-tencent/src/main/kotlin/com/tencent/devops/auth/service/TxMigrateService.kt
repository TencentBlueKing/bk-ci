package com.tencent.devops.auth.service

import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

interface TxMigrateService {
    fun migrateRemoteDevManager(projectConditionDTO: ProjectConditionDTO)

    fun migrateRemoteDevManager(projectCode: String): Int
}
