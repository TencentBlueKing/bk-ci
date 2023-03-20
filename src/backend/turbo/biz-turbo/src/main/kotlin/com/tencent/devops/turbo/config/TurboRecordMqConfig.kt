package com.tencent.devops.turbo.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.common.util.constants.QUEUE_TURBO_PLUGIN_DATA
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_CREATE
import com.tencent.devops.common.util.constants.QUEUE_TURBO_REPORT_UPDATE
import com.tencent.devops.turbo.component.TurboRecordConsumer
import com.tencent.devops.turbo.dto.TurboRecordCreateDto
import com.tencent.devops.turbo.dto.TurboRecordPluginUpdateDto
import com.tencent.devops.turbo.dto.TurboRecordUpdateDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import java.util.function.Consumer

@Configuration
class TurboRecordMqConfig {

    companion object {
        const val STREAM_CONSUMER_GROUP = "turbo-service"
    }

    @Bean
    fun measureEventDispatcher(streamBridge: StreamBridge) = SampleEventDispatcher(streamBridge)

    @EventConsumer(QUEUE_TURBO_REPORT_CREATE, STREAM_CONSUMER_GROUP)
    fun turboRecordCreateListener(
        @Autowired turboRecordConsumer: TurboRecordConsumer
    ): Consumer<Message<TurboRecordCreateDto>> {
        return Consumer { event: Message<TurboRecordCreateDto> ->
            turboRecordConsumer.createSingleTurboRecord(event.payload)
        }
    }

    @EventConsumer(QUEUE_TURBO_REPORT_UPDATE, STREAM_CONSUMER_GROUP)
    fun turboRecordUpdateListener(
        @Autowired turboRecordConsumer: TurboRecordConsumer
    ): Consumer<Message<TurboRecordUpdateDto>> {
        return Consumer { event: Message<TurboRecordUpdateDto> ->
            turboRecordConsumer.updateSingleTurboRecord(event.payload)
        }
    }

    @EventConsumer(QUEUE_TURBO_PLUGIN_DATA, STREAM_CONSUMER_GROUP)
    fun turboPluginUpdateListener(
        @Autowired turboRecordConsumer: TurboRecordConsumer
    ): Consumer<Message<TurboRecordPluginUpdateDto>> {
        return Consumer { event: Message<TurboRecordPluginUpdateDto> ->
            turboRecordConsumer.updateSingleRecordForPlugin(event.payload)
        }
    }
}
