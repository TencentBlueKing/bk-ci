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

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.archive.client.JfrogService
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.pojo.migcdn.MigCDNUploadParam
import okhttp3.Request
import okhttp3.internal.Util
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class MigCDNService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val jfrogService: JfrogService,
    private val buildLogPrinter: BuildLogPrinter
) {

    @Value("\${gateway.url}")
    private lateinit var gatewayUrl: String

    val MIG_CDN_URL = "http://up.cdn.qq.com:8600/uploadserver/uploadfile.jsp"

    fun pushFile(migCDNUploadParam: MigCDNUploadParam): String {
        val fileParams = migCDNUploadParam.fileParams
        logger.info("MIG CDN upload param for build(${fileParams.buildId}): $migCDNUploadParam")

        var hasMatchedFile = false
        fileParams.regexPath.split(",").map { it.trim() }.forEach outside@{
            val path = if (fileParams.custom) "/${it.removePrefix("/")}" else "/${fileParams.pipelineId}/${fileParams.buildId}/${it.removePrefix("/")}"

            val param = ArtifactorySearchParam(
                    migCDNUploadParam.fileParams.projectId,
                    migCDNUploadParam.fileParams.pipelineId,
                    migCDNUploadParam.fileParams.buildId,
                    it,
                    migCDNUploadParam.fileParams.custom,
                    migCDNUploadParam.fileParams.executeCount,
                    migCDNUploadParam.fileParams.elementId
            )

            val fileMatched = jfrogService.matchFiles(param).map {
                it.removePrefix("bk-archive/")
                        .removePrefix("bk-custom/")
            }
            if (fileMatched.isEmpty()) {
                logger.info("File matched nothing with regex: $it")
                return@outside
            }

            hasMatchedFile = true
            fileMatched.forEach inside@{ fileIter ->
                val fileNameType = getFileNameType(fileIter)
                if (fileNameType.first.isBlank()) {
                    logger.info("File type invalid, file:$fileIter, skipped")
                    buildLogPrinter.addLine(fileParams.buildId, "该文件不允许上传，文件:$fileIter，将跳过",
                        fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                    return@inside
                }

                val downLoadUrl = getUrl(fileIter, fileParams.custom)
                logger.info("downLoadUrl: $downLoadUrl")

                val url = "$MIG_CDN_URL?appname=${migCDNUploadParam.para.appName}&user=${migCDNUploadParam.operator}&filename=${fileNameType.first}" +
                        "&filetype=${fileNameType.second}&filepath=${migCDNUploadParam.para.destFileDir}&filesize=0&isunzip=${migCDNUploadParam.para.needUnzip}" +
                        "&remoteurl=${URLEncoder.encode(downLoadUrl, "UTF-8")}"
                logger.info("MIG CDN upload request url: $url")
                val request = Request.Builder().url(url).post(Util.EMPTY_REQUEST).addHeader("X-CDN-Authentication", migCDNUploadParam.para.appSecret).build()
                OkhttpUtils.doHttp(request).use { res ->
                    val response = res.body()!!.string()
                    logger.info("MIG CDN upload response for build(${fileParams.buildId}): $response")
                    val jsonMap = objectMapper.readValue<Map<String, Any>>(response)
                    val retCode = jsonMap["ret_code"] as Int
                    if (retCode != 200) {
                        val msg = jsonMap["err_msg"] as String
                        buildLogPrinter.addLine(fileParams.buildId, "上传CDN失败，文件：$path\n错误码：$retCode\n错误信息：$msg",
                            fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                        throw RuntimeException("上传到CDN失败")
                    }
                    val cdnUrl = if (null == jsonMap["cdn_url"]) "" else jsonMap["cdn_url"] as String
                    val fileMd5 = if (null == jsonMap["file_md5"]) "" else jsonMap["file_md5"] as String

                    buildLogPrinter.addLine(fileParams.buildId, "上传CDN成功，文件:$path:\ncdn_url:$cdnUrl\nfileMd5:$fileMd5",
                        fileParams.elementId, fileParams.containerId, fileParams.executeCount)
                }
            }
        }
        if (!hasMatchedFile) {
            logger.info("File matched nothing")
            buildLogPrinter.addLine(fileParams.buildId, "没有匹配到任何文件，请检查版本仓库以及源文件设置",
                fileParams.elementId, fileParams.containerId, fileParams.executeCount)
            throw RuntimeException("上传到CDN失败")
        }

        return "success"
    }

    private fun getFileNameType(realPath: String): Pair<String, String> {
        val destFileFullName = realPath.substringAfterLast('/')
        val destFileName = destFileFullName.substringBeforeLast('.')
        val destFileType: String
        val pos = destFileFullName.lastIndexOf('.')
        destFileType = if (-1 == pos) {
            "undef_ext"
        } else {
            if ("" == destFileFullName.substringAfterLast('.')) "undef_ext" else destFileFullName.substringAfterLast('.')
        }

        return Pair(destFileName, destFileType)
    }

    private fun getUrl(realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "http://$gatewayUrl/jfrog/storage/service/custom/$realPath"
        } else {
            "http://$gatewayUrl/jfrog/storage/service/archive/$realPath"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MigCDNService::class.java)
    }
}
