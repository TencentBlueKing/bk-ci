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

package com.tencent.bkrepo.dockerapi.controller

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.dockerapi.client.BkAuthClient
import com.tencent.bkrepo.dockerapi.pojo.DockerRepo
import com.tencent.bkrepo.dockerapi.pojo.DockerTag
import com.tencent.bkrepo.dockerapi.pojo.QueryImageTagRequest
import com.tencent.bkrepo.dockerapi.pojo.QueryProjectImageRequest
import com.tencent.bkrepo.dockerapi.pojo.QueryPublicImageRequest
import com.tencent.bkrepo.dockerapi.service.ImageService
import com.tencent.bkrepo.dockerapi.util.JwtUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Api("镜像接口")
@RestController
@RequestMapping("/api/image")
class UserImageController @Autowired constructor(
    private val imageService: ImageService,
    private val jwtUtils: JwtUtils,
    private val bkAuthClient: BkAuthClient
) {
    @ApiOperation("查询镜像")
    @PostMapping("/queryPublicImage")
    fun queryPublicImage(
        @RequestBody request: QueryPublicImageRequest
    ): Response<Page<DockerRepo>> {
        return ResponseBuilder.success(imageService.queryPublicImage(request))
    }

    @ApiOperation("查询镜像")
    @PostMapping("/queryProjectImage")
    fun queryProjectImage(
        @RequestHeader("X-BKAPI-JWT") jwkToken: String?,
        @RequestBody request: QueryProjectImageRequest
    ): Response<Page<DockerRepo>> {
//        val jwkData = jwtUtils.parseJwtToken(jwkToken!!) ?: throw StatusCodeException(HttpStatus.UNAUTHORIZED,
//        "invalid jwt token")
//        val hashPermission = bkAuthClient.checkProjectPermission(jwkData!!.userName, request.projectId)
//        if (!hashPermission) throw StatusCodeException(HttpStatus.UNAUTHORIZED, "permission denied")
        return ResponseBuilder.success(imageService.queryProjectImage(request))
    }

    @ApiOperation("查询tag")
    @PostMapping("/queryImageTag")
    fun queryImageTag(
        @RequestBody request: QueryImageTagRequest
    ): Response<Page<DockerTag>> {
        return ResponseBuilder.success(imageService.queryImageTag(request))
    }
}
