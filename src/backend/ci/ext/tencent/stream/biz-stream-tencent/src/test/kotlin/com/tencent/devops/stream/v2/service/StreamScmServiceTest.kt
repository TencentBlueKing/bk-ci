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

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.scm.api.ServiceGitCiResource
import com.tencent.devops.scm.pojo.GitCIProjectInfo
import com.tencent.devops.stream.service.StreamGitTokenService
import com.tencent.devops.stream.service.StreamScmService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.ws.rs.core.Response

@Suppress("ALL")
class StreamScmServiceTest {

    private val client: Client = mockk()
    private val streamGitTokenService: StreamGitTokenService = mockk()
    private val serviceGitCiResource: ServiceGitCiResource = mockk()
    private val streamScmService: StreamScmService =
        StreamScmService(client, mockk(), mockk(), mockk(), streamGitTokenService)

    @BeforeEach
    fun init() {
        every { client.getScm(ServiceGitCiResource::class) } returns (serviceGitCiResource)
        every { streamGitTokenService.getToken(1, false) } returns ("1")
        every { streamGitTokenService.getToken(1, true) } returns ("1")
    }

    @Test
    fun testGetYaml403RetrySuccess() {
        every {
            serviceGitCiResource.getGitCIFileContent("1", "1", "1", "1", true)
        } throws (CustomException(Response.Status.FORBIDDEN, "403 无权限")) andThen Result("success")
        val yamlFromGit = streamScmService.getYamlFromGit("1", "1", "1", "1", true)
        verify(exactly = 2) { serviceGitCiResource.getGitCIFileContent("1", "1", "1", "1", true) }
        verify { streamGitTokenService.getToken(1, true) }
        assertEquals(yamlFromGit, "success")
    }

    @Test
    fun testGetYaml403RetryFail() {
        every {
            serviceGitCiResource.getGitCIFileContent("1", "1", "1", "1", true)
        } throws (CustomException(Response.Status.FORBIDDEN, "403 无权限"))
        var flag = false
        try {
            streamScmService.getYamlFromGit("1", "1", "1", "1", true)
        } catch (e: Exception) {
            flag = true
        }
        verify(exactly = 2) { serviceGitCiResource.getGitCIFileContent("1", "1", "1", "1", true) }
        verify { streamGitTokenService.getToken(1, true) }
        assertTrue(flag)
    }

    @Test
    fun testGetProjectInfSuccess() {
        val gitCIProjectInfoMock = GitCIProjectInfo(222, "1", "1", "1", "1", "1", "1", "1", "1", "1", "1")
        every {
            serviceGitCiResource.getProjectInfo("1", "1", true)
        } throws (CustomException(Response.Status.FORBIDDEN, "403 无权限")) andThen (Result(gitCIProjectInfoMock))
        val gitCIProjectInfo = streamScmService.getProjectInfo("1", "1", true)
        verify(exactly = 2) { serviceGitCiResource.getProjectInfo("1", "1", true) }
        verify { streamGitTokenService.getToken(1, true) }
        assertEquals(gitCIProjectInfoMock.gitProjectId, gitCIProjectInfo?.gitProjectId ?: 111)
    }

    @Test
    fun testGetProjectInfFail() {
        every {
            serviceGitCiResource.getProjectInfo("1", "1", true)
        } throws (CustomException(
            Response.Status.FORBIDDEN,
            "403 无权限"
        )) andThenThrows (CustomException(Response.Status.FORBIDDEN, "403 无权限"))
        var flag = false
        try {
            streamScmService.getProjectInfo("1", "1", true)
        } catch (e: Exception) {
            flag = true
        }
        verify(exactly = 2) { serviceGitCiResource.getProjectInfo("1", "1", true) }
        verify { streamGitTokenService.getToken(1, true) }
        assertTrue(flag)
    }
//    @Test
//    fun testGetTokenNotCache() {
//        val redisMock: RedisOperation = mockk()
//        val scmMock: StreamScmService = mockk()
//        val streamGitTokenService = StreamGitTokenService(scmMock, redisMock)
//        Mockito.`when`(redisMock.get(getGitTokenKey(1))).thenReturn("1")
//        Mockito.`when`(scmMock.getToken("1")).thenReturn(GitToken("1"))
//        Mockito.verify(scmMock).getToken("1")
//        streamGitTokenService.getToken(1, true)
//    }
//
//    private val STREAM_GIT_TOKEN_PROJECT_PREFIX = "stream:git:project:token:"
//    fun getGitTokenKey(gitProjectId: Long) = STREAM_GIT_TOKEN_PROJECT_PREFIX + gitProjectId
}
