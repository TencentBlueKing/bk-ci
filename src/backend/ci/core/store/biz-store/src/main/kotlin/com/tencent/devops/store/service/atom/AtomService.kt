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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.VersionInfo

/**
 * 插件业务逻辑类
 *
 * since: 2018-12-20
 */
@Suppress("ALL")
interface AtomService {

    /**
     * 获取插件列表
     */
    fun getPipelineAtoms(
        accessToken: String,
        userId: String,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String? = AtomCategoryEnum.TASK.name,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean = true,
        fitOsFlag: Boolean? = true,
        queryFitAgentBuildLessAtomFlag: Boolean? = true,
        page: Int = 1,
        pageSize: Int = 10
    ): Result<AtomResp<AtomRespItem>?>

    /**
     * 获取插件列表
     */
    fun serviceGetPipelineAtoms(
        userId: String,
        serviceScope: String?,
        jobType: String?,
        os: String?,
        projectCode: String,
        category: String? = AtomCategoryEnum.TASK.name,
        classifyId: String?,
        recommendFlag: Boolean?,
        keyword: String?,
        queryProjectAtomFlag: Boolean = true,
        fitOsFlag: Boolean? = true,
        queryFitAgentBuildLessAtomFlag: Boolean? = true,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?>

    /**
     * 获取项目下插件相关的信息
     */
    fun getProjectElements(projectCode: String): Result<Map<String, String>>

    /**
     * 获取项目下所有可用的插件信息
     */
    fun getProjectElementsInfo(projectCode: String): Result<Map<String, String>>

    /**
     * 根据插件代码和版本号获取插件信息
     */
    fun getPipelineAtom(
        projectCode: String,
        atomCode: String,
        version: String,
        atomStatus: Byte? = null,
        queryOfflineFlag: Boolean = false
    ): Result<PipelineAtom?>

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineAtomDetail(
        projectCode: String? = null,
        atomCode: String,
        version: String,
        atomStatus: Byte? = null,
        queryOfflineFlag: Boolean = false
    ): Result<PipelineAtom?>

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineAtomVersions(projectCode: String? = null, atomCode: String): Result<List<VersionInfo>>

    /**
     * 添加插件信息
     */
    fun savePipelineAtom(userId: String, atomRequest: AtomCreateRequest): Result<Boolean>

    /**
     * 更新插件信息
     */
    fun updatePipelineAtom(userId: String, id: String, atomUpdateRequest: AtomUpdateRequest): Result<Boolean>

    /**
     * 删除插件信息
     */
    fun deletePipelineAtom(id: String): Result<Boolean>

    /**
     * 根据插件ID和插件代码判断插件是否存在
     */
    fun judgeAtomExistByIdAndCode(atomId: String, atomCode: String): Result<Boolean>

    /**
     * 根据用户ID和插件代码判断该插件是否由该用户创建
     */
    fun judgeAtomIsCreateByUserId(userId: String, atomCode: String): Result<Boolean>

    /**
     * 是否有管理插件权限
     */
    fun hasManagerPermission(projectCode: String, userId: String): Boolean

    /**
     * 获取已安装的插件列表
     */
    fun getInstalledAtoms(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<InstalledAtom>

    /**
     * 获取已安装的插件列表
     */
    fun listInstalledAtomByProject(
        projectCode: String
    ): List<InstalledAtom>

    /**
     * 卸载插件
     */
    fun uninstallAtom(
        userId: String,
        projectCode: String,
        atomCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean>

    /**
     * 更新插件基本信息
     */
    fun updateAtomBaseInfo(
        userId: String,
        atomCode: String,
        atomBaseInfoUpdateRequest: AtomBaseInfoUpdateRequest
    ): Result<Boolean>

    /**
     * 获取插件真实版本号
     * @param projectCode 项目代码
     * @param atomCode 插件代码
     * @param version 插件版本号
     * @return 插件真实版本号
     */
    fun getAtomRealVersion(projectCode: String, atomCode: String, version: String): Result<String?>

    /**
     * 获取插件默认可用版本号
     * @param projectCode 项目代码
     * @param atomCode 插件代码
     * @return 插件默认版本号
     */
    fun getAtomDefaultValidVersion(projectCode: String, atomCode: String): Result<VersionInfo?>
}
