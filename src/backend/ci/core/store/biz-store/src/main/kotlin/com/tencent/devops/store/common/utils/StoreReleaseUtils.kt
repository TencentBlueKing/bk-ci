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

package com.tencent.devops.store.common.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvRequest
import com.tencent.devops.store.pojo.common.publication.StoreBaseExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureRequest

object StoreReleaseUtils {

    fun generateStoreBaseEnvPO(
        baseEnvInfos: List<StoreBaseEnvRequest>?,
        storeId: String,
        userId: String
    ): Pair<MutableList<StoreBaseEnvDataPO>?, MutableList<StoreBaseEnvExtDataPO>?> {
        var storeBaseEnvDataPOs: MutableList<StoreBaseEnvDataPO>? = null
        var storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO>? = null
        baseEnvInfos?.forEach { baseEnvInfo ->
            val envId = UUIDUtil.generate()
            val storeBaseEnvDataPO = StoreBaseEnvDataPO(
                id = envId,
                storeId = storeId,
                language = baseEnvInfo.language,
                creator = userId,
                modifier = userId
            )
            if (storeBaseEnvDataPOs == null) {
                storeBaseEnvDataPOs = mutableListOf()
            }
            storeBaseEnvDataPOs?.add(storeBaseEnvDataPO)
            val extBaseEnvInfo = baseEnvInfo.extBaseEnvInfo
            storeBaseEnvExtDataPOs = generateStoreBaseEnvExtPO(
                envId = envId,
                storeId = storeId,
                userId = userId,
                extBaseEnvInfo = extBaseEnvInfo
            )
        }
        return Pair(storeBaseEnvDataPOs, storeBaseEnvExtDataPOs)
    }

    fun generateStoreBaseEnvExtPO(
        envId: String,
        storeId: String,
        userId: String,
        extBaseEnvInfo: Map<String, Any>?
    ): MutableList<StoreBaseEnvExtDataPO>? {
        var storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO>? = null
        extBaseEnvInfo?.forEach { (key, value) ->
            if (storeBaseEnvExtDataPOs == null) {
                storeBaseEnvExtDataPOs = mutableListOf()
            }
            storeBaseEnvExtDataPOs?.add(
                StoreBaseEnvExtDataPO(
                    id = UUIDUtil.generate(),
                    envId = envId,
                    storeId = storeId,
                    fieldName = key,
                    fieldValue = JsonUtil.toJson(value, false),
                    creator = userId,
                    modifier = userId
                )
            )
        }
        return storeBaseEnvExtDataPOs
    }

    fun generateStoreBaseFeaturePO(
        baseFeatureInfo: StoreBaseFeatureRequest?,
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String
    ): Pair<StoreBaseFeatureDataPO?, MutableList<StoreBaseFeatureExtDataPO>?> {
        var storeBaseFeatureDataPO: StoreBaseFeatureDataPO? = null
        var storeBaseFeatureExtDataPOs: MutableList<StoreBaseFeatureExtDataPO>? = null
        baseFeatureInfo?.let {
            val featureId = UUIDUtil.generate()
            storeBaseFeatureDataPO = StoreBaseFeatureDataPO(
                id = featureId,
                storeCode = storeCode,
                storeType = storeType,
                type = baseFeatureInfo.type,
                rdType = baseFeatureInfo.rdType?.name,
                creator = userId,
                modifier = userId
            )
            val extBaseFeatureInfo = baseFeatureInfo.extBaseFeatureInfo
            extBaseFeatureInfo?.forEach { (key, value) ->
                if (storeBaseFeatureExtDataPOs == null) {
                    storeBaseFeatureExtDataPOs = mutableListOf()
                }
                storeBaseFeatureExtDataPOs?.add(
                    StoreBaseFeatureExtDataPO(
                        id = UUIDUtil.generate(),
                        featureId = featureId,
                        storeCode = storeCode,
                        storeType = storeType,
                        fieldName = key,
                        fieldValue = JsonUtil.toJson(value, false),
                        creator = userId,
                        modifier = userId
                    )
                )
            }
        }
        return Pair(storeBaseFeatureDataPO, storeBaseFeatureExtDataPOs)
    }

    fun generateStoreBaseExtDataPO(
        extBaseInfo: Map<String, Any>?,
        storeId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String
    ): MutableList<StoreBaseExtDataPO>? {
        var storeBaseExtDataPOs: MutableList<StoreBaseExtDataPO>? = null
        extBaseInfo?.forEach { (key, value) ->
            if (storeBaseExtDataPOs == null) {
                storeBaseExtDataPOs = mutableListOf()
            }
            storeBaseExtDataPOs?.add(
                StoreBaseExtDataPO(
                    id = UUIDUtil.generate(),
                    storeId = storeId,
                    storeCode = storeCode,
                    storeType = storeType,
                    fieldName = key,
                    fieldValue = JsonUtil.toJson(value, false),
                    creator = userId,
                    modifier = userId
                )
            )
        }
        return storeBaseExtDataPOs
    }
}
