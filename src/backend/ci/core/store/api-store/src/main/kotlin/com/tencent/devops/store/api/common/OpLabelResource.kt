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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.label.LabelRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_LABEL", description = "OP-STORE-标签")
@Path("/op/store/label")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpLabelResource {

    @Operation(summary = "添加标签")
    @POST
    @Path("/types/{labelType}")
    fun add(
        @Parameter(description = "类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum,
        @Parameter(description = "标签信息请求报文体", required = true)
        labelRequest: LabelRequest
    ): Result<Boolean>

    @Operation(summary = "更新标签信息")
    @PUT
    @Path("/types/{labelType}/ids/{id}")
    fun update(
        @Parameter(description = "类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum,
        @Parameter(description = "标签ID", required = true)
        @PathParam("id")
        id: String,
        @Parameter(description = "标签信息请求报文体", required = true)
        labelRequest: LabelRequest
    ): Result<Boolean>

    @Operation(summary = "获取所有标签信息")
    @GET
    @Path("/types/{labelType}")
    fun listAllLabels(
        @Parameter(description = "类别", required = true)
        @PathParam("labelType")
        labelType: StoreTypeEnum
    ): Result<List<Label>?>

    @Operation(summary = "根据ID获取标签信息")
    @GET
    @Path("/{id}")
    fun getLabelById(
        @Parameter(description = "标签ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Label?>

    @Operation(summary = "根据ID删除标签信息")
    @DELETE
    @Path("/{id}")
    fun deleteLabelById(
        @Parameter(description = "标签ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
