package com.tencent.devops.ai.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "知识库目录快照")
data class AiKbCatalogVO(
    val sources: List<AiKbSourceVO>,
    val entries: List<AiKbEntryVO>
)

@Schema(title = "知识库")
data class AiKbSourceVO(
    val id: String,
    val sourceName: String,
    val description: String? = null,
    val projectId: String? = null,
    val spaceKey: String? = null,
    val spaceId: String? = null,
    val rootDocId: String? = null,
    val rootUrl: String? = null,
    val urlTemplate: String,
    val bindAgent: String,
    val maxSearch: Int,
    val maxLocate: Int,
    val maxGet: Int,
    val enabled: Boolean,
    val createdTime: Long? = null,
    val updatedTime: Long? = null
)

@Schema(title = "知识库目录条目")
data class AiKbEntryVO(
    val id: String,
    val sourceId: String,
    val title: String,
    val docId: String? = null,
    val keywords: String? = null,
    val hint: String? = null,
    val sortOrder: Int,
    val enabled: Boolean,
    val createdTime: Long? = null,
    val updatedTime: Long? = null
)

@Schema(title = "知识库创建/更新")
data class AiKbSourceUpsertRequest(
    val sourceName: String,
    val description: String? = null,
    val projectId: String? = null,
    val spaceKey: String? = null,
    val spaceId: String? = null,
    val rootDocId: String? = null,
    val rootUrl: String? = null,
    val urlTemplate: String? = null,
    val bindAgent: String = "supervisor",
    val maxSearch: Int = 1,
    val maxLocate: Int = 1,
    val maxGet: Int = 2,
    val enabled: Boolean = true
)

@Schema(title = "知识库条目创建/更新")
data class AiKbEntryUpsertRequest(
    val sourceId: String,
    val title: String,
    val docId: String? = null,
    val keywords: String? = null,
    val hint: String? = null,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)
