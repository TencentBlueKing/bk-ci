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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.pojo.common.KEY_INSTALL_PARAMS
import com.tencent.devops.store.pojo.common.KEY_INSTALL_PATH
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TYPE
import com.tencent.devops.store.pojo.common.enums.StoreInstallTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvRequest
import com.tencent.devops.store.pojo.common.publication.StoreBaseExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureExtDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureRequest

object StoreReleaseUtils {

    private const val MAX_INSTALL_PATH_LENGTH = 512
    private const val MAX_INSTALL_PARAMS_LENGTH = 1024

    // 安装路径中禁止出现的字符：控制字符、换行、以及Windows路径非法字符
    private val INSTALL_PATH_ILLEGAL_REGEX = Regex("[\\u0000-\\u001f<>|*?\"]")

    // 安装参数中禁止出现的字符/片段：防止命令拼接注入(客户端会用其拼接安装命令执行)
    private val INSTALL_PARAMS_ILLEGAL_REGEX = Regex("[\\u0000-\\u001f`;&|<>\\n\\r]|\\$\\(|\\$\\{")

    /**
     * 校验组件部署相关扩展字段(安装路径/安装方式/安装参数)。
     * 仅对 DEVX 类型的组件进行校验，其他类型组件跳过。
     * @param extBaseInfo 版本级扩展信息(含安装方式installType、安装参数installParams)
     * @param extBaseFeatureInfo 组件级共享扩展信息(含安装路径installPath)
     * @param storeType 组件类型
     */
    fun validateDeployExtInfo(
        extBaseInfo: Map<String, Any>?,
        extBaseFeatureInfo: Map<String, Any>?,
        storeType: StoreTypeEnum
    ) {
        // 仅 DEVX 类型组件需要校验部署扩展字段
        if (storeType != StoreTypeEnum.DEVX) return
        // 安装方式：枚举校验
        extBaseInfo?.get(KEY_INSTALL_TYPE)?.let { value ->
            val installType = value as? String
            if (installType.isNullOrBlank() || StoreInstallTypeEnum.getStoreInstallType(installType) == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(KEY_INSTALL_TYPE, value.toString())
                )
            }
        }
        // 安装参数：长度与危险字符校验
        extBaseInfo?.get(KEY_INSTALL_PARAMS)?.let { value ->
            val installParams = value as? String
            if (installParams != null) {
                validateSecurityField(
                    fieldName = KEY_INSTALL_PARAMS,
                    fieldValue = installParams,
                    maxLength = MAX_INSTALL_PARAMS_LENGTH,
                    illegalRegex = INSTALL_PARAMS_ILLEGAL_REGEX
                )
            }
        }
        // 安装路径：长度与危险字符校验
        extBaseFeatureInfo?.get(KEY_INSTALL_PATH)?.let { value ->
            val installPath = value as? String
            if (installPath != null) {
                validateSecurityField(
                    fieldName = KEY_INSTALL_PATH,
                    fieldValue = installPath,
                    maxLength = MAX_INSTALL_PATH_LENGTH,
                    illegalRegex = INSTALL_PATH_ILLEGAL_REGEX
                )
            }
        }
    }

    private fun validateSecurityField(
        fieldName: String,
        fieldValue: String,
        maxLength: Int,
        illegalRegex: Regex
    ) {
        if (fieldValue.length > maxLength || illegalRegex.containsMatchIn(fieldValue)) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(fieldName, fieldValue)
            )
        }
    }

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
