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

package com.tencent.devops.process.service.wetest

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.process.dao.third.ThirdJobDao
import com.tencent.devops.process.pojo.third.enum.JobType
import com.tencent.devops.process.pojo.third.wetest.WetestCallback
import com.tencent.devops.process.pojo.third.wetest.WetestResponse
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val thirdJobDao: ThirdJobDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestService::class.java)
    }

    fun saveCallback(callback: WetestCallback) {
        val str = objectMapper.writeValueAsString(callback)
        logger.info("JinGangAppCallback: $str")
        thirdJobDao.saveCallback(dslContext, callback.taskID, JobType.WETEST, str)
    }

    fun saveResponse(response: WetestResponse, projectId: String, pipelineId: String, buildId: String) {
        val str = objectMapper.writeValueAsString(response)
        if (response.taskId.isNullOrBlank()) throw RuntimeException("task id is null")
        thirdJobDao.saveResponse(dslContext, response.taskId!!, str, JobType.WETEST, projectId, pipelineId, buildId)
    }
}
