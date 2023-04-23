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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.OpAtomResource
import com.tencent.devops.store.pojo.atom.ApproveReq
import com.tencent.devops.store.pojo.atom.Atom
import com.tencent.devops.store.pojo.atom.AtomCreateRequest
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.AtomResp
import com.tencent.devops.store.pojo.atom.AtomUpdateRequest
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.service.atom.AtomReleaseService
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.atom.OpAtomService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class OpAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService,
    private val opAtomService: OpAtomService,
    private val marketAtomService: MarketAtomService,
    private val atomReleaseService: AtomReleaseService
) : OpAtomResource {

    override fun add(userId: String, atomCreateRequest: AtomCreateRequest): Result<Boolean> {
        return atomService.savePipelineAtom(userId, atomCreateRequest)
    }

    override fun update(userId: String, id: String, atomUpdateRequest: AtomUpdateRequest): Result<Boolean> {
        return atomService.updatePipelineAtom(userId, id, atomUpdateRequest)
    }

    override fun listAllPipelineAtoms(
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
    ): Result<AtomResp<Atom>?> {
        return opAtomService.getOpPipelineAtoms(
            atomName = atomName,
            atomCode = atomCode,
            atomType = atomType,
            serviceScope = serviceScope,
            os = os,
            category = category,
            classifyId = classifyId,
            atomStatus = atomStatus,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getPipelineAtomById(id: String): Result<Atom?> {
        return opAtomService.getPipelineAtom(id)
    }

    override fun deletePipelineAtomById(id: String): Result<Boolean> {
        return atomService.deletePipelineAtom(id)
    }

    override fun approveAtom(userId: String, atomId: String, approveReq: ApproveReq): Result<Boolean> {
        return opAtomService.approveAtom(userId, atomId, approveReq)
    }

    override fun generateCiYaml(
        atomCode: String?,
        os: String?,
        classType: String?,
        defaultShowFlag: Boolean?
    ): Result<String> {
        return Result(marketAtomService.generateCiYaml(atomCode, os, classType, defaultShowFlag))
    }

    override fun offlineAtom(userId: String, atomCode: String, atomOfflineReq: AtomOfflineReq): Result<Boolean> {
        return atomReleaseService.offlineAtom(
            userId = userId,
            atomCode = atomCode,
            atomOfflineReq = atomOfflineReq,
            checkPermissionFlag = false
        )
    }

    override fun releaseAtom(
        userId: String,
        atomCode: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        return opAtomService.releaseAtom(
            userId = userId,
            atomCode = atomCode,
            inputStream = inputStream,
            disposition = disposition
        )
    }

    override fun setDefault(userId: String, atomCode: String): Result<Boolean> {
        return Result(opAtomService.setDefault(userId = userId, atomCode = atomCode))
    }
}
