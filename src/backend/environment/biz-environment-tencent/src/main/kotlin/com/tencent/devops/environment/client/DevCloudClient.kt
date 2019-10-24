package com.tencent.devops.environment.client

import com.tencent.devops.common.environment.agent.client.DevCloudContainerInstanceClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DevCloudClient {

    @Value("\${devCloud.appId}")
    val devCloudAppId: String = ""

    @Value("\${devCloud.token}")
    val devCloudToken: String = ""

    @Value("\${devCloud.url}")
    val devCloudUrl: String = ""

    fun getContainerInstance(staffName: String, id: String) =
        DevCloudContainerInstanceClient.getContainerInstance(devCloudUrl, devCloudAppId, devCloudToken, staffName, id)
}