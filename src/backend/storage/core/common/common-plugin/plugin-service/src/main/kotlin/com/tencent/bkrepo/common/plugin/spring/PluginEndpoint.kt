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

package com.tencent.bkrepo.common.plugin.spring

import com.tencent.bkrepo.common.plugin.api.PluginInfo
import com.tencent.bkrepo.common.plugin.core.DefaultPluginManager
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse.STATUS_INTERNAL_SERVER_ERROR

@Endpoint(id = "plugin")
class PluginEndpoint(
    private val defaultPluginManager: DefaultPluginManager
) {

    @ReadOperation
    fun list(): Map<String, PluginInfo> {
        return defaultPluginManager.getPluginMap()
    }

    @WriteOperation
    fun load(): WebEndpointResponse<String> {
        return try {
            defaultPluginManager.load()
            WebEndpointResponse("ok")
        } catch (ignored: Exception) {
            WebEndpointResponse(ignored.message.orEmpty(), STATUS_INTERNAL_SERVER_ERROR)
        }
    }

    @WriteOperation
    fun load(@Selector id: String): WebEndpointResponse<String> {
        return try {
            defaultPluginManager.load(id)
            WebEndpointResponse("ok")
        } catch (ignored: Exception) {
            WebEndpointResponse(ignored.message.orEmpty(), STATUS_INTERNAL_SERVER_ERROR)
        }
    }

    @DeleteOperation
    fun unload(@Selector id: String): WebEndpointResponse<String> {
        return try {
            defaultPluginManager.unload(id)
            WebEndpointResponse("ok")
        } catch (ignored: Exception) {
            WebEndpointResponse(ignored.message.orEmpty(), STATUS_INTERNAL_SERVER_ERROR)
        }
    }
}
