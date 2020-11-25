package com.tencent.devops.worker.common.api.archive

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.pojo.QueryData
import com.tencent.devops.worker.common.api.pojo.QueryNodeInfo
import okhttp3.MediaType
import okhttp3.RequestBody
import org.slf4j.LoggerFactory

class BkRepoResourceApi : AbstractBuildResourceApi() {
    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoResourceApi::class.java)
    }

    fun queryByPathEqOrNameMatchOrMetadataEqAnd(
        userId: String,
        projectId: String, // eq
        repoNames: List<String>, // eq or
        filePaths: List<String>, // eq or
        fileNames: List<String>, // match or
        metadata: Map<String, String>, // eq and
        page: Int,
        pageSize: Int
    ): List<QueryNodeInfo> {
        logger.info("queryByPathEqOrNameMatchOrMetadataEqAnd, userId: $userId, projectId: $projectId, repoNames: $repoNames, filePaths: $filePaths, fileNames: $fileNames, metadata: $metadata, page: $page, pageSize: $pageSize")
        val projectRule = Rule.QueryRule("projectId", projectId, OperationType.EQ)
        val repoRule = Rule.QueryRule("repoName", repoNames, OperationType.IN)
        var ruleList = mutableListOf<Rule>(projectRule, repoRule, Rule.QueryRule("folder", false, OperationType.EQ))
        if (filePaths.isNotEmpty()) {
            val filePathRule = Rule.NestedRule(filePaths.map { Rule.QueryRule("path", it, OperationType.EQ) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(filePathRule)
        }
        if (fileNames.isNotEmpty()) {
            val fileNameRule = Rule.NestedRule(fileNames.map { Rule.QueryRule("name", it, OperationType.MATCH) }.toMutableList(), Rule.NestedRule.RelationType.OR)
            ruleList.add(fileNameRule)
        }
        if (metadata.isNotEmpty()) {
            val metadataRule = Rule.NestedRule(metadata.map { Rule.QueryRule("metadata.${it.key}", it.value, OperationType.EQ) }.toMutableList(), Rule.NestedRule.RelationType.AND)
            ruleList.add(metadataRule)
        }
        var rule = Rule.NestedRule(ruleList, Rule.NestedRule.RelationType.AND)

        return query(userId, projectId, rule, page, pageSize)
    }

    private fun query(userId: String, projectId: String, rule: Rule, page: Int, pageSize: Int): List<QueryNodeInfo> {
        // logger.info("query, userId: $userId, rule: $rule, page: $page, pageSize: $pageSize")
        val url = "/bkrepo/api/build/repository/api/node/query"
        val queryModel = QueryModel(
            page = PageLimit(page, pageSize),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf(),
            rule = rule
        )

        val requestBody = objectMapper.writeValueAsString(queryModel)
        // logger.info("requestBody: $requestBody")
        val request = buildPost(
            url,
            RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                requestBody
            ),
            mutableMapOf("X-BKREPO-UID" to userId)
        )
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("query failed, responseContent: $responseContent")
                throw RuntimeException("query failed")
            }

            val responseData = objectMapper.readValue<Response<QueryData>>(responseContent)
            if (responseData.isNotOk()) {
                throw RuntimeException("query failed: ${responseData.message}")
            }

            return responseData.data!!.records
        }
    }
}