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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceEnvDao
import com.tencent.devops.store.dao.ExtServiceFeatureDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.UpdateExtServiceEnvInfoDTO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceEnvService @Autowired constructor(
    private val dslContext: DSLContext,
    private val extServiceDao: ExtServiceDao,
    private val extServiceFeatureDao: ExtServiceFeatureDao,
    private val extServiceEnvDao: ExtServiceEnvDao,
    private val storeProjectRelDao: StoreProjectRelDao
) {

    private val logger = LoggerFactory.getLogger(ExtServiceEnvService::class.java)

    /**
     * 更新扩展服务执行环境信息
     */
    fun updateExtServiceEnvInfo(
        projectCode: String,
        serviceCode: String,
        version: String,
        updateExtServiceEnvInfo: UpdateExtServiceEnvInfoDTO
    ): Result<Boolean> {
        logger.info("updateExtServiceEnvInfo params:[$projectCode|$serviceCode|$version|$updateExtServiceEnvInfo")
        val extServiceRecord = extServiceDao.getExtService(dslContext, serviceCode, version)
        if (null == extServiceRecord || extServiceRecord.deleteFlag) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("$serviceCode+$version"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        // 判断用户的项目是否安装了该扩展服务
        val extServiceFeatureRecord = extServiceFeatureDao.getServiceByCode(dslContext, serviceCode)!!
        if (!extServiceFeatureRecord.publicFlag) {
            val flag = storeProjectRelDao.isInstalledByProject(
                dslContext = dslContext,
                projectCode = projectCode,
                storeCode = serviceCode,
                storeType = StoreTypeEnum.SERVICE.type.toByte()
            )
            if (!flag) {
                return MessageUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("$serviceCode+$version"),
                    data = false,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        }
        extServiceEnvDao.updateExtServiceEnvInfo(dslContext, extServiceRecord.id, updateExtServiceEnvInfo)
        return Result(true)
    }
}
