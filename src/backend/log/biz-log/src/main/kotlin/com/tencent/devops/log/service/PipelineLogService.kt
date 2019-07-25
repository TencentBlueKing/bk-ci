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

package com.tencent.devops.log.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.jmx.CreateIndexBean
import com.tencent.devops.log.jmx.LogBean
import com.tencent.devops.log.jmx.UpdateIndexBean
import com.tencent.devops.log.model.pojo.EndPageQueryLogs
import com.tencent.devops.log.model.pojo.PageQueryLogs
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.log.model.pojo.enums.LogStatus
import org.elasticsearch.client.transport.TransportClient
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class PipelineLogService @Autowired constructor(
    client: TransportClient,
    private val indexService: IndexService,
    defaultKeywords: List<String>,
    private val logBean: LogBean,
    updateIndexBean: UpdateIndexBean,
    createIndexBean: CreateIndexBean,
    redisOperation: RedisOperation,
    rabbitTemplate: RabbitTemplate
) : LogService(
    client, indexService, defaultKeywords, logBean, updateIndexBean,
    createIndexBean, redisOperation, rabbitTemplate
) {

    override fun queryInitLogsPage(
        buildId: String,
        index: String,
        type: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): PageQueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.queryInitLogsPage(
                buildId,
                index,
                type,
                isAnalysis,
                keywordsStr,
                tag,
                executeCount ?: 1,
                page,
                pageSize
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.queryInitLogsPage(
                        buildId,
                        index,
                        type,
                        isAnalysis,
                        keywordsStr,
                        tag,
                        null,
                        page,
                        pageSize
                    )
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$buildId|$index|$type|$isAnalysis|$keywordsStr|$tag|$executeCount|$page|$pageSize] " +
                    "It took ${elapse}ms to query init log page with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun queryInitLogs(
        buildId: String,
        index: String,
        type: String,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        executeCount: Int?
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.queryInitLogs(
                buildId, index, type, isAnalysis,
                keywordsStr, tag, executeCount ?: 1
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.queryInitLogs(buildId, index, type, isAnalysis, keywordsStr, tag, null)
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$buildId|$index|$type|$isAnalysis|$keywordsStr|$tag|$executeCount] " +
                    "It took ${elapse}ms to query the inig logs with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun queryInitLogsPage(
        buildId: String,
        index: String,
        type: String,
        wholeQuery: Boolean,
        keywords: List<String>,
        tag: String?,
        executeCount: Int?,
        page: Int,
        pageSize: Int
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.queryInitLogsPage(
                buildId,
                index,
                type,
                wholeQuery,
                keywords,
                tag,
                executeCount ?: 1,
                page,
                pageSize
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.queryInitLogsPage(
                        buildId,
                        index,
                        type,
                        wholeQuery,
                        keywords,
                        tag,
                        null,
                        page,
                        pageSize
                    )
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$buildId|$index|$type|$wholeQuery|$keywords|$tag|$executeCount|$page|$pageSize] " +
                    "It took ${elapse}ms to query the init logs with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun queryMoreLogsBetweenLines(
        buildId: String,
        index: String,
        type: String,
        num: Int,
        fromStart: Boolean,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.queryMoreLogsBetweenLines(
                buildId,
                index,
                type,
                num,
                fromStart,
                start,
                end,
                tag,
                executeCount ?: 1
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.queryMoreLogsBetweenLines(
                        buildId, index, type, num, fromStart,
                        start, end, tag, null
                    )
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$buildId|$index|$type|$num|$fromStart|$start|$end|$tag|$executeCount] " +
                    "It took ${elapse}ms to query logs between line with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun queryMoreLogsAfterLine(
        buildId: String,
        index: String,
        type: String,
        start: Long,
        isAnalysis: Boolean,
        keywordsStr: String?,
        tag: String?,
        executeCount: Int?
    ): QueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.queryMoreLogsAfterLine(
                buildId,
                index,
                type,
                start,
                isAnalysis,
                keywordsStr,
                tag,
                executeCount ?: 1
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.queryMoreLogsAfterLine(
                        buildId, index, type,
                        start, isAnalysis, keywordsStr, tag, null
                    )
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$$buildId|$index|$type|$start|$isAnalysis|$keywordsStr|$tag|$executeCount] " +
                    "It took ${elapse}ms to query more logs after lines with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun downloadLogs(pipelineId: String, buildId: String, tag: String, executeCount: Int?): Response {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            val index = indexService.queryIndex(buildId)
            val logSize = getLogSize(index, buildId, tag, executeCount ?: 1)
            // 兼容旧的没有executeCount的数据
            if (logSize == 0L) {
                if (executeCount == null || executeCount == 1) {
                    val response = super.downloadLogs(pipelineId, buildId, tag, null)
                    success = true
                    return response
                }
            }
            val response = super.downloadLogs(pipelineId, buildId, tag, executeCount)
            success = true
            return response
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$pipelineId|$buildId|$tag|$executeCount] " +
                    "It took ${elapse}ms to downloads with result - $success"
            )
            logBean.query(elapse, success)
        }
    }

    override fun getEndLogs(pipelineId: String, buildId: String, tag: String, executeCount: Int?, size: Int)
        : EndPageQueryLogs {
        val startEpoch = System.currentTimeMillis()
        var success = false
        try {
            var result = super.getEndLogs(
                pipelineId, buildId, tag,
                executeCount ?: 1, size
            )
            // 兼容旧的没有executeCount的数据
            if (result.status == LogStatus.EMPTY) {
                if (executeCount == null || executeCount == 1) {
                    result = super.getEndLogs(pipelineId, buildId, tag, null, size)
                }
            }
            success = result.status != LogStatus.FAIL
            return result
        } finally {
            val elapse = System.currentTimeMillis() - startEpoch
            logger.info(
                "[$pipelineId|$buildId|$tag|$executeCount|$size] " +
                    "It took ${elapse}ms to query the end logs with results - $success"
            )
            logBean.query(elapse, success)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineLogService::class.java)
    }
}
