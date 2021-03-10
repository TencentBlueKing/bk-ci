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

package com.tencent.devops.statistics.service.project.impl

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.BkAuthProperties
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.AuthProjectForList
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.statistics.dao.project.ProjectDao
import com.tencent.devops.statistics.jmx.api.project.ProjectJmxApi
import com.tencent.devops.statistics.service.project.ProjectPermissionService
import com.tencent.devops.statistics.util.project.ProjectUtils
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.ArrayList

@Service
class TxProjectServiceImpl @Autowired constructor(
    projectPermissionService: ProjectPermissionService,
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao,
    private val bkAuthProperties: BkAuthProperties,
    projectJmxApi: ProjectJmxApi,
    redisOperation: RedisOperation,
    gray: Gray
) : AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray) {

    private var authUrl: String = "${bkAuthProperties.url}/projects"

    override fun list(userId: String, accessToken: String?): List<ProjectVO> {
        val startEpoch = System.currentTimeMillis()
        try {

            val projects = getProjectFromAuth(userId, accessToken).toSet()
            if (projects == null || projects.isEmpty()) {
                return emptyList()
            }
            logger.info("项目列表：$projects")
            val list = ArrayList<ProjectVO>()
            projectDao.list(dslContext, projects).map {
                list.add(ProjectUtils.packagingBean(it, grayProjectSet()))
            }
            return list
        } finally {
            logger.info("It took ${System.currentTimeMillis() - startEpoch}ms to list projects")
        }
    }

    override fun getProjectFromAuth(userId: String?, accessToken: String?): List<String> {
        val url = "$authUrl?access_token=$accessToken"
        logger.info("Start to get auth projects - ($url)")
        val request = Request.Builder().url(url).get().build()
        val responseContent = request(request, MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        val result = objectMapper.readValue<Result<ArrayList<AuthProjectForList>>>(responseContent)
        if (result.isNotOk()) {
            logger.warn("Fail to get the project info with response $responseContent")
            throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PEM_QUERY_ERROR))
        }
        if (result.data == null) {
            return emptyList()
        }

        return result.data!!.map {
            it.project_id
        }
    }

    private fun request(request: Request, errorMessage: String): String {
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("Fail to request($request) with code ${response.code()} , message ${response.message()} and response $responseContent")
                throw OperationException(errorMessage)
            }
            return responseContent
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxProjectServiceImpl::class.java)!!
    }
}
