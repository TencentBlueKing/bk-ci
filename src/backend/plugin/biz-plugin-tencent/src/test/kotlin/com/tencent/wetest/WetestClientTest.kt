package com.tencent.wetest

import com.tencent.devops.plugin.client.WeTestClient
import org.junit.Test

class WetestClientTest {
    private val secretId = "tBQNvtJiwxglMLI3"
    private val secretKey = "TH5YkuT9rJoyyA31"

    private val client = WeTestClient(secretId, secretKey)

    @Test
    fun testGetMyCloud() {
        println(client.getMyCloud())
    }

    @Test
    fun testGetCloudDevices() {
        println(client.getCloudDevices("7", "1", "0"))
    }
}
