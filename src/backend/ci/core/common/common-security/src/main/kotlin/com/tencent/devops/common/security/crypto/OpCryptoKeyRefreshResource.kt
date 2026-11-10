package com.tencent.devops.common.security.crypto

import com.tencent.devops.common.security.pojo.CryptoKeyRefreshRequest
import com.tencent.devops.common.util.ThreadPoolUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_CRYPTO", description = "OP-AES密钥刷新")
@Path("/op/crypto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class OpCryptoKeyRefreshResource(
    private val applicationName: String,
    private val executor: CryptoKeyRefreshExecutor,
    private val writers: List<CryptoKeyRefreshWriter>
) {
    @Operation(summary = "刷新加密密钥（重加密密文并写 AES_KEY_SHA，可按 Writer 字段过滤）")
    @POST
    @Path("/refresh")
    fun refresh(request: CryptoKeyRefreshRequest?) {
        val writer = request?.writer
        val filters = request?.filters.orEmpty().filterValues { it.isNotBlank() }
        val targetWriters = if (writer.isNullOrBlank()) {
            writers
        } else {
            writers.filter { it.name == writer }
        }
        val writerLabel = writer ?: "all"
        ThreadPoolUtil.submitAction(
            actionTitle = "crypto-key-refresh|$applicationName|writer=$writerLabel|filters=$filters"
        ) {
            executor.runUntilAllDone(
                applicationName = applicationName,
                writers = targetWriters,
                filters = filters
            )
        }
    }
}
