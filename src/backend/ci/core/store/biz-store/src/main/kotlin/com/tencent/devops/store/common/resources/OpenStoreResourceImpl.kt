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
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.annotation.BkApiPermission
import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.store.api.common.OpenStoreResource
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreProjectService
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpenStoreResourceImpl @Autowired constructor(
    private val storeProjectService: StoreProjectService,
    private val storeCommonService: StoreCommonService,
    private val redisOperation: RedisOperation
) : OpenStoreResource {

    @BkApiPermission([BkApiHandleType.API_OPEN_TOKEN_CHECK])
    override fun validateProjectComponentPermission(
        token: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        val storePublicFlagKey = StoreUtils.getStorePublicFlagKey(storeType.name)
        if (redisOperation.isMember(storePublicFlagKey, storeCode)) {
            // 如果从缓存中查出该组件是公共组件则无需权限校验
            return Result(true)
        }
        return Result(
            storeCommonService.getStorePublicFlagByCode(storeCode, storeType) ||
                storeProjectService.isInstalledByProject(
                    projectCode = projectCode,
                    storeCode = storeCode,
                    storeType = storeType.type.toByte()
                )
        )
    }
}
