package com.tencent.devops.ai.dao

import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 知识库目录 DAO。表由 1002_ci_ai_kb_catalog_mysql.sql 创建，
 * 使用通用 JOOQ DSL，避免等待 codegen。
 */
@Repository
class AiKbCatalogDao {

    private val sourceTable = DSL.table("T_AI_KB_SOURCE")
    private val entryTable = DSL.table("T_AI_KB_ENTRY")

    fun listSources(dsl: DSLContext): List<Record> {
        return dsl.selectFrom(sourceTable)
            .orderBy(DSL.field("UPDATED_TIME").desc())
            .fetch()
    }

    fun listEntries(dsl: DSLContext): List<Record> {
        return dsl.selectFrom(entryTable)
            .orderBy(DSL.field("SORT_ORDER").asc(), DSL.field("CREATED_TIME").asc())
            .fetch()
    }

    fun getSource(dsl: DSLContext, id: String): Record? {
        return dsl.selectFrom(sourceTable).where(DSL.field("ID").eq(id)).fetchOne()
    }

    fun getEntry(dsl: DSLContext, id: String): Record? {
        return dsl.selectFrom(entryTable).where(DSL.field("ID").eq(id)).fetchOne()
    }

    fun insertSource(
        dsl: DSLContext,
        id: String,
        sourceName: String,
        description: String?,
        projectId: String?,
        spaceKey: String?,
        spaceId: String?,
        rootDocId: String?,
        rootUrl: String?,
        urlTemplate: String,
        bindAgent: String,
        maxSearch: Int,
        maxLocate: Int,
        maxGet: Int,
        enabled: Boolean
    ) {
        val now = LocalDateTime.now()
        dsl.insertInto(sourceTable)
            .set(DSL.field("ID"), id)
            .set(DSL.field("SOURCE_NAME"), sourceName)
            .set(DSL.field("DESCRIPTION"), description)
            .set(DSL.field("PROJECT_ID"), projectId)
            .set(DSL.field("SPACE_KEY"), spaceKey)
            .set(DSL.field("SPACE_ID"), spaceId)
            .set(DSL.field("ROOT_DOC_ID"), rootDocId)
            .set(DSL.field("ROOT_URL"), rootUrl)
            .set(DSL.field("URL_TEMPLATE"), urlTemplate)
            .set(DSL.field("BIND_AGENT"), bindAgent)
            .set(DSL.field("MAX_SEARCH"), maxSearch)
            .set(DSL.field("MAX_LOCATE"), maxLocate)
            .set(DSL.field("MAX_GET"), maxGet)
            .set(DSL.field("ENABLED"), enabled)
            .set(DSL.field("CREATED_TIME"), now)
            .set(DSL.field("UPDATED_TIME"), now)
            .execute()
    }

    fun updateSource(
        dsl: DSLContext,
        id: String,
        sourceName: String,
        description: String?,
        projectId: String?,
        spaceKey: String?,
        spaceId: String?,
        rootDocId: String?,
        rootUrl: String?,
        urlTemplate: String,
        bindAgent: String,
        maxSearch: Int,
        maxLocate: Int,
        maxGet: Int,
        enabled: Boolean
    ): Int {
        return dsl.update(sourceTable)
            .set(DSL.field("SOURCE_NAME"), sourceName)
            .set(DSL.field("DESCRIPTION"), description)
            .set(DSL.field("PROJECT_ID"), projectId)
            .set(DSL.field("SPACE_KEY"), spaceKey)
            .set(DSL.field("SPACE_ID"), spaceId)
            .set(DSL.field("ROOT_DOC_ID"), rootDocId)
            .set(DSL.field("ROOT_URL"), rootUrl)
            .set(DSL.field("URL_TEMPLATE"), urlTemplate)
            .set(DSL.field("BIND_AGENT"), bindAgent)
            .set(DSL.field("MAX_SEARCH"), maxSearch)
            .set(DSL.field("MAX_LOCATE"), maxLocate)
            .set(DSL.field("MAX_GET"), maxGet)
            .set(DSL.field("ENABLED"), enabled)
            .set(DSL.field("UPDATED_TIME"), LocalDateTime.now())
            .where(DSL.field("ID").eq(id))
            .execute()
    }

    fun deleteSource(dsl: DSLContext, id: String): Int {
        dsl.deleteFrom(entryTable).where(DSL.field("SOURCE_ID").eq(id)).execute()
        return dsl.deleteFrom(sourceTable).where(DSL.field("ID").eq(id)).execute()
    }

    fun insertEntry(
        dsl: DSLContext,
        id: String,
        sourceId: String,
        title: String,
        docId: String?,
        keywords: String?,
        hint: String?,
        sortOrder: Int,
        enabled: Boolean
    ) {
        val now = LocalDateTime.now()
        dsl.insertInto(entryTable)
            .set(DSL.field("ID"), id)
            .set(DSL.field("SOURCE_ID"), sourceId)
            .set(DSL.field("TITLE"), title)
            .set(DSL.field("DOC_ID"), docId)
            .set(DSL.field("KEYWORDS"), keywords)
            .set(DSL.field("HINT"), hint)
            .set(DSL.field("SORT_ORDER"), sortOrder)
            .set(DSL.field("ENABLED"), enabled)
            .set(DSL.field("CREATED_TIME"), now)
            .set(DSL.field("UPDATED_TIME"), now)
            .execute()
    }

    fun updateEntry(
        dsl: DSLContext,
        id: String,
        sourceId: String,
        title: String,
        docId: String?,
        keywords: String?,
        hint: String?,
        sortOrder: Int,
        enabled: Boolean
    ): Int {
        return dsl.update(entryTable)
            .set(DSL.field("SOURCE_ID"), sourceId)
            .set(DSL.field("TITLE"), title)
            .set(DSL.field("DOC_ID"), docId)
            .set(DSL.field("KEYWORDS"), keywords)
            .set(DSL.field("HINT"), hint)
            .set(DSL.field("SORT_ORDER"), sortOrder)
            .set(DSL.field("ENABLED"), enabled)
            .set(DSL.field("UPDATED_TIME"), LocalDateTime.now())
            .where(DSL.field("ID").eq(id))
            .execute()
    }

    fun deleteEntry(dsl: DSLContext, id: String): Int {
        return dsl.deleteFrom(entryTable).where(DSL.field("ID").eq(id)).execute()
    }
}
