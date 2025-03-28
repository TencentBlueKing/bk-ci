package com.tencent.devops.common.stream.config.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.pojo.IEvent
import com.tencent.devops.common.stream.constants.StreamBinder

/**
 * 给extend mq初始化用
 */
@Event(destination = "extend.init", binder = StreamBinder.EXTEND_RABBIT)
class ExtendInitEvent : IEvent()