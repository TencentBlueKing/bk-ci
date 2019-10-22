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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.log.model.pojo.QueryLogs
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

/**
 *
 * Powered By Tencent
 */
@Api(tags = ["OP_LOG"], description = "管理-日志资源")
@Path("/op/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpLogResource {

    @ApiOperation("(Warning: 没事不要随便调用哦) 预创建 numDays 天内的 ES Index")
    @POST
    @Path("/preCreateIndices")
    fun preCreateIndices(
        @ApiParam("创建天数", required = true)
        @QueryParam("numDays")
        numDays: Int
    ): Result<Int>

    @ApiOperation("(Warning: 没事不要随便调用哦) 创建 log status index")
    @POST
    @Path("/logStatus")
    fun createLogStatus(): Result<Boolean>

    @ApiOperation("设置项目支持v2版本")
    @PUT
    @Path("/v2/projects/{projectId}")
    fun enableV2(
            @ApiParam("项目ID", required = true)
            @PathParam("projectId")
            projectId: String,
            @ApiParam("开启或禁止", required = true)
            @QueryParam("enable")
            enable: Boolean
    ): Result<Boolean>

    @ApiParam("获取支持V2版本的项目")
    @GET
    @Path("/v2/projects")
    fun getV2Projects(): Result<Set<String>>


    @ApiParam("设置构建清理过期时长")
    @PUT
    @Path("/v2/build/clean/expire")
    fun setBuildExpire(
            @ApiParam("时间, 单位(天)", required = true)
            @QueryParam("expire")
            expire: Int
    ): Result<Boolean>

    @ApiParam("获取构建清理过期时长")
    @GET
    @Path("/v2/build/clean/expire")
    fun getBuildExpire(): Result<Int>

    @ApiParam("设置ES索引过期时长")
    @PUT
    @Path("/v2/es/index/expire")
    fun setESExpire(
            @ApiParam("时间, 单位(天)", required = true)
            @QueryParam("expire")
            expire: Int
    ): Result<Boolean>

    @ApiParam("获取ES索引过期时长")
    @GET
    @Path("/v2/es/index/expire")
    fun getESExpire(): Result<Int>

    @ApiParam("重新打开索引")
    @PUT
    @Path("/v2/es/index/reopen")
    fun reopenIndex(
            @ApiParam("构建ID", required = true)
            @QueryParam("buildId")
            buildId: String
    ): Result<Boolean>

    @ApiOperation("根据构建ID获取初始化所有日志")
    @GET
    @Path("/{buildId}/")
    fun getInitLogs(
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("是否请求分析日志", required = false)
        @QueryParam("isAnalysis")
        isAnalysis: Boolean? = false,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("queryKeywords")
        queryKeywords: String?,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @ApiOperation("获取更多日志")
    @GET
    @Path("/{buildId}/more")
    fun getMoreLogs(
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("日志行数", required = false)
        @QueryParam("num")
        num: Int? = 100,
        @ApiParam("是否正序输出", required = false)
        @QueryParam("fromStart")
        fromStart: Boolean? = true,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @ApiParam("结尾行号", required = true)
        @QueryParam("end")
        end: Long,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>

    @ApiOperation("获取某行后的日志")
    @GET
    @Path("/{buildId}/after")
    fun getAfterLogs(
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @ApiParam("是否请求分析日志", required = false)
        @QueryParam("isAnalysis")
        isAnalysis: Boolean? = false,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("queryKeywords")
        queryKeywords: String?,
        @ApiParam("对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs>
}