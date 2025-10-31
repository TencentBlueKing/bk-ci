package com.tencent.devops.misc.strategy

import com.tencent.devops.misc.pojo.process.MigrationContext

interface MigrationStrategy {

    /**
     * 迁移数据
     * @param context 迁移上下文
     */
    fun migrate(context: MigrationContext)
}
