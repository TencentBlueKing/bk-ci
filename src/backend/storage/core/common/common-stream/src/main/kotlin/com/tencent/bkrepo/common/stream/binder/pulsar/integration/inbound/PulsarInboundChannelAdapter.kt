/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.stream.binder.pulsar.integration.inbound

import com.tencent.bkrepo.common.stream.binder.pulsar.properties.PulsarBinderConfigurationProperties
import com.tencent.bkrepo.common.stream.binder.pulsar.properties.PulsarConsumerProperties
import com.tencent.bkrepo.common.stream.binder.pulsar.support.PulsarMessageConverterSupport
import com.tencent.bkrepo.common.stream.binder.pulsar.util.PulsarUtils
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties
import org.springframework.integration.context.OrderlyShutdownCapable
import org.springframework.integration.endpoint.MessageProducerSupport
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessagingException
import org.springframework.retry.RecoveryCallback
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.support.RetryTemplate
import org.springframework.util.Assert

class PulsarInboundChannelAdapter(
    private val destination: String,
    private val pulsarClient: PulsarClient,
    private val group: String? = null,
    private var extendedConsumerProperties: ExtendedConsumerProperties<PulsarConsumerProperties>,
    private val pulsarProperties: PulsarBinderConfigurationProperties
) : MessageProducerSupport(), OrderlyShutdownCapable {
    var consumer: Consumer<Any>? = null
    var retryTemplate: RetryTemplate? = null
    var recoveryCallback: RecoveryCallback<Any>? = null
    private var topic: String = ""
    override fun onInit() {
        if (extendedConsumerProperties.extension == null) {
            return
        }
        try {
            super.onInit()
            topic = PulsarUtils.generateTopic(
                tenant = pulsarProperties.tenant,
                namespace = pulsarProperties.namespace,
                topic = destination
            )
            val (deadLetter, retryLetter) = PulsarUtils.generateDeadLetterTopics(
                tenant = pulsarProperties.tenant,
                namespace = pulsarProperties.namespace,
                group = group,
                subscriptionName = extendedConsumerProperties.extension.subscriptionName,
                deadLetterTopic = extendedConsumerProperties.extension.deadLetterTopic,
                retryLetterTopic = extendedConsumerProperties.extension.retryLetterTopic
            )

            createRetryTemplate()
            // TODO prepare register consumer message listener
            val messageListener = createListener()
            // TODO multi topic如何处理， batch 如何处理， 对于Subscription多种模式如何处理
            consumer = PulsarConsumerFactory.initPulsarConsumer(
                topic = topic,
                group = group,
                consumerProperties = extendedConsumerProperties.extension,
                pulsarClient = pulsarClient,
                messageListener = messageListener,
                deadLetterTopic = deadLetter,
                retryLetterTopic = retryLetter
            )
        } catch (e: Exception) {
            logger.error("DefaultPulsarConsumer init failed, Caused by " + e.message)
            throw MessagingException(
                MessageBuilder.withPayload(
                    "DefaultPulsarConsumer init failed, Caused by " + e.message
                ).build(),
                e
            )
        }
    }

    private fun createRetryTemplate() {
        retryTemplate?.let {
            Assert.state(
                errorChannel == null,
                "Cannot have an 'errorChannel' property when a 'RetryTemplate' is " +
                    "provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to " +
                    "send an error message when retries are exhausted"
            )
            retryTemplate!!.registerListener(object : RetryListener {
                override fun <T, E : Throwable?> open(
                    context: RetryContext,
                    callback: RetryCallback<T, E>
                ): Boolean {
                    return true
                }

                override fun <T, E : Throwable?> close(
                    context: RetryContext,
                    callback: RetryCallback<T, E>,
                    throwable: Throwable?
                ) = Unit

                override fun <T, E : Throwable?> onError(
                    context: RetryContext,
                    callback: RetryCallback<T, E>,
                    throwable: Throwable?
                ) = Unit
            })
        }
    }

    private fun createListener(): (Consumer<*>, Message<*>) -> Unit {
        return { it: Consumer<*>, msg: Message<*> ->
            try {
                if (logger.isDebugEnabled) {
                    logger.debug("Message received $msg")
                }
                val message = PulsarMessageConverterSupport.convertMessage2Spring(msg)
                if (retryTemplate != null) {
                    retryTemplate!!.execute(
                        RetryCallback<Any, RuntimeException> { context: RetryContext? ->
                            sendMessage(message)
                            if (logger.isDebugEnabled) {
                                logger.info("will send acknowledge: ${msg.messageId}")
                            }
                            it.acknowledge(msg)
                            message
                        },
                        recoveryCallback
                    )
                } else {
                    sendMessage(message)
                    if (logger.isDebugEnabled) {
                        logger.info("will send acknowledge: ${msg.messageId}")
                    }
                    it.acknowledge(msg)
                }
                if (logger.isDebugEnabled) {
                    logger.info("Message ${msg.messageId} has been consumed")
                }
            } catch (e: Exception) {
                logger.warn("Error occurred during consume message ${msg.messageId}: $e")
                it.negativeAcknowledge(msg)
            }
        }
    }

    override fun doStart() {
        if (extendedConsumerProperties.extension == null) {
            return
        }
//        val instrumentation = Instrumentation(topic, this)
//        try {
//            instrumentation.markStartedSuccessfully()
//        } catch (e: java.lang.Exception) {
//            instrumentation.markStartFailed(e)
//            logger.error("PulsarConsumer init failed, Caused by " + e.message)
//            throw MessagingException(
//                MessageBuilder.withPayload(
//                    "PulsarConsumer init failed, Caused by " + e.message
//                )
//                    .build(),
//                e
//            )
//        } finally {
//            InstrumentationManager.addHealthInstrumentation(instrumentation)
//        }
    }

    override fun doStop() {
        // 2.8.1版本调用会出错，所以升级https://github.com/apache/pulsar/issues/12024
        consumer?.close()
    }

    override fun beforeShutdown(): Int {
        this.stop()
        return 0
    }

    override fun afterShutdown(): Int {
        return 0
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PulsarInboundChannelAdapter::class.java)
    }
}
