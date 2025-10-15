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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.UserRepositoryConfigResource
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import com.tencent.devops.repository.pojo.RepositoryConfigLogoInfo
import com.tencent.devops.repository.pojo.RepositoryScmConfigReq
import com.tencent.devops.repository.pojo.RepositoryScmConfigVo
import com.tencent.devops.repository.pojo.RepositoryScmProviderVo
import com.tencent.devops.repository.pojo.ScmConfigBaseInfo
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import com.tencent.devops.repository.service.RepositoryScmConfigService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserRepositoryConfigResourceImpl @Autowired constructor(
    private val repositoryScmConfigService: RepositoryScmConfigService
) : UserRepositoryConfigResource {

    override fun list(userId: String, scmType: ScmType?): Result<List<ScmConfigBaseInfo>> {
        return Result(
            repositoryScmConfigService.listConfigBaseInfo(userId, scmType)
        )
    }

    override fun listProvider(userId: String): Result<List<RepositoryScmProviderVo>> {
        return Result(repositoryScmConfigService.listProvider(userId))
    }

    override fun listConfig(
        userId: String,
        status: ScmConfigStatus?,
        excludeStatus: ScmConfigStatus?,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<RepositoryScmConfigVo>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        return Result(
            repositoryScmConfigService.listConfigVo(
                userId = userId,
                status = status,
                excludeStatus = excludeStatus,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
        )
    }

    override fun create(userId: String, request: RepositoryScmConfigReq): Result<Boolean> {
        repositoryScmConfigService.create(
            userId = userId,
            request = request
        )
        return Result(true)
    }

    override fun edit(userId: String, scmCode: String, request: RepositoryScmConfigReq): Result<Boolean> {
        repositoryScmConfigService.edit(
            userId = userId,
            scmCode = scmCode,
            request = request
        )
        return Result(true)
    }

    override fun enable(userId: String, scmCode: String): Result<Boolean> {
        repositoryScmConfigService.enable(
            userId = userId,
            scmCode = scmCode
        )
        return Result(true)
    }

    override fun disable(userId: String, scmCode: String): Result<Boolean> {
        repositoryScmConfigService.disable(
            userId = userId,
            scmCode = scmCode
        )
        return Result(true)
    }

    override fun delete(userId: String, scmCode: String): Result<Boolean> {
        repositoryScmConfigService.delete(
            userId = userId,
            scmCode = scmCode
        )
        return Result(true)
    }

    override fun uploadLogo(
        userId: String,
        contentLength: Long,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<RepositoryConfigLogoInfo?> {
        return Result(
            repositoryScmConfigService.uploadLogo(
                userId = userId,
                contentLength = contentLength,
                inputStream = inputStream,
                disposition = disposition
            )
        )
    }

    override fun supportEvents(userId: String, scmCode: String): Result<List<IdValue>> {
        return Result(
            repositoryScmConfigService.supportEvents(
                userId = userId,
                scmCode = scmCode
            )
        )
    }

    override fun supportEventActions(userId: String, scmCode: String, eventType: String): Result<List<IdValue>> {
        return Result(
            repositoryScmConfigService.supportEventActions(
                userId = userId,
                scmCode = scmCode,
                eventType = eventType
            )
        )
    }

    override fun supportDept(
        userId: String,
        scmCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<SQLPage<RepositoryConfigVisibility>> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        return Result(
            repositoryScmConfigService.listDept(
                userId = userId,
                scmCode = scmCode,
                limit = sqlLimit.limit,
                offset = sqlLimit.offset
            )
        )
    }

    override fun addDept(
        userId: String,
        scmCode: String,
        deptList: List<RepositoryConfigVisibility>?
    ): Result<Boolean> {
        deptList?.let {
            repositoryScmConfigService.addDept(
                userId = userId,
                scmCode = scmCode,
                deptList = it
            )
        }
        return Result(true)
    }

    override fun deleteDept(
        userId: String,
        scmCode: String,
        deptList: List<Int>?
    ): Result<Boolean> {
        deptList?.let {
            repositoryScmConfigService.deleteDept(
                userId = userId,
                scmCode = scmCode,
                deptList = it
            )
        }
        return Result(true)
    }
}
