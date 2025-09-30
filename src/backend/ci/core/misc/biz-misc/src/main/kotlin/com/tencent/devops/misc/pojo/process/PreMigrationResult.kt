package com.tencent.devops.misc.pojo.process

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "预迁移结果")
data class PreMigrationResult(val routingRuleMap: Map<String, String>)
