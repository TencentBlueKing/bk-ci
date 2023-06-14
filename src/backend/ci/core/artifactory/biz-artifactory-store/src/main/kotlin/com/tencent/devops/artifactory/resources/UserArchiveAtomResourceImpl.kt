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

package com.tencent.devops.artifactory.resources

import com.tencent.devops.artifactory.api.UserArchiveAtomResource
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.artifactory.pojo.ReArchiveAtomRequest
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class UserArchiveAtomResourceImpl @Autowired constructor(private val archiveAtomService: ArchiveAtomService) :
    UserArchiveAtomResource {

    override fun archiveAtom(
        userId: String,
        projectCode: String,
        atomId: String,
        atomCode: String,
        version: String,
        releaseType: ReleaseTypeEnum,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        os: String
    ): Result<ArchiveAtomResponse?> {
        return archiveAtomService.archiveAtom(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            archiveAtomRequest = ArchiveAtomRequest(
                projectCode = projectCode,
                atomCode = atomCode,
                version = version,
                releaseType = releaseType,
                os = os
            )
        )
    }

    override fun reArchiveAtom(
        userId: String,
        projectCode: String,
        atomId: String,
        atomCode: String,
        version: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        fieldCheckConfirmFlag: Boolean?
    ): Result<ArchiveAtomResponse?> {
        return archiveAtomService.reArchiveAtom(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            reArchiveAtomRequest = ReArchiveAtomRequest(
                projectCode = projectCode,
                atomId = atomId,
                atomCode = atomCode,
                version = version,
                fieldCheckConfirmFlag = fieldCheckConfirmFlag
            )
        )
    }
}
