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

package com.tencent.devops.misc.service.artifactory

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.misc.dao.artifactory.TxArtifactoryDataClearDao
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TxArtifactoryDataClearServiceImpl @Autowired constructor(
    dslContext: DSLContext
) : ArtifactoryDataClearService(dslContext) {

    @Autowired
    private lateinit var txArtifactoryDataClearDao: TxArtifactoryDataClearDao

    @Value("\${build.data.clear.basicAuth.bkrepo.baseUrl:}")
    private val bkRepoBaseUrl: String = ""

    @Value("\${build.data.clear.basicAuth.bkrepo.username:}")
    private val repoUserName: String = ""
    @Value("\${build.data.clear.basicAuth.bkrepo.password:}")
    private val repoPassword: String = ""

    override fun deleteTableData(dslContext: DSLContext, buildId: String) {
        txArtifactoryDataClearDao.deleteArtifacetoryInfoByBuildId(dslContext, buildId)
    }

    override fun cleanBuildHistoryRepoData(projectId: String, pipelineId: String, buildIds: List<String>) {
        val url = "${getBkRepoUrl()}/repository/api/ext/pipeline/build/data/clear"
        logger.info("pipelineBuildHistoryDataClear|$projectId|$pipelineId|buildIds = $buildIds")
        val context = mapOf<String, Any>(
            "projectId" to projectId,
            "pipelineId" to pipelineId,
            "buildIds" to buildIds
        )

        val body = RequestBody.create(
            MediaType.parse("application/json"),
            JsonUtil.toJson(context)
        )

        val request = Request.Builder()
            .url(url)
            .put(body)
            .addHeader("Authorization" , Credentials.basic(repoUserName, repoPassword))
            .addHeader(AUTH_HEADER_DEVOPS_PROJECT_ID, projectId)
            .build()
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.warn("cleanBuildHistoryRepoData fail body is $body")
            }
            logger.info("cleanBuildHistoryRepoData response is $responseContent")
        }
    }

    private fun getBkRepoUrl(): String {
        return bkRepoBaseUrl.removeSuffix("/")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxArtifactoryDataClearServiceImpl::class.java)
    }
}
