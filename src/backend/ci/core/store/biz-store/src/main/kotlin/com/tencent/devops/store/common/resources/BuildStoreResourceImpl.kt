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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.BuildStoreResource
import com.tencent.devops.store.common.service.StorePackageDeployService
import com.tencent.devops.store.common.service.StorePkgRunEnvInfoService
import com.tencent.devops.store.common.service.StoreReleaseService
import com.tencent.devops.store.common.service.UserSensitiveConfService
import com.tencent.devops.store.pojo.common.enums.FieldTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.env.StorePkgRunEnvInfo
import com.tencent.devops.store.pojo.common.publication.StoreProcessInfo
import com.tencent.devops.store.pojo.common.sensitive.SensitiveConfResp
import java.io.InputStream
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildStoreResourceImpl @Autowired constructor(
    private val sensitiveConfService: UserSensitiveConfService,
    private val storePkgRunEnvInfoService: StorePkgRunEnvInfoService,
    private val storePackageDeployService: StorePackageDeployService,
    private val storeReleaseService: StoreReleaseService
) : BuildStoreResource {

    override fun getSensitiveConf(
        buildId: String,
        vmSeqId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<List<SensitiveConfResp>?> {
        sensitiveConfService.checkOperationAuthority(buildId, vmSeqId, storeType, storeCode)
        val types = FieldTypeEnum.BACKEND.name + "," + FieldTypeEnum.ALL.name
        return sensitiveConfService.list(
            userId = "",
            storeType = storeType,
            storeCode = storeCode,
            isDecrypt = true,
            types = types
        )
    }

    override fun getStorePkgRunEnvInfo(
        devopsEnv: String?,
        storeType: StoreTypeEnum,
        language: String,
        runtimeVersion: String,
        osName: String,
        osArch: String
    ): Result<StorePkgRunEnvInfo?> {
        return Result(
            storePkgRunEnvInfoService.getStorePkgRunEnvInfo(
                devopsEnv = devopsEnv,
                userId = "",
                storeType = storeType,
                language = language,
                osName = osName,
                osArch = osArch,
                runtimeVersion = runtimeVersion
            )
        )
    }

    override fun oneClickDeployComponent(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<String?> {
        return Result(
            storePackageDeployService.oneClickDeployComponent(
                userId = userId,
                storeCode = storeCode,
                storeType = storeType,
                inputStream = inputStream,
                disposition = disposition,
            )
        )
    }

    override fun getProcessInfo(userId: String, storeId: String): Result<StoreProcessInfo> {
        return Result(storeReleaseService.getProcessInfo(userId, storeId))
    }
}
