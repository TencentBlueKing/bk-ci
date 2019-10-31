package com.tencent.devops.process.util.gcloud

import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.ticket.api.ServiceCredentialResource
import java.util.Base64

class TicketUtil constructor(
    private val client: Client
) {
    fun getAccesIdAndToken(projectId: String, ticketId: String): Pair<String/*accessId*/, String/*accessKey*/> {
        val decoder = Base64.getDecoder()
        val pair = DHUtil.initKey()
        val credential = client.get(ServiceCredentialResource::class)
            .get(projectId, ticketId, Base64.getEncoder().encodeToString(pair.publicKey)).data ?: return Pair("", "")

        val accessId = String(DHUtil.decrypt(
            decoder.decode(credential.v1),
            decoder.decode(credential.publicKey),
            pair.privateKey))

        val accessKey = String(DHUtil.decrypt(
            decoder.decode(credential.v2),
            decoder.decode(credential.publicKey),
            pair.privateKey))

        return Pair(accessId, accessKey)
    }
}