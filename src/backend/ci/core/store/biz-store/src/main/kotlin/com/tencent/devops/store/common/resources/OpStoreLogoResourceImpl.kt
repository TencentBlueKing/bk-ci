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

package com.tencent.devops.store.common.resources

import com.tencent.devops.store.api.common.OpStoreLogoResource
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.pojo.common.logo.Logo
import com.tencent.devops.store.pojo.common.logo.StoreLogoInfo
import com.tencent.devops.store.pojo.common.logo.StoreLogoReq
import com.tencent.devops.store.pojo.common.enums.LogoTypeEnum
import com.tencent.devops.store.common.service.StoreLogoService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired
import java.io.InputStream

@RestResource
class OpStoreLogoResourceImpl @Autowired constructor(
    private val storeLogoService: StoreLogoService
) : OpStoreLogoResource {

    override fun uploadStoreLogo(
        userId: String,
        contentLength: Long,
        sizeLimitFlag: Boolean?,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<StoreLogoInfo?> {
        return storeLogoService.uploadStoreLogo(
            userId = userId,
            sizeLimitFlag = sizeLimitFlag,
            contentLength = contentLength,
            inputStream = inputStream,
            disposition = disposition
        )
    }

    override fun add(userId: String, logoType: LogoTypeEnum, storeLogoReq: StoreLogoReq): Result<Boolean> {
        return storeLogoService.add(userId, logoType.name, storeLogoReq)
    }

    override fun update(userId: String, id: String, storeLogoReq: StoreLogoReq): Result<Boolean> {
        return storeLogoService.update(userId, id, storeLogoReq)
    }

    override fun get(userId: String, id: String): Result<Logo?> {
        return storeLogoService.get(userId, id)
    }

    override fun list(userId: String, logoType: LogoTypeEnum): Result<List<Logo>?> {
        return storeLogoService.list(userId, logoType.name)
    }

    override fun delete(userId: String, id: String): Result<Boolean> {
        return storeLogoService.delete(userId, id)
    }
}
