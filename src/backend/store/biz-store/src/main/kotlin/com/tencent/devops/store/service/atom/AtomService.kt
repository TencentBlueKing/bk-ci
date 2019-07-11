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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomRespItem
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.VersionInfo
import org.springframework.stereotype.Service

/**
 * 插件业务逻辑类
 *
 * since: 2018-12-20
 */
@Service
interface AtomService {

    /**
     * 获取插件列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineAtoms(
        accessToken: String,
        userId: String,
        serviceScope: String?,
        os: String?,
        projectCode: String,
        category: String?,
        classifyId: String?,
        page: Int?,
        pageSize: Int?
    ): Result<AtomResp<AtomRespItem>?>

    /**
     * 根据id获取插件信息
     */
    fun getPipelineAtom(id: String): Result<Atom?>

    /**
     * 根据插件代码和版本号获取插件信息
     */
    fun getPipelineAtom(projectCode: String, atomCode: String, version: String): Result<PipelineAtom?>

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineAtomDetail(projectCode: String, atomCode: String, version: String): Result<PipelineAtom?>

    /**
     * 根据项目代码、插件代码和版本号获取插件信息
     */
    @Suppress("UNCHECKED_CAST")
    fun getPipelineAtomVersions(projectCode: String, atomCode: String): Result<List<VersionInfo>>

    /**
     * 根据插件代码和版本号获取插件信息
     */
    fun getPipelineAtom(atomCode: String, version: String): Result<Atom?>

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
     * 获取插件的中文名
     */
    fun getProjectAtomNames(projectCode: String): Result<Map<String, String>>
}
