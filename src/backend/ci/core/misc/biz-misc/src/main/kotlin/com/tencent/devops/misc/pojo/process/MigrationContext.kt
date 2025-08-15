package com.tencent.devops.misc.pojo.process

import io.swagger.v3.oas.annotations.media.Schema
import org.jooq.DSLContext

@Schema(title = "迁移上下文")
data class MigrationContext(
    val dslContext: DSLContext,
    val migratingShardingDslContext: DSLContext,
    val projectId: String,
    val pipelineId: String? = null,
    val archiveFlag: Boolean? = null,
    val sourceId: String? = null
)
