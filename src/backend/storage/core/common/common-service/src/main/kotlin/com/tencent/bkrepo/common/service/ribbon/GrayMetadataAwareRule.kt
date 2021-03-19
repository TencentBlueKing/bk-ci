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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.service.ribbon

import com.netflix.loadbalancer.AbstractServerPredicate
import com.netflix.loadbalancer.AvailabilityPredicate
import com.netflix.loadbalancer.CompositePredicate
import com.netflix.loadbalancer.PredicateBasedRule
import com.netflix.loadbalancer.Server
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import org.springframework.cloud.client.serviceregistry.Registration

class GrayMetadataAwareRule : PredicateBasedRule() {

    private val properties: RibbonGrayProperties = SpringContextUtils.getBean(RibbonGrayProperties::class.java)

    private val predicate: AbstractServerPredicate

    private val localHost: String = SpringContextUtils.getBean(Registration::class.java).host

    init {
        val metadataAwarePredicate = GrayMetadataAwarePredicate(properties)
        val availabilityPredicate = AvailabilityPredicate(this, null)
        predicate = CompositePredicate.withPredicates(metadataAwarePredicate, availabilityPredicate)
            .addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
            .build()
    }

    override fun getPredicate() = predicate

    override fun choose(key: Any?): Server? {
        val serverList = filterServers(loadBalancer.allServers)
        return predicate.chooseRoundRobinAfterFiltering(serverList, key).orNull()
    }

    private fun filterServers(serverList: List<Server>): List<Server> {
        if (!properties.localPrior) {
            return serverList
        }
        for (server in serverList) {
            if (server.host == localHost) {
                return listOf(server)
            }
        }
        return serverList
    }

    private fun createCompositePredicate(vararg predicate: AbstractServerPredicate): CompositePredicate {
        return CompositePredicate.withPredicates(*predicate)
            .addFallbackPredicate(AbstractServerPredicate.alwaysTrue())
            .build()
    }
}
