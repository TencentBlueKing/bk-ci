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

package com.tencent.devops.stream.v2.service

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.stream.v2.dao.StreamBasicSettingDao
import org.jooq.DSLContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import javax.ws.rs.core.Response

class TestStreamScmService {

    private val client: Client = mock()
    private val dslContext: DSLContext = mock()
    private val oauthService: StreamOauthService = mock()
    private val streamBasicSettingDao: StreamBasicSettingDao = mock()
    private val streamGitTokenService: StreamGitTokenService = mock()
    private val serviceGitCiResource: ServiceGitCiResource = mock()
    private val streamScmService: StreamScmService = StreamScmService(client, dslContext, oauthService, streamBasicSettingDao, streamGitTokenService)

    @Before
    fun init() {
        Mockito.`when`(client.getScm(ServiceGitCiResource::class)).thenReturn(serviceGitCiResource)
        Mockito.`when`(
            serviceGitCiResource.getGitCIFileContent("1", "1", "1", "1", true)
        )
            .thenThrow(CustomException(Response.Status.FORBIDDEN, "403 无权限"))
            .thenReturn(Result("success"))
        Mockito.`when`(streamGitTokenService.getToken(1)).thenReturn("1");
    }

    @Test
    fun testGetYaml403Retry() {
        val yamlFromGit = streamScmService.getYamlFromGit("1", "1", "1", "1", true)
        Mockito.verify(serviceGitCiResource, times(2)).getGitCIFileContent("1", "1", "1", "1", true)
        Mockito.verify(streamGitTokenService).getToken(1)
        assertEquals(yamlFromGit, "success")
    }

}
