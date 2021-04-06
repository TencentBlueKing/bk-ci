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

package com.tencent.bkrepo.docker.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_NAME
import com.tencent.bkrepo.docker.constant.DOCKER_PROJECT_ID
import com.tencent.bkrepo.docker.constant.DOCKER_REPO_ADDR
import com.tencent.bkrepo.docker.constant.DOCKER_REPO_NAME
import com.tencent.bkrepo.docker.constant.DOCKER_TAG
import com.tencent.bkrepo.docker.constant.DOCKER_USER_DELETE_IMAGE_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_LAYER_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_MANIFEST_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_REPO_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_REPO_TAG_DETAIL_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_REPO_TAG_SUFFIX
import com.tencent.bkrepo.docker.constant.DOCKER_USER_TAG_SUFFIX
import com.tencent.bkrepo.docker.constant.PAGE_NUMBER
import com.tencent.bkrepo.docker.constant.PAGE_SIZE
import com.tencent.bkrepo.docker.constant.USER_API_PREFIX
import com.tencent.bkrepo.docker.pojo.DockerImageResult
import com.tencent.bkrepo.docker.pojo.DockerTagDetail
import com.tencent.bkrepo.docker.pojo.DockerTagResult
import com.tencent.bkrepo.docker.response.DockerResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpServletRequest

/**
 * docker image extension api
 */
@Api("docker镜像仓库扩展查询api")
@RequestMapping(USER_API_PREFIX)
interface User {

    @ApiOperation("获取manifest文件")
    @GetMapping(DOCKER_USER_MANIFEST_SUFFIX)
    fun getManifest(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = DOCKER_TAG, required = true)
        tag: String
    ): Response<String>

    @ApiOperation("获取layer文件")
    @GetMapping(DOCKER_USER_LAYER_SUFFIX)
    fun getLayer(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @PathVariable
        @ApiParam(value = "id", required = true)
        id: String
    ): DockerResponse

    @ApiOperation("获取所有image")
    @GetMapping(DOCKER_USER_REPO_SUFFIX)
    fun getRepo(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @RequestParam(required = true)
        @ApiParam(value = PAGE_NUMBER, required = true)
        pageNumber: Int,
        @RequestParam(required = true)
        @ApiParam(value = PAGE_SIZE, required = true)
        pageSize: Int,
        @RequestParam(required = false)
        @ApiParam(value = DOCKER_NODE_NAME, required = true)
        name: String?
    ): Response<DockerImageResult>

    @ApiOperation("获取repo所有的tag")
    @GetMapping(DOCKER_USER_TAG_SUFFIX)
    fun getRepoTag(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @ApiParam(value = PAGE_NUMBER, required = true)
        pageNumber: Int,
        @RequestParam(required = true)
        @ApiParam(value = PAGE_SIZE, required = true)
        pageSize: Int,
        @RequestParam(required = false)
        @ApiParam(value = DOCKER_TAG, required = true)
        tag: String?
    ): Response<DockerTagResult>

    @ApiOperation("删除repo下的指定镜像")
    @DeleteMapping(DOCKER_USER_DELETE_IMAGE_SUFFIX)
    fun deleteRepo(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @RequestParam(required = true)
        @ApiParam(value = "packageKey", required = true)
        packageKey: String
    ): Response<Boolean>

    @ApiOperation("删除repo下的指定镜像")
    @DeleteMapping(DOCKER_USER_REPO_TAG_SUFFIX)
    fun deleteRepoTag(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @RequestParam(required = true)
        @ApiParam(value = "packageKey", required = true)
        packageKey: String,
        @RequestParam(required = true)
        @ApiParam(value = "version", required = true)
        version: String
    ): Response<Boolean>

    @ApiOperation("获取镜像tag下的详情")
    @GetMapping(DOCKER_USER_REPO_TAG_DETAIL_SUFFIX)
    fun getRepoTagDetail(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?,
        @PathVariable
        @ApiParam(value = DOCKER_PROJECT_ID, required = true)
        projectId: String,
        @PathVariable
        @ApiParam(value = DOCKER_REPO_NAME, required = true)
        repoName: String,
        @RequestParam(required = true)
        @ApiParam(value = "packageKey", required = true)
        packageKey: String,
        @RequestParam(required = true)
        @ApiParam(value = "version", required = true)
        version: String
    ): Response<DockerTagDetail?>

    @ApiOperation("获取docker仓库地址")
    @GetMapping(DOCKER_REPO_ADDR)
    fun getDockerRepoAddr(
        request: HttpServletRequest,
        @RequestAttribute
        userId: String?
    ): Response<String?>
}
