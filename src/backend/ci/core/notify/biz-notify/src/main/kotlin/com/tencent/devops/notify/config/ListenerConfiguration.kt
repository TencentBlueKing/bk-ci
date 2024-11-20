/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.notify.config

import com.tencent.devops.common.event.annotation.EventConsumer
import com.tencent.devops.common.stream.ScsConsumerBuilder
import com.tencent.devops.notify.consumer.NotifyMessageConsumer
import com.tencent.devops.notify.model.EmailNotifyMessageWithOperation
import com.tencent.devops.notify.model.RtxNotifyMessageWithOperation
import com.tencent.devops.notify.model.SmsNotifyMessageWithOperation
import com.tencent.devops.notify.model.VoiceNotifyMessageWithOperation
import com.tencent.devops.notify.model.WechatNotifyMessageWithOperation
import com.tencent.devops.notify.model.WeworkNotifyMessageWithOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration

@Configuration
class ListenerConfiguration {
    @EventConsumer
    fun rtxNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<RtxNotifyMessageWithOperation> { listener.onReceiveRtxMessage(it) }

    @EventConsumer
    fun emailNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<EmailNotifyMessageWithOperation> { listener.onReceiveEmailMessage(it) }

    @EventConsumer
    fun smsNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<SmsNotifyMessageWithOperation> { listener.onReceiveSmsMessage(it) }

    @EventConsumer
    fun wechatNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<WechatNotifyMessageWithOperation> { listener.onReceiveWechatMessage(it) }

    @EventConsumer
    fun weworkNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<WeworkNotifyMessageWithOperation> { listener.onReceiveWeworkMessage(it) }

    @EventConsumer
    fun voiceNotifyConsumer(
        @Autowired listener: NotifyMessageConsumer
    ) = ScsConsumerBuilder.build<VoiceNotifyMessageWithOperation> { listener.onReceiveVoiceMessage(it) }
}
