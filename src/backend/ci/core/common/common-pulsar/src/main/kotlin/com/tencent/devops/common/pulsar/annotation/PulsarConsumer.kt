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

package com.tencent.devops.common.pulsar.annotation

import com.tencent.devops.common.pulsar.enum.Serialization
import org.apache.pulsar.client.api.SubscriptionInitialPosition
import org.apache.pulsar.client.api.SubscriptionType
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class PulsarConsumer(

    val topic: String,

    val clazz: KClass<*> = ByteArray::class,

    val serialization: Serialization = Serialization.JSON,
    /**
     * Type of subscription.
     *
     * Shared - This will allow you to have multiple consumers/instances of the application in a cluster with same subscription
     * name and guarantee that the message is read only by one consumer.
     *
     * Exclusive - message will be delivered to every subscription name only once but won't allow to instantiate multiple
     * instances or consumers of the same subscription name. With a default configuration you don't need to worry about horizontal
     * scaling because message will be delivered to each pod in a cluster since in case of exclusive subscription
     * the name is unique per instance and can be nicely used to update state of each pod in case your service
     * is stateful (For example - you need to update in-memory cached configuration for each instance of authorization microservice).
     *
     * By default the type is `Exclusive` but you can also override the default in `application.properties`.
     * This can be handy in case you are using `Shared` subscription in your application all the time and you
     * don't want to override this value every time you use `@PulsarConsumer`.
     */
    val subscriptionType: Array<SubscriptionType> = [],
    /**
     * (Optional) Consumer names are auto-generated but in case you wish to use your custom consumer names,
     * feel free to override it.
     */
    val consumerName: String = "",
    /**
     * (Optional) Subscription names are auto-generated but in case you wish to use your custom subscription names,
     * feel free to override it.
     */
    val subscriptionName: String = "",
    /**
     * Maximum number of times that a message will be redelivered before being sent to the dead letter queue.
     * Note: Currently, dead letter topic is enabled only in the shared subscription mode.
     */
    val maxRedeliverCount: Int = -1,
    /**
     * Name of the dead topic where the failing messages will be sent.
     */
    val deadLetterTopic: String = "",
    /**
     * If value is set to true, the consumer will autostart on application startup automatically.
     * When the value is set to false, consumer will not subscribe to the topic.
     * By default, the value is `true`
     */
    val autoStart: Boolean = true,
    /**
     * Set the namespace, which is set in the configuration file by default.
     * After the setting here, it shall prevail. It is mainly used for multiple namespaces in one project.
     */
    val namespace: String = "",
    /**
     * When creating a consumer, if the subscription does not exist, a new subscription will be created.
     * By default, the subscription will be created at the end of the topic (Latest).
     */
    val initialPosition: SubscriptionInitialPosition = SubscriptionInitialPosition.Latest
)
