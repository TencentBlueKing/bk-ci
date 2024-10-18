package com.tencent.devops.common.stream

import com.tencent.devops.common.event.pojo.IEvent
import org.springframework.messaging.Message
import java.util.function.Consumer

object ScsConsumerBuilder {
    fun <T : IEvent> build(action: (a: T) -> Unit): Consumer<Message<T>> {
        return Consumer { t -> action.invoke(t.payload) }
    }
}
