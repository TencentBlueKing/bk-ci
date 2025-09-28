package com.tencent.devops.misc.pojo.process

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.misc.dao.process.ProcessDao
import com.tencent.devops.misc.factory.MigrationStrategyFactory
import com.tencent.devops.misc.service.process.ProcessDataDeleteService
import com.tencent.devops.misc.service.project.ProjectDataMigrateHistoryService
import io.swagger.v3.oas.annotations.media.Schema
import org.jooq.DSLContext

@Schema(title = "迁移执行器配置")
data class MigrationExecutorConfig(
    val migratingShardingDslContext: DSLContext,
    val userId: String,
    val projectId: String,
    val dataTag: String?,
    val cancelFlag: Boolean,
    val sourceDataSourceName: String,
    val preMigrationResult: PreMigrationResult,
    val dslContext: DSLContext,
    val processDao: ProcessDao,
    val processDataDeleteService: ProcessDataDeleteService,
    val redisOperation: RedisOperation,
    val client: Client,
    val migrationStrategyFactory: MigrationStrategyFactory,
    val projectDataMigrateHistoryService: ProjectDataMigrateHistoryService,
    val migrationSourceDbDataDeleteFlag: Boolean,
    val migrationProcessDbUnionClusterFlag: Boolean,
    val migrationTimeout: Long,
    val migrationProcessDbMicroServices: String
)
