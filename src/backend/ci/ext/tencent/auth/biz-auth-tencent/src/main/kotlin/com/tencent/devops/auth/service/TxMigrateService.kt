package com.tencent.devops.auth.service

import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

interface TxMigrateService {
    fun migrateRemoteDevManager(projectConditionDTO: ProjectConditionDTO)

    /**
     * 异步迁移云研发管理员，提交后立即返回，返回值不再是真实 groupId。
     */
    fun migrateRemoteDevManager(projectCode: String): Int
}
