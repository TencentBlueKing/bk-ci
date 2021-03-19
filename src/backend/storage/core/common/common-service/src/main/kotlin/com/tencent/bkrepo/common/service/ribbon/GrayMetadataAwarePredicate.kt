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
import com.netflix.loadbalancer.PredicateKey
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import org.springframework.cloud.client.serviceregistry.Registration
import org.springframework.cloud.consul.discovery.ConsulServer

class GrayMetadataAwarePredicate(val properties: RibbonGrayProperties) : AbstractServerPredicate() {
    override fun apply(input: PredicateKey?): Boolean {
        if (input == null) {
            return false
        }
        val registration = SpringContextUtils.getBean(Registration::class.java)
        val localEnvTag = registration.metadata.getOrDefault(ENV, ENV_RELEASE)
        val server = input.server as ConsulServer
        val serverEnvTag = server.metadata.getOrDefault(ENV, ENV_RELEASE)
        return localEnvTag == serverEnvTag
    }

    companion object {
        private const val ENV = "env"
        private const val ENV_RELEASE = "release"
    }
}
