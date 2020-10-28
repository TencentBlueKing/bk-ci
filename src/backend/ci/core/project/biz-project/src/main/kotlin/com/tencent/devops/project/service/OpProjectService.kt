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

package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.OpGrayProject
import com.tencent.devops.project.pojo.OpProjectUpdateInfoRequest
import com.tencent.devops.project.pojo.Result

interface OpProjectService {

//    fun syncCCAppName(): Int

    fun listGrayProject(): Result<OpGrayProject>

    fun setGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean

    fun setCodeCCGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean

    fun setRepoGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean

    fun setRepoNotGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean

    fun setMacOSGrayProject(projectCodeList: List<String>, operateFlag: Int): Boolean

    fun updateProjectFromOp(userId: String, accessToken: String, projectInfoRequest: OpProjectUpdateInfoRequest): Int

    fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean
    ): Result<Map<String, Any?>?>

    fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        repoGrayFlag: Boolean
    ): Result<Map<String, Any?>?>

    fun getProjectList(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean,
        macosGrayFlag: Boolean
    ): Result<Map<String, Any?>?>

    fun getProjectCount(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        grayFlag: Boolean
    ): Result<Int>

    fun synProject(
        projectCode: String,
        isRefresh: Boolean? = true
    ): Result<Boolean>

    fun synProjectInit(
        isRefresh: Boolean? = true
    ): Result<List<String>>

}