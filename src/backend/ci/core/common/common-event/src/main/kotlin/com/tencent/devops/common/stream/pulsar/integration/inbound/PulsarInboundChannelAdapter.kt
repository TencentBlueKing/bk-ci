/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.devops.common.stream.pulsar.integration.inbound

import com.tencent.devops.common.stream.pulsar.properties.PulsarConsumerProperties
import com.tencent.devops.common.stream.pulsar.properties.PulsarProperties
import com.tencent.devops.common.stream.pulsar.support.PulsarMessageConverterSupport
import com.tencent.devops.common.stream.pulsar.util.PulsarClientUtils
import com.tencent.devops.common.stream.pulsar.util.PulsarTopicUtils
import org.apache.pulsar.client.api.Consumer
import org.apache.pulsar.client.api.Message
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

@Suppress("EmptyClassBlock")
class PulsarInboundChannelAdapter(
    private val destination: String,
    private val group: String? = null,
    private var extendedConsumerProperties: ExtendedConsumerProperties<PulsarConsumerProperties>,
    private val pulsarProperties: PulsarProperties
) : MessageProducerSupport(), OrderlyShutdownCapable {
    private val consumers: MutableList<Consumer<Any>> = mutableListOf()
    var retryTemplate: RetryTemplate? = null
    var recoveryCallback: RecoveryCallback<Any>? = null
    private var topic: String = ""
    override fun onInit() {
        if (extendedConsumerProperties.extension == null) {
            return
        }
        try {
            super.onInit()
            topic = PulsarTopicUtils.generateTopic(
                tenant = pulsarProperties.tenant,
                namespace = pulsarProperties.namespace,
                topic = destination
            )
            // TODO prepare register consumer message listener
            val messageListener = createListener()
            createRetryTemplate()
            if (extendedConsumerProperties.concurrency > 1) {
                val client = PulsarClientUtils.pulsarClient(
                    pulsarProperties = pulsarProperties,
                    concurrency = extendedConsumerProperties.concurrency
                )
                // TODO multi topic如何处理， batch 如何处理， 对于Subscription多种模式如何处理
                for (i in 1..extendedConsumerProperties.concurrency) {
                    val consumer = PulsarConsumerFactory.initPulsarConsumer(
                        topic = topic,
                        group = group,
                        consumerProperties = extendedConsumerProperties,
                        messageListener = messageListener,
                        pulsarProperties = pulsarProperties,
                        pulsarClient = client
                    )
                    consumers.add(consumer)
                }
            } else {
                val consumer = PulsarConsumerFactory.initPulsarConsumer(
                    topic = topic,
                    group = group,
                    consumerProperties = extendedConsumerProperties,
                    messageListener = messageListener,
                    pulsarProperties = pulsarProperties
                )
                consumers.add(consumer)
            }
        } catch (ignore: Exception) {
            logger.error("DefaultPulsarConsumer init failed, Caused by " + ignore.message)
            throw MessagingException(
                MessageBuilder.withPayload(
                    "DefaultPulsarConsumer init failed, Caused by " + ignore.message
                ).build(),
                ignore
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
                        RetryCallback<Any, RuntimeException> { _: RetryContext? ->
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
            } catch (ignore: Exception) {
                logger.warn("Error occurred during consume message ${msg.messageId}: $ignore")
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
        consumers.forEach {
            try {
                it.close()
            } catch (ignore: Exception) {
            }
        }
    }

    override fun beforeShutdown(): Int {
        this.stop()
        return 0
    }

    override fun afterShutdown(): Int {
        return 0
    }

    companion object {
//        private val logger = LoggerFactory.getLogger(PulsarInboundChannelAdapter::class.java)
    }
}
