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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.web.mq.property

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.util.ArrayList


@ConfigurationProperties(prefix = "spring.rabbitmq")
class RabbitMQProperties {
    /**
     * RabbitMQ host.
     */
    var host = "localhost"

    /**
     * RabbitMQ port.
     */
    var port = 5672

    /**
     * Login user to authenticate to the broker.
     */
    var username: String? = null

    /**
     * Login to authenticate against the broker.
     */
    var password: String? = null

    /**
     * SSL configuration.
     */
    val ssl = Ssl()

    /**
     * Virtual host to use when connecting to the broker.
     */
    var virtualHost: String? = null
        private set

    /**
     * Comma-separated list of addresses to which the client should connect.
     */
    var addresses: String? = null
        set(addresses) {
            field = addresses
            parsedAddresses = parseAddresses(addresses)
        }

    /**
     * Requested heartbeat timeout, in seconds; zero for none.
     */
    var requestedHeartbeat: Int? = null

    /**
     * Enable publisher confirms.
     */
    var isPublisherConfirms = false

    /**
     * Enable publisher returns.
     */
    var isPublisherReturns = false

    /**
     * Connection timeout, in milliseconds; zero for infinite.
     */
    var connectionTimeout: Int? = null

    /**
     * Cache configuration.
     */
    val cache = Cache()

    /**
     * Listener container configuration.
     */
    val listener = Listener()
    val template = Template()
    private var parsedAddresses: List<Address?>? = null

    /**
     * Returns the host from the first address, or the configured host if no addresses
     * have been set.
     * @return the host
     * @see .setAddresses
     * @see .getHost
     */
    fun determineHost(): String? {
        return if (CollectionUtils.isEmpty(parsedAddresses)) {
            host
        } else parsedAddresses!![0]!!.host
    }

    /**
     * Returns the port from the first address, or the configured port if no addresses
     * have been set.
     * @return the port
     * @see .setAddresses
     * @see .getPort
     */
    fun determinePort(): Int {
        if (CollectionUtils.isEmpty(parsedAddresses)) {
            return port
        }
        val address = parsedAddresses!![0]
        return address!!.port
    }

    /**
     * Returns the comma-separated addresses or a single address (`host:port`)
     * created from the configured host and port if no addresses have been set.
     * @return the addresses
     */
    fun determineAddresses(): String {
        if (CollectionUtils.isEmpty(parsedAddresses)) {
            return host + ":" + port
        }
        val addressStrings: MutableList<String?> = ArrayList()
        for (parsedAddress in parsedAddresses!!) {
            addressStrings.add(parsedAddress!!.host + ":" + parsedAddress.port)
        }
        return StringUtils.collectionToCommaDelimitedString(addressStrings)
    }

    private fun parseAddresses(addresses: String?): List<Address?> {
        val parsedAddresses: MutableList<Address?> = ArrayList()
        for (address in StringUtils.commaDelimitedListToStringArray(addresses)) {
            parsedAddresses.add(Address(address))
        }
        return parsedAddresses
    }

    /**
     * If addresses have been set and the first address has a username it is returned.
     * Otherwise returns the result of calling `getUsername()`.
     * @return the username
     * @see .setAddresses
     * @see .getUsername
     */
    fun determineUsername(): String? {
        if (CollectionUtils.isEmpty(parsedAddresses)) {
            return username
        }
        val address = parsedAddresses!![0]
        return if (address!!.username == null) username else address.username
    }

    /**
     * If addresses have been set and the first address has a password it is returned.
     * Otherwise returns the result of calling `getPassword()`.
     * @return the password or `null`
     * @see .setAddresses
     * @see .getPassword
     */
    fun determinePassword(): String? {
        if (CollectionUtils.isEmpty(parsedAddresses)) {
            return password
        }
        val address = parsedAddresses!![0]
        return if (address!!.password == null) password else address.password
    }

    /**
     * If addresses have been set and the first address has a virtual host it is returned.
     * Otherwise returns the result of calling `getVirtualHost()`.
     * @return the virtual host or `null`
     * @see .setAddresses
     * @see .getVirtualHost
     */
    fun determineVirtualHost(): String? {
        if (CollectionUtils.isEmpty(parsedAddresses)) {
            return virtualHost
        }
        val address = parsedAddresses!![0]
        return if (address!!.virtualHost == null) virtualHost else address.virtualHost
    }

    fun setVirtualHost(virtualHost: String) {
        this.virtualHost = if ("" == virtualHost) "/" else virtualHost
    }

    class Ssl {
        /**
         * Enable SSL support.
         */
        var isEnabled = false

        /**
         * Path to the key store that holds the SSL certificate.
         */
        var keyStore: String? = null

        /**
         * Password used to access the key store.
         */
        var keyStorePassword: String? = null

        /**
         * Trust store that holds SSL certificates.
         */
        var trustStore: String? = null

        /**
         * Password used to access the trust store.
         */
        var trustStorePassword: String? = null

        /**
         * SSL algorithm to use (e.g. TLSv1.1). Default is set automatically by the rabbit
         * client library.
         */
        var algorithm: String? = null

    }

    class Cache {
        val channel = Cache.Channel()
        val connection = Cache.Connection()

        class Channel {
            /**
             * Number of channels to retain in the cache. When "check-timeout" > 0, max
             * channels per connection.
             */
            var size: Int? = null

            /**
             * Number of milliseconds to wait to obtain a channel if the cache size has
             * been reached. If 0, always create a new channel.
             */
            var checkoutTimeout: Long? = null

        }

        class Connection {
            /**
             * Connection factory cache mode.
             */
            var mode = CachingConnectionFactory.CacheMode.CHANNEL

            /**
             * Number of connections to cache. Only applies when mode is CONNECTION.
             */
            var size: Int? = null

        }
    }

