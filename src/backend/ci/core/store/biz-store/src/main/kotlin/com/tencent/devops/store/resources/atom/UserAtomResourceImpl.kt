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

package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.UserAtomResource
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.service.atom.AtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAtomResourceImpl @Autowired constructor(private val atomService: AtomService) :
    UserAtomResource {

    override fun getPipelineAtom(
        projectCode: String,
        atomCode: String,
        version: String,
        queryOfflineFlag: Boolean?
    ): Result<PipelineAtom?> {
        return atomService.getPipelineAtom(
            projectCode = projectCode,
            atomCode = atomCode,
            version = version,
            queryOfflineFlag = queryOfflineFlag ?: true
        )
    }

    override fun listAllPipelineAtoms(
        accessToken: String,
        userId: String,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean,
        fitOsFlag: Boolean?,
        queryFitAgentBuildLessAtomFlag: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<AtomResp<AtomRespItem>?> {
        return atomService.getPipelineAtoms(
            accessToken = accessToken,
            userId = userId,
            serviceScope = serviceScope,
            jobType = jobType,
            os = os,
            projectCode = projectCode,
            category = category,
            classifyId = classifyId,
            recommendFlag = recommendFlag,
            keyword = keyword,
            queryProjectAtomFlag = queryProjectAtomFlag,
            queryFitAgentBuildLessAtomFlag = queryFitAgentBuildLessAtomFlag,
            fitOsFlag = fitOsFlag,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getPipelineAtomVersions(projectCode: String, atomCode: String): Result<List<VersionInfo>> {
        return atomService.getPipelineAtomVersions(projectCode, atomCode)
    }

    override fun getInstalledAtoms(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        name: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<InstalledAtom>> {
        return Result(
            atomService.getInstalledAtoms(
                userId = userId,
                projectCode = projectCode,
                classifyCode = classifyCode,
                name = name,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun updateAtomBaseInfo(
        userId: String,
        atomCode: String,
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Result<Boolean> {
        return atomService.updateAtomBaseInfo(userId, atomCode, atomBaseInfoUpdateRequest)
    }

    override fun uninstallAtom(
        userId: String,
        projectCode: String,
        atomCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean> {
        return atomService.uninstallAtom(userId, projectCode, atomCode, unInstallReq)
    }
}
