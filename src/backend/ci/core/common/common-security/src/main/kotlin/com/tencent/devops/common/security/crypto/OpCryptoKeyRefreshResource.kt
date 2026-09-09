package com.tencent.devops.common.security.crypto

import com.tencent.devops.common.util.ThreadPoolUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_CRYPTO", description = "OP-AES密钥指纹")
@Path("/op/crypto")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class OpCryptoKeyRefreshResource(
    private val applicationName: String,
    private val executor: CryptoKeyRefreshExecutor,
    private val writers: List<CryptoKeyRefreshWriter>
) {
    @Operation(summary = "给存量数据补 AES_KEY_SHA（不重加密凭证）")
    @POST
    @Path("/updateAesKeySha")
    fun updateAesKeySha(
        @Parameter(description = "Writer 名称，为空则刷新当前服务全部")
        @QueryParam("writer")
        writer: String?
    ) {
        val writerLabel = writer ?: "all"
        ThreadPoolUtil.submitAction(
            actionTitle = "crypto-aes-key-sha|$applicationName|writer=$writerLabel"
        ) {
            val targetWriters = if (writer.isNullOrBlank()) {
                writers
            } else {
                writers.filter { it.name == writer }
            }
            executor.updateAesKeySha(
                applicationName = applicationName,
                writers = targetWriters
            )
        }
    }
}