    class Listener {
        @NestedConfigurationProperty
        val simple = AmqpContainer()

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.auto-startup")
        @set:Deprecated("")
        var isAutoStartup: Boolean
            get() = simple.isAutoStartup
            set(autoStartup) {
                simple.isAutoStartup = autoStartup
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.acknowledge-mode")
        @set:Deprecated("")
        var acknowledgeMode: AcknowledgeMode?
            get() = simple.acknowledgeMode
            set(acknowledgeMode) {
                simple.acknowledgeMode = acknowledgeMode
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.concurrency")
        @set:Deprecated("")
        var concurrency: Int?
            get() = simple.concurrency
            set(concurrency) {
                simple.concurrency = concurrency
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.max-concurrency")
        @set:Deprecated("")
        var maxConcurrency: Int?
            get() = simple.maxConcurrency
            set(maxConcurrency) {
                simple.maxConcurrency = maxConcurrency
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.prefetch")
        @set:Deprecated("")
        var prefetch: Int?
            get() = simple.prefetch
            set(prefetch) {
                simple.prefetch = prefetch
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.transaction-size")
        @set:Deprecated("")
        var transactionSize: Int?
            get() = simple.transactionSize
            set(transactionSize) {
                simple.transactionSize = transactionSize
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.default-requeue-rejected")
        @set:Deprecated("")
        var defaultRequeueRejected: Boolean?
            get() = simple.defaultRequeueRejected
            set(defaultRequeueRejected) {
                simple.defaultRequeueRejected = defaultRequeueRejected
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.idle-event-interval")
        @set:Deprecated("")
        var idleEventInterval: Long?
            get() = simple.idleEventInterval
            set(idleEventInterval) {
                simple.idleEventInterval = idleEventInterval
            }

        @get:Deprecated("")
        @get:DeprecatedConfigurationProperty(replacement = "spring.rabbitmq.listener.simple.retry")
        val retry: ListenerRetry
            get() = simple.retry

    }

    class AmqpContainer {
        /**
         * Start the container automatically on startup.
         */
        var isAutoStartup = true

        /**
         * Acknowledge mode of container.
         */
        var acknowledgeMode: AcknowledgeMode? = null

        /**
         * Minimum number of consumers.
         */
        var concurrency: Int? = null

        /**
         * Maximum number of consumers.
         */
        var maxConcurrency: Int? = null

        /**
         * Number of messages to be handled in a single request. It should be greater than
         * or equal to the transaction size (if used).
         */
        var prefetch: Int? = null

        /**
         * Number of messages to be processed in a transaction. For best results it should
         * be less than or equal to the prefetch count.
         */
        var transactionSize: Int? = null

        /**
         * Whether rejected deliveries are requeued by default; default true.
         */
        var defaultRequeueRejected: Boolean? = null

        /**
         * How often idle container events should be published in milliseconds.
         */
        var idleEventInterval: Long? = null

        /**
         * Optional properties for a retry interceptor.
         */
        @NestedConfigurationProperty
        val retry = ListenerRetry()

    }

    class Template {
        @NestedConfigurationProperty
        val retry = Retry()

        /**
         * Enable mandatory messages. If a mandatory message cannot be routed to a queue
         * by the server, it will return an unroutable message with a Return method.
         */
        var mandatory: Boolean? = null

        /**
         * Timeout for receive() operations.
         */
        var receiveTimeout: Long? = null

        /**
         * Timeout for sendAndReceive() operations.
         */
        var replyTimeout: Long? = null

    }

    open class Retry {
        /**
         * Whether or not publishing retries are enabled.
         */
        var isEnabled = false

        /**
         * Maximum number of attempts to publish or deliver a message.
         */
        var maxAttempts = 3

        /**
         * Interval between the first and second attempt to publish or deliver a message.
         */
        var initialInterval = 1000L

        /**
         * A multiplier to apply to the previous retry interval.
         */
        var multiplier = 1.0

        /**
         * Maximum interval between attempts.
         */
        var maxInterval = 10000L

    }

    class ListenerRetry : Retry() {
        /**
         * Whether or not retries are stateless or stateful.
         */
        var isStateless = true

    }

    private class Address constructor(input: String) {
        var host: String? = null
        var port = 0
        var username: String? = null
        var password: String? = null
        var virtualHost: String? = null
        private fun trimPrefix(input: String): String {
            var input = input
            if (input.startsWith(PREFIX_AMQP)) {
                input = input.substring(PREFIX_AMQP.length)
            }
            return input
        }

        private fun parseUsernameAndPassword(input: String): String {
            var input = input
            if (input.contains("@")) {
                var split = StringUtils.split(input, "@")
                val creds = split[0]
                input = split[1]
                split = StringUtils.split(creds, ":")
                username = split[0]
                if (split.size > 0) {
                    password = split[1]
                }
            }
            return input
        }

        private fun parseVirtualHost(input: String): String {
            var input = input
            val hostIndex = input.indexOf("/")
            if (hostIndex >= 0) {
                virtualHost = input.substring(hostIndex + 1)
                if (virtualHost!!.isEmpty()) {
                    virtualHost = "/"
                }
                input = input.substring(0, hostIndex)
            }
            return input
        }

        private fun parseHostAndPort(input: String) {
            val portIndex = input.indexOf(':')
            if (portIndex == -1) {
                host = input
                port = Address.Companion.DEFAULT_PORT
            } else {
                host = input.substring(0, portIndex)
                port = Integer.valueOf(input.substring(portIndex + 1))
            }
        }

        companion object {
            private const val PREFIX_AMQP = "amqp://"
            private const val DEFAULT_PORT = 5672
        }

        init {
            var input = input
            input = input.trim { it <= ' ' }
            input = trimPrefix(input)
            input = parseUsernameAndPassword(input)
            input = parseVirtualHost(input)
            parseHostAndPort(input)
        }
    }
}