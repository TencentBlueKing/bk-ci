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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.store.config.ExtServiceIngressConfig
import com.tencent.devops.store.dao.ExtItemServiceDao
import com.tencent.devops.store.pojo.vo.ExtItemServiceVO
import com.tencent.devops.store.pojo.vo.ExtServiceVO
import com.tencent.devops.store.pojo.vo.ExtServiceVendorVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.MessageFormat

@Service
class ExtItemServiceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val extItemServiceDao: ExtItemServiceDao,
    private val extServiceIngressConfig: ExtServiceIngressConfig
) {

    private val logger = LoggerFactory.getLogger(ExtItemServiceService::class.java)

    fun getExtItemServiceList(
        userId: String,
        projectCode: String,
        itemIds: String
    ): Result<List<ExtItemServiceVO>> {
        logger.info("getExtItemServiceList userId is :$userId,itemIds is :$itemIds,projectCode is :$projectCode")
        val extServiceList = mutableListOf<ExtItemServiceVO>()
        val itemIdList = itemIds.split(",")
        itemIdList.forEach { it ->
            val serviceRecords = extItemServiceDao.getExtItemServiceList(
                dslContext = dslContext,
                userId = userId,
                itemId = it,
                projectCode = projectCode,
                page = null,
                pageSize = null
            )
            val serviceList = mutableListOf<ExtServiceVO>()
            serviceRecords?.forEach { service ->
                val props = service["props"] as? String
                val serviceCode = service["serviceCode"] as String
                // 获取扩展服务对应的域名
                val host = MessageFormat(extServiceIngressConfig.host).format(arrayOf(serviceCode))
                serviceList.add(
                    ExtServiceVO(
                        serviceId = service["serviceId"] as String,
                        serviceName = service["serviceName"] as String,
                        serviceCode = service["serviceCode"] as String,
                        version = service["version"] as String,
                        summary = service["summary"] as? String,
                        vendor = ExtServiceVendorVO(
                            name = service["publisher"] as String
                        ),
                        baseUrl = "http://$host",
                        props = if (!props.isNullOrBlank()) JsonUtil.toMap(props!!) else null
                    )
                )
            }
            extServiceList.add(ExtItemServiceVO(itemId = it, extServiceList = serviceList))
        }
        return Result(extServiceList)
    }
}