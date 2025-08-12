package com.tencent.devops.misc.strategy

import com.tencent.devops.misc.pojo.process.MigrationContext

interface MigrationStrategy {
    fun migrate(context: MigrationContext)
}