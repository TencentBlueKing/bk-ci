/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.defect.service.impl

import com.tencent.bk.codecc.defect.service.LogService
import com.tencent.bk.codecc.task.vo.QueryLogRepVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.pojo.AllProperties
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.OkhttpUtils.okHttpClient
import com.tencent.devops.log.api.ServiceLogResource
import okhttp3.Request
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletResponse

/**
 * @date 2019/7/11
 */
@Service
class LogServiceImpl @Autowired constructor(
    private val client: Client,
    private val allProperties: AllProperties
) : LogService {

    /**
     * 获取日志服务接口实现
     * 1. 通过projectId、pipelineId、buildId查到的日志是此次在流水线分析的所有原子日志[排队、下载代码、扫描分析、 提单]
     * 2. 加上tag可以查到具体的原子日志，但目前CodeCC中的TaskLogEntity中没有存流水线返回的tag.
     */
    override fun getAnalysisLog(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        queryKeywords: String?,
        tag: String?
    ): QueryLogRepVO? {
        val result = client.getDevopsService(ServiceLogResource::class.java)
            .getInitLogs(userId, projectId, pipelineId, buildId, false, null,
                tag, null, 1)
        if (result.isNotOk() || null == result.data) {
            logger.error(
                "get log info fail! bs project id: {}, bs pipeline id: {}, build id: {}",
                projectId,
                pipelineId,
                buildId
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val queryLogs = QueryLogRepVO()
        BeanUtils.copyProperties(result.data!!, queryLogs)
        return queryLogs
    }

    /**
     * 获取分析记录的更多日志
     */
    override fun getMoreLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        executeCount: Int?
    ): QueryLogRepVO {
        val result = client.getDevopsService(ServiceLogResource::class.java)
            .getMoreLogs(userId,
                projectId, pipelineId, buildId, false,null,num ?: 100, fromStart
                    ?: true, start, end, tag, null, executeCount ?: 1
            )
        if (result.isNotOk() || null == result.data) {
            logger.error(
                "get more log info fail! bs project id: {}, bs pipeline id: {}, build id: {}",
                projectId,
                pipelineId,
                buildId
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val queryLogs = QueryLogRepVO()
        BeanUtils.copyProperties(result.data!!, queryLogs)
        queryLogs.status = QueryLogRepVO.LogStatus.SUCCEED.value
        return queryLogs
    }

    /**
     * 下载分析记录日志
     */
    override fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        executeCount: Int?
    ) {
        logger.info(
            "start download analysis log file. project id: {}, bs pipeline id: {}, build id: {}",
            projectId,
            pipelineId,
            buildId
        )
        // val downloadLogs = client.getDevopsService(ServiceLogResource::class.java).downloadLogs(projectId, pipelineId, buildId, tag, executeCount ?: 1)
        val execute = executeCount ?: 1
        val tagEle = tag ?: ""
        var url =
            "http://${allProperties.devopsDevUrl}/ms/log/api/service/logs/$projectId/$pipelineId/$buildId/download?executeCount=$execute"
        logger.info("download file url: {}", url)
        if (StringUtils.isNotBlank(tagEle)) {
            url = "$url&tag=$tagEle"
        }

        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val resp = attributes!!.response ?: return
        resp.setHeader("content-disposition", "attachment; filename = $pipelineId-$buildId-log.txt")
        resp.setHeader("Cache-Control", "no-cache")
        resp.contentType = "application/octet-stream; charset=UTF-8"
        downloadFile(url, resp)
    }

    /**
     * 获取某行后的日志
     */
    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        queryKeywords: String?,
        tag: String?,
        executeCount: Int?
    ): QueryLogRepVO {
        val result = client.getDevopsService(ServiceLogResource::class.java)
            .getAfterLogs(userId, projectId, pipelineId, buildId, start, false,null,
                tag, null, executeCount ?: 1)
        if (result.isNotOk() || null == result.data) {
            logger.error(
                "get more log info fail! bs project id: {}, bs pipeline id: {}, build id: {}",
                projectId,
                pipelineId,
                buildId
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val queryLogs = QueryLogRepVO()
        BeanUtils.copyProperties(result.data!!, queryLogs)
        queryLogs.status = QueryLogRepVO.LogStatus.SUCCEED.value
        return queryLogs
    }

    /**
     * 下载文件
     */
    fun downloadFile(url: String, httpResponse: HttpServletResponse) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.code() == 404) {
                throw OperationException("文件不存在")
            }
            if (!response.isSuccessful) {
                throw OperationException("获取文件失败")
            }

            response.body()!!.byteStream().use { bs ->
                val buf = ByteArray(4096)
                var len = bs.read(buf)
                httpResponse.outputStream.use { os ->
                    while (len != -1) {
                        os.write(buf, 0, len)
                        len = bs.read(buf)
                    }
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LogServiceImpl::class.java)
    }
}

