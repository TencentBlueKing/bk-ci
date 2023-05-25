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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import java.io.InputStream

interface OpAtomService {

    /**
     * op系统获取插件信息
     */
    fun getOpPipelineAtoms(
        atomName: String?,
        atomCode: String?,
        atomType: AtomTypeEnum?,
        serviceScope: String?,
        os: String?,
        category: String?,
        classifyId: String?,
        atomStatus: AtomStatusEnum?,
        sortType: OpSortTypeEnum?,
        desc: Boolean?,
        page: Int,
        pageSize: Int
    ): Result<AtomResp<Atom>?>

    /**
     * 根据id获取插件信息
     */
    fun getPipelineAtom(id: String): Result<Atom?>

    /**
     * 根据插件代码和版本号获取插件信息
     */
    fun getPipelineAtom(atomCode: String, version: String): Result<Atom?>

    /**
     * 审核插件
     */
    fun approveAtom(userId: String, atomId: String, approveReq: ApproveReq): Result<Boolean>

    /**
     * 一键部署发布插件
     */
    fun releaseAtom(
        userId: String,
        atomCode: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean>

    /**
     * 将插件设置为默认
     */
    fun setDefault(userId: String, atomCode: String): Boolean
}
