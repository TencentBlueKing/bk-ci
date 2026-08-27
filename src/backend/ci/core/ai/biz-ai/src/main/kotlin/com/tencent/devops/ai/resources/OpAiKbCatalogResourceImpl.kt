package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.op.OpAiKbCatalogResource
import com.tencent.devops.ai.pojo.AiKbCatalogVO
import com.tencent.devops.ai.pojo.AiKbEntryUpsertRequest
import com.tencent.devops.ai.pojo.AiKbEntryVO
import com.tencent.devops.ai.pojo.AiKbSourceUpsertRequest
import com.tencent.devops.ai.pojo.AiKbSourceVO
import com.tencent.devops.ai.service.AiKbCatalogService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAiKbCatalogResourceImpl @Autowired constructor(
    private val kbCatalogService: AiKbCatalogService
) : OpAiKbCatalogResource {
    override fun list(): Result<AiKbCatalogVO> = Result(kbCatalogService.list())
    override fun createSource(request: AiKbSourceUpsertRequest): Result<AiKbSourceVO> =
        Result(kbCatalogService.createSource(request))
    override fun updateSource(sourceId: String, request: AiKbSourceUpsertRequest): Result<Boolean> =
        Result(kbCatalogService.updateSource(sourceId, request))
    override fun deleteSource(sourceId: String): Result<Boolean> = Result(kbCatalogService.deleteSource(sourceId))
    override fun createEntry(request: AiKbEntryUpsertRequest): Result<AiKbEntryVO> =
        Result(kbCatalogService.createEntry(request))
    override fun updateEntry(entryId: String, request: AiKbEntryUpsertRequest): Result<Boolean> =
        Result(kbCatalogService.updateEntry(entryId, request))
    override fun deleteEntry(entryId: String): Result<Boolean> = Result(kbCatalogService.deleteEntry(entryId))
}
