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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.store.dao.ExtItemServiceDao
import com.tencent.devops.store.pojo.vo.ExtItemServiceVO
import com.tencent.devops.store.pojo.vo.ExtServiceVendorVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtItemServiceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val extItemServiceDao: ExtItemServiceDao
) {

    fun getExtItemServiceList(
        userId: String,
        itemId: String,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ExtItemServiceVO>> {
        logger.info("getExtItemServiceList userId is :$userId,itemId is :$itemId,projectCode is :$projectCode")
        logger.info("getExtItemServiceList page is :$page,pageSize is :$pageSize")
        val serviceCount = extItemServiceDao.getExtItemServiceCount(dslContext, userId, itemId, projectCode)
        val serviceRecords = extItemServiceDao.getExtItemServiceList(dslContext, userId, itemId, projectCode, page, pageSize)
        val serviceList = mutableListOf<ExtItemServiceVO>()
        serviceRecords?.forEach {
            val props = it["props"] as? String
            serviceList.add(
                ExtItemServiceVO(
                    serviceId = it["serviceId"] as String,
                    serviceName = it["serviceName"] as String,
                    serviceCode = it["serviceCode"] as String,
                    version = it["version"] as String,
                    summary = it["summary"] as? String,
                    vendor = ExtServiceVendorVO(
                        name = it["publisher"] as String
                    ),
                    baseUrl = "", // todo 待网关完善好后把扩展服务访问地址前缀逻辑补上
                    props = if (!props.isNullOrBlank()) JsonUtil.toMap(props!!) else null
                )
            )
        }
        val totalPages = PageUtil.calTotalPage(pageSize, serviceCount)
        return Result(Page(count = serviceCount, page = page ?: 1, pageSize = pageSize ?: -1, totalPages = totalPages, records = serviceList))
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}