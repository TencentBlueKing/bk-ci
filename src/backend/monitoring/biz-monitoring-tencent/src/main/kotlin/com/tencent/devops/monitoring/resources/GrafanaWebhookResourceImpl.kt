package com.tencent.devops.monitoring.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.monitoring.api.service.GrafanaWebhookResource
import com.tencent.devops.monitoring.pojo.GrafanaNotification
import com.tencent.devops.monitoring.services.GrafanaWebhookService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class GrafanaWebhookResourceImpl @Autowired constructor(private val grafanaWebhookService: GrafanaWebhookService) : GrafanaWebhookResource {

    override fun webhookCallBack(grafanaNotification: GrafanaNotification): Result<Boolean> {
        return grafanaWebhookService.webhookCallBack(grafanaNotification)
    }
}