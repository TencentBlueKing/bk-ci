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

package com.tencent.devops.metrics.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.metrics.api.UserAtomDisplayConfigResource
import com.tencent.devops.metrics.pojo.`do`.AtomBaseInfoDO
import com.tencent.devops.metrics.service.AtomDisplayConfigManageService
import com.tencent.devops.metrics.pojo.dto.AtomDisplayConfigDTO
import com.tencent.devops.metrics.pojo.vo.AtomDisplayConfigVO
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAtomDisplayConfigResourceImpl @Autowired constructor(
    private val atomDisplayConfigManageService: AtomDisplayConfigManageService
) : UserAtomDisplayConfigResource {
    override fun addAtomDisplayConfig(
        projectId: String,
        userId: String,
        atomDisplayConfig: AtomDisplayConfigVO
    ): Result<Boolean> {
        return Result(
            atomDisplayConfigManageService.addAtomDisplayConfig(
                AtomDisplayConfigDTO(
                    projectId = projectId,
                    userId = userId,
                    atomBaseInfos = atomDisplayConfig.atomBaseInfos
                )
            )
        )
    }

    override fun deleteAtomDisplayConfig(
        projectId: String,
        userId: String,
        atomDisplayConfig: AtomDisplayConfigVO
    ): Result<Boolean> {
        return Result(
            atomDisplayConfigManageService.deleteAtomDisplayConfig(
                    projectId = projectId,
                    userId = userId,
                    atomCodes = atomDisplayConfig.atomBaseInfos
            )
        )
    }

    override fun getAtomDisplayConfig(
        projectId: String,
        userId: String,
        keyword: String?
    ): Result<AtomDisplayConfigVO> {
        return Result(
            atomDisplayConfigManageService.getAtomDisplayConfig(
                projectId = projectId,
                userId = userId,
                keyword = keyword
            )
        )
    }

    override fun getOptionalAtomDisplayConfig(
        projectId: String,
        userId: String,
        keyword: String?,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomBaseInfoDO>> {
        return Result(
            atomDisplayConfigManageService.getOptionalAtomDisplayConfig(
                projectId = projectId,
                userId = userId,
                keyword = keyword,
                page = page,
                pageSize = pageSize
            )
        )
    }
}
