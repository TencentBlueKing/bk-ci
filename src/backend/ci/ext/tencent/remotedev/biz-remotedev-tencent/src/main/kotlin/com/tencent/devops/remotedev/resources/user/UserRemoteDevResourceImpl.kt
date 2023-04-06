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

package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevResource
import com.tencent.devops.remotedev.pojo.BKGPT
import com.tencent.devops.remotedev.pojo.RemoteDevSettings
import com.tencent.devops.remotedev.service.BKGPTService
import com.tencent.devops.remotedev.service.RemoteDevSettingService
import org.glassfish.jersey.server.ChunkedOutput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Executors
import javax.ws.rs.core.HttpHeaders

@RestResource
@Suppress("ALL")
class UserRemoteDevResourceImpl @Autowired constructor(
    val remoteDevSettingService: RemoteDevSettingService,
    val bkgptService: BKGPTService
) : UserRemoteDevResource {

    companion object {
        private val logger = LoggerFactory.getLogger(UserRemoteDevResourceImpl::class.java)
    }

    private val executor = Executors.newCachedThreadPool()
    private val SEPARATOR = System.getProperty("line.separator")
    override fun getRemoteDevSettings(userId: String): Result<RemoteDevSettings> {
        return Result(remoteDevSettingService.getRemoteDevSettings(userId))
    }

    override fun updateRemoteDevSettings(userId: String, remoteDevSettings: RemoteDevSettings): Result<Boolean> {
        return Result(remoteDevSettingService.updateRemoteDevSettings(userId, remoteDevSettings))
    }

    override fun bkGPT(
        userId: String,
        bkTicket: String,
        headers: HttpHeaders,
        data: BKGPT
    ): ChunkedOutput<String> {
        val output: ChunkedOutput<String> = ChunkedOutput<String>(String::class.java, SEPARATOR)
        executor.execute {
            try {
                output.use { out ->
                    bkgptService.streamCompletions(data, bkTicket, out)
                }
            } catch (ex: Exception) {
                logger.warn("Chunked output error!!!!!!")
            }
        }
        return output
    }
}
