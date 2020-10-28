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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectGraySetRequest
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PROJECT"], description = "项目列表接口")
@Path("/op/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPProjectResource {

    @GET
    @Path("/")
    @ApiOperation("查询所有项目")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ProjectVO>>

    @ApiOperation("更新项目信息")
    @PUT
    @Path("/")
    fun updateProject(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("PAAS_CC Token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam(value = "项目信息请求实体", required = true)
        projectInfoRequest: OpProjectUpdateInfoRequest
    ): Result<Int>

    @ApiOperation("获取项目信息列表")
    @GET
    @Path("/list/project")
    fun getProjectList(
        @ApiParam(value = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @ApiParam(value = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @ApiParam(value = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @ApiParam(value = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @ApiParam(value = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @ApiParam(value = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @ApiParam(value = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @ApiParam(value = "偏移量", required = true)
        @QueryParam(value = "offset")
        offset: Int,
        @ApiParam(value = "查询数量", required = true)
        @QueryParam(value = "limit")
        limit: Int,
        @ApiParam(value = "是否灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_gray")
        grayFlag: Boolean,
        @Context request: HttpServletRequest
    ): Result<Map<String, Any?>?>

    @ApiOperation("获取项目信息列表，支持筛选仓库灰度")
    @GET
    @Path("/list/repoGrayProject")
    fun getProjectList(
        @ApiParam(value = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @ApiParam(value = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @ApiParam(value = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @ApiParam(value = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @ApiParam(value = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @ApiParam(value = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @ApiParam(value = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @ApiParam(value = "偏移量", required = true)
        @QueryParam(value = "offset")
        offset: Int,
        @ApiParam(value = "查询数量", required = true)
        @QueryParam(value = "limit")
        limit: Int,
        @ApiParam(value = "是否灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_gray")
        grayFlag: Boolean,
        @ApiParam(value = "是否仓库灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_repo_gray")
        repoGrayFlag: Boolean,
        @Context request: HttpServletRequest
    ): Result<Map<String, Any?>?>

    @ApiOperation("获取项目信息列表，支持筛选仓库灰度")
    @GET
    @Path("/list/macosGrayProject")
    fun getProjectList(
        @ApiParam(value = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @ApiParam(value = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @ApiParam(value = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @ApiParam(value = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @ApiParam(value = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @ApiParam(value = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @ApiParam(value = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @ApiParam(value = "偏移量", required = true)
        @QueryParam(value = "offset")
        offset: Int,
        @ApiParam(value = "查询数量", required = true)
        @QueryParam(value = "limit")
        limit: Int,
        @ApiParam(value = "是否灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_gray")
        grayFlag: Boolean,
        @ApiParam(value = "是否CodeCC灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_codecc_gray")
        codeccGrayFlag: Boolean,
        @ApiParam(value = "是否仓库灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_repo_gray")
        repoGrayFlag: Boolean,
        @ApiParam(value = "是否MacOS公共构建机灰度 true：是 false：否", required = true)
        @QueryParam(value = "is_repo_gray")
        macosGrayFlag: Boolean,
        @Context request: HttpServletRequest
    ): Result<Map<String, Any?>?>

//    @ApiOperation("获取项目信息列表")
//    @GET
//    @Path("/list/query")
//    fun getProjectListV2(
//            @ApiParam(value = "项目名称", required = false)
//            @QueryParam(value = "project_name")
//            projectName: String?,
//            @ApiParam(value = "项目简称", required = false)
//            @QueryParam(value = "english_name")
//            englishName: String?,
//            @ApiParam(value = "项目类型", required = false)
//            @QueryParam(value = "project_type")
//            projectType: Int?,
//            @ApiParam(value = "是否保密", required = false)
//            @QueryParam(value = "is_secrecy")
//            isSecrecy: Boolean?,
//            @ApiParam(value = "注册人", required = false)
//            @QueryParam(value = "creator")
//            creator: String?,
//            @ApiParam(value = "审批人", required = false)
//            @QueryParam(value = "approver")
//            approver: String?,
//            @ApiParam(value = "审核状态", required = false)
//            @QueryParam(value = "approval_status")
//            approvalStatus: Int?,
//            @ApiParam(value = "偏移量", required = true)
//            @QueryParam(value = "offset")
//            offset: Int,
//            @ApiParam(value = "查询数量", required = true)
//            @QueryParam(value = "limit")
//            limit: Int,
//            @ApiParam(value = "是否灰度 true：是 false：否", required = true)
//            @QueryParam(value = "is_gray")
//            grayFlag: Boolean,
//            @Context request: HttpServletRequest
//    ): Result<Map<String, Any?>?>

    @ApiOperation("获取项目数量")
    @GET
    @Path("/list/projectCount")
    fun getProjectCount(
        @ApiParam(value = "项目名称", required = false)
        @QueryParam(value = "project_name")
        projectName: String?,
        @ApiParam(value = "项目简称", required = false)
        @QueryParam(value = "english_name")
        englishName: String?,
        @ApiParam(value = "项目类型", required = false)
        @QueryParam(value = "project_type")
        projectType: Int?,
        @ApiParam(value = "是否保密", required = false)
        @QueryParam(value = "is_secrecy")
        isSecrecy: Boolean?,
        @ApiParam(value = "注册人", required = false)
        @QueryParam(value = "creator")
        creator: String?,
        @ApiParam(value = "审批人", required = false)
        @QueryParam(value = "approver")
        approver: String?,
        @ApiParam(value = "审核状态", required = false)
        @QueryParam(value = "approval_status")
        approvalStatus: Int?,
        @ApiParam(value = "是否灰度 true：是 false：否", defaultValue = false.toString())
        @QueryParam(value = "is_gray")
        grayFlag: Boolean
    ): Result<Int>

    @ApiOperation("灰度项目设置")
    @PUT
    @Path("/setGrayProject")
    fun setGrayProject(
        @ApiParam(value = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @ApiOperation("灰度项目设置")
    @PUT
    @Path("/codecc/setGrayProject")
    fun setCodeCCGrayProject(
        @ApiParam(value = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @ApiOperation("bkrepo灰度项目设置")
    @PUT
    @Path("/setRepoGrayProject")
    fun setRepoGrayProject(
        @ApiParam(value = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @ApiOperation("bkrepo非灰度项目设置")
    @PUT
    @Path("/setRepoNotGrayProject")
    fun setRepoNotGrayProject(
        @ApiParam(value = "非灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

    @ApiOperation("macos灰度项目设置")
    @PUT
    @Path("/setMacOSGrayProject")
    fun setMacOSGrayProject(
        @ApiParam(value = "灰度项目设置请求实体", required = true)
        projectGraySetRequest: OpProjectGraySetRequest
    ): Result<Boolean>

//    @ApiOperation("灰度项目设置")
//    @PUT
//    @Path("/{projectId}/gray")
//    fun setGrayProjectV2(
//            @ApiParam(value = "灰度项目设置请求实体", required = true)
//            projectGraySetRequest: OpProjectGraySetRequest
//    ): Result<Boolean>

    @ApiOperation("查看灰度项目列表")
    @GET
    @Path("/listGrayProject")
    fun listGrayProject(): Result<OpGrayProject>

    @ApiOperation("同步项目")
    @PUT
    @Path("/{projectId}/syn")
    fun synProject(
        @ApiParam(value = "项目code", required = true)
        @PathParam("projectId")
        projectCode: String,
        @ApiParam(value = "是否触发刷数据 true：是 false：否", defaultValue = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<Boolean>

    @ApiOperation("同步项目初始化")
    @PUT
    @Path("/init/syn")
    fun synProjectInit(
        @ApiParam(value = "是否触发刷数据 true：是 false：否", defaultValue = false.toString())
        @QueryParam(value = "isRefresh")
        isRefresh: Boolean
    ): Result<List<String>>
}