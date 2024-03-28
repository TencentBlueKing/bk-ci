package com.tencent.devops.repository.sdk.tapd

import com.tencent.devops.repository.sdk.tapd.request.ShardIdRequest
import com.tencent.devops.repository.sdk.tapd.request.StatusMapRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class TapdApiTest {

    val client = DefaultTapdClient(
        serverUrl = "http://tapd.cn",
        apiUrl = "http://apiv2.tapd.cn",
        clientId = "devops",
        clientSecret = "dfdfdfdfdf"
    )

    @Test
    fun statusMap() {
        val request = StatusMapRequest(
            workspaceId = 20357512,
            system = "story"
        )
        val result = client.execute(request = request)
        println(result.data!!["testing"])
    }

    @Test
    fun shardId() {
        val request = ShardIdRequest(
            type = "story",
            shortId = "861747127,868642039"
        )
        val result = client.execute(request = request)
        println(result.data!!)
    }
}
