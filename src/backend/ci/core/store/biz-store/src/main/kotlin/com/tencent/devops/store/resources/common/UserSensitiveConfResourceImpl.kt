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

package com.tencent.devops.store.resources.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserSensitiveConfResource
import com.tencent.devops.store.pojo.common.SensitiveConfReq
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.UserSensitiveConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserSensitiveConfResourceImpl @Autowired constructor(
    private val userSensitiveConfService: UserSensitiveConfService
) : UserSensitiveConfResource {

    override fun create(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        return userSensitiveConfService.create(userId, storeType, storeCode, sensitiveConfReq)
    }

    override fun edit(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        return userSensitiveConfService.update(userId, storeType, storeCode, id, sensitiveConfReq)
    }

    override fun delete(userId: String, storeType: StoreTypeEnum, storeCode: String, ids: String): Result<Boolean> {
        return userSensitiveConfService.delete(userId, storeType, storeCode, ids)
    }

    override fun get(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String
    ): Result<SensitiveConfResp?> {
        return userSensitiveConfService.get(userId, storeType, storeCode, id)
    }

    override fun list(userId: String, storeType: StoreTypeEnum, storeCode: String): Result<List<SensitiveConfResp>?> {
        return userSensitiveConfService.list(userId, storeType, storeCode, false)
    }
}