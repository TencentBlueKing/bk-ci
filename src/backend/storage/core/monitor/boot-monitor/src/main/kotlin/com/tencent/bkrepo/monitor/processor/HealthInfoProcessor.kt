/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.monitor.processor

import com.tencent.bkrepo.monitor.metrics.HealthInfo
import de.codecentric.boot.admin.server.domain.entities.Instance
import de.codecentric.boot.admin.server.domain.events.InstanceEvent
import de.codecentric.boot.admin.server.domain.events.InstanceRegisteredEvent
import de.codecentric.boot.admin.server.domain.events.InstanceRegistrationUpdatedEvent
import de.codecentric.boot.admin.server.domain.events.InstanceStatusChangedEvent
import de.codecentric.boot.admin.server.services.AbstractEventHandler
import de.codecentric.boot.admin.server.services.InstanceRegistry
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class HealthInfoProcessor(
    private val instanceRegistry: InstanceRegistry,
    publisher: Publisher<InstanceEvent>
) : AbstractEventHandler<InstanceEvent>(publisher, InstanceEvent::class.java) {

    private val processor = EmitterProcessor.create<HealthInfo>()

    fun getFlux(): Flux<HealthInfo> {
        val currentFlux = instanceRegistry.instances
            .filter { it.isRegistered }
            .concatMap { convert(it) }
        return Flux.merge(currentFlux, processor)
    }

    override fun handle(publisher: Flux<InstanceEvent>): Publisher<Void> {
        return publisher.filter {
            it is InstanceRegisteredEvent || it is InstanceRegistrationUpdatedEvent || it is InstanceStatusChangedEvent
        }.flatMap { event ->
            instanceRegistry.getInstance(event.instance).map { instance ->
                convert(instance).map { processor.onNext(it) }
            }
            Mono.empty<Void>()
        }
    }

    companion object {
        private fun convert(instance: Instance): Flux<HealthInfo> {
            return Flux.fromIterable(instance.statusInfo.details.entries).map {
                HealthInfo(
                    name = it.key,
                    status = it.value,
                    application = instance.registration.name,
                    instance = instance.id.value
                )
            }
        }
    }
}
