/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AtomWhitelistConfigService @Autowired constructor(
    private val businessConfigService: BusinessConfigService
) {
    private val logger = LoggerFactory.getLogger(AtomWhitelistConfigService::class.java)
    private val objectMapper = ObjectMapper()

    fun isAtomInWhitelist(atomCode: String, whitelistType: String): Boolean {
        return try {
            val configValue = businessConfigService.getConfigValue(
                business = "ATOM",
                feature = "ATOM_WHITELIST",
                businessValue = whitelistType
            )
            val atomCodes = if (configValue != null) {
                objectMapper.readValue(configValue, object : TypeReference<List<String>>() {})
            } else {
                emptyList()
            }
            atomCodes.contains(atomCode)
        } catch (ignored: Throwable) {
            logger.warn("isAtomInWhitelist failed|atomCode=$atomCode|whitelistType=$whitelistType", ignored)
            true // fail-open: 异常时放行
        }
    }
}
