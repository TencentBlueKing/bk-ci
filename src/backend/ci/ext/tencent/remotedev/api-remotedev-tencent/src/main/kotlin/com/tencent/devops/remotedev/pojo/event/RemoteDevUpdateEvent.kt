package com.tencent.devops.remotedev.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.remotedev.pojo.MQ.EXCHANGE_WORKSPACE_UPDATE_FROM_K8S
import com.tencent.devops.remotedev.pojo.MQ.ROUTE_WORKSPACE_UPDATE_FROM_K8S
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.MDC

@Event(EXCHANGE_WORKSPACE_UPDATE_FROM_K8S, ROUTE_WORKSPACE_UPDATE_FROM_K8S)
@ApiModel("k8s -> remoteDev 消息队列事件")
data class RemoteDevUpdateEvent(
    @ApiModelProperty("BIZ ID")
    val traceId: String = MDC.get(TraceTag.BIZID),
    @ApiModelProperty("操作类型")
    val type: UpdateEventType,
    @ApiModelProperty("工作空间名称<唯一性id>")
    val workspaceName: String,
    @ApiModelProperty("是否执行成功")
    val status: Boolean
)
