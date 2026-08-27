package com.tencent.devops.ai.service

import com.tencent.devops.ai.constant.AiMessageCode
import com.tencent.devops.ai.dao.AiKbCatalogDao
import com.tencent.devops.ai.pojo.AiKbCatalogVO
import com.tencent.devops.ai.pojo.AiKbEntryUpsertRequest
import com.tencent.devops.ai.pojo.AiKbEntryVO
import com.tencent.devops.ai.pojo.AiKbSourceUpsertRequest
import com.tencent.devops.ai.pojo.AiKbSourceVO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import org.jooq.Record
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class AiKbCatalogService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dao: AiKbCatalogDao
) {

    fun list(): AiKbCatalogVO {
        return AiKbCatalogVO(
            sources = dao.listSources(dslContext).map { it.toSource() },
            entries = dao.listEntries(dslContext).map { it.toEntry() }
        )
    }

    fun createSource(request: AiKbSourceUpsertRequest): AiKbSourceVO {
        val id = UUIDUtil.generate()
        dao.insertSource(
            dsl = dslContext,
            id = id,
            sourceName = request.sourceName.trim(),
            description = request.description,
            projectId = request.projectId?.trim()?.ifBlank { null },
            spaceKey = request.spaceKey,
            spaceId = request.spaceId,
            rootDocId = request.rootDocId,
            rootUrl = request.rootUrl,
            urlTemplate = request.urlTemplate?.ifBlank { null } ?: DEFAULT_URL_TEMPLATE,
            bindAgent = request.bindAgent.ifBlank { "supervisor" },
            maxSearch = request.maxSearch,
            maxLocate = request.maxLocate,
            maxGet = request.maxGet,
            enabled = request.enabled
        )
        return dao.getSource(dslContext, id)?.toSource()
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_KB_SOURCE_FAILED,
                defaultMessage = "Failed to create knowledge source"
            )
    }

    fun updateSource(sourceId: String, request: AiKbSourceUpsertRequest): Boolean {
        requireSource(sourceId)
        return dao.updateSource(
            dsl = dslContext,
            id = sourceId,
            sourceName = request.sourceName.trim(),
            description = request.description,
            projectId = request.projectId?.trim()?.ifBlank { null },
            spaceKey = request.spaceKey,
            spaceId = request.spaceId,
            rootDocId = request.rootDocId,
            rootUrl = request.rootUrl,
            urlTemplate = request.urlTemplate?.ifBlank { null } ?: DEFAULT_URL_TEMPLATE,
            bindAgent = request.bindAgent.ifBlank { "supervisor" },
            maxSearch = request.maxSearch,
            maxLocate = request.maxLocate,
            maxGet = request.maxGet,
            enabled = request.enabled
        ) > 0
    }

    fun deleteSource(sourceId: String): Boolean {
        requireSource(sourceId)
        return dao.deleteSource(dslContext, sourceId) > 0
    }

    fun createEntry(request: AiKbEntryUpsertRequest): AiKbEntryVO {
        requireSource(request.sourceId)
        val id = UUIDUtil.generate()
        dao.insertEntry(
            dsl = dslContext,
            id = id,
            sourceId = request.sourceId,
            title = request.title.trim(),
            docId = request.docId,
            keywords = request.keywords,
            hint = request.hint,
            sortOrder = request.sortOrder,
            enabled = request.enabled
        )
        return dao.getEntry(dslContext, id)?.toEntry()
            ?: throw ErrorCodeException(
                errorCode = AiMessageCode.CREATE_KB_ENTRY_FAILED,
                defaultMessage = "Failed to create knowledge entry"
            )
    }

    fun updateEntry(entryId: String, request: AiKbEntryUpsertRequest): Boolean {
        dao.getEntry(dslContext, entryId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.KB_ENTRY_NOT_FOUND,
                defaultMessage = "Knowledge entry not found"
            )
        requireSource(request.sourceId)
        return dao.updateEntry(
            dsl = dslContext,
            id = entryId,
            sourceId = request.sourceId,
            title = request.title.trim(),
            docId = request.docId,
            keywords = request.keywords,
            hint = request.hint,
            sortOrder = request.sortOrder,
            enabled = request.enabled
        ) > 0
    }

    fun deleteEntry(entryId: String): Boolean {
        dao.getEntry(dslContext, entryId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.KB_ENTRY_NOT_FOUND,
                defaultMessage = "Knowledge entry not found"
            )
        return dao.deleteEntry(dslContext, entryId) > 0
    }

    private fun requireSource(sourceId: String) {
        dao.getSource(dslContext, sourceId)
            ?: throw ErrorCodeException(
                statusCode = 404,
                errorCode = AiMessageCode.KB_SOURCE_NOT_FOUND,
                defaultMessage = "Knowledge source not found"
            )
    }

    private fun Record.toSource(): AiKbSourceVO {
        return AiKbSourceVO(
            id = get("ID", String::class.java),
            sourceName = get("SOURCE_NAME", String::class.java),
            description = get("DESCRIPTION", String::class.java),
            projectId = get("PROJECT_ID", String::class.java),
            spaceKey = get("SPACE_KEY", String::class.java),
            spaceId = get("SPACE_ID", String::class.java),
            rootDocId = get("ROOT_DOC_ID", String::class.java),
            rootUrl = get("ROOT_URL", String::class.java),
            urlTemplate = get("URL_TEMPLATE", String::class.java) ?: DEFAULT_URL_TEMPLATE,
            bindAgent = get("BIND_AGENT", String::class.java) ?: "supervisor",
            maxSearch = get("MAX_SEARCH", Int::class.java) ?: 1,
            maxLocate = get("MAX_LOCATE", Int::class.java) ?: 1,
            maxGet = get("MAX_GET", Int::class.java) ?: 2,
            enabled = get("ENABLED", Boolean::class.java) ?: true,
            createdTime = get("CREATED_TIME", LocalDateTime::class.java)?.toEpoch(),
            updatedTime = get("UPDATED_TIME", LocalDateTime::class.java)?.toEpoch()
        )
    }

    private fun Record.toEntry(): AiKbEntryVO {
        return AiKbEntryVO(
            id = get("ID", String::class.java),
            sourceId = get("SOURCE_ID", String::class.java),
            title = get("TITLE", String::class.java),
            docId = get("DOC_ID", String::class.java),
            keywords = get("KEYWORDS", String::class.java),
            hint = get("HINT", String::class.java),
            sortOrder = get("SORT_ORDER", Int::class.java) ?: 0,
            enabled = get("ENABLED", Boolean::class.java) ?: true,
            createdTime = get("CREATED_TIME", LocalDateTime::class.java)?.toEpoch(),
            updatedTime = get("UPDATED_TIME", LocalDateTime::class.java)?.toEpoch()
        )
    }

    private fun LocalDateTime.toEpoch(): Long {
        return toInstant(ZoneOffset.ofHours(8)).toEpochMilli()
    }

    companion object {
        private const val DEFAULT_URL_TEMPLATE = "https://iwiki.woa.com/p/{docId}"
    }
}
