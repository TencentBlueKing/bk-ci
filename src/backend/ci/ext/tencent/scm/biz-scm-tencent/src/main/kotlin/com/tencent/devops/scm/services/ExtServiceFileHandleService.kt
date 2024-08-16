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

package com.tencent.devops.scm.services

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.nio.charset.Charset

@Service("SERVICE_FILE_HANDLE")
class ExtServiceFileHandleService @Autowired constructor(
    private val redisOperation: RedisOperation
) : AbstractFileHandleService {

    private val logger = LoggerFactory.getLogger(ExtServiceFileHandleService::class.java)

    private val serviceItemsRedisKey = "ext:service:items"

    /**
     * 处理extension.json文件
     */
    override fun handleFile(
        repositoryName: String,
        fileName: String,
        workspace: File?
    ): Result<Boolean> {
        // 把extension.json中的serviceCode修改成用户对应的
        val extensionJsonFile = File(workspace, fileName)
        if (extensionJsonFile.exists()) {
            val jsonStr = extensionJsonFile.readText(Charset.forName("UTF-8"))
            val jsonMap = JsonUtil.toMap(jsonStr).toMutableMap()
            jsonMap["serviceCode"] = repositoryName
            val serviceItemsJsonStr = redisOperation.get("$serviceItemsRedisKey:$repositoryName")
            logger.info("handleFile serviceItemsJsonStr is:$serviceItemsJsonStr")
            if (serviceItemsJsonStr != null) {
                val itemList = mutableListOf<Map<String, Any>>()
                val serviceItems = JsonUtil.to(serviceItemsJsonStr, object : TypeReference<Set<String>>() {})
                serviceItems.forEach {
                    itemList.add(
                        mapOf(
                            "itemCode" to it,
                            "props" to emptyMap<String, Any>()
                        )
                    )
                }
                jsonMap["itemList"] = itemList
            }
            val deleteFlag = extensionJsonFile.delete()
            if (deleteFlag) {
                extensionJsonFile.createNewFile()
                extensionJsonFile.writeText(JsonUtil.toJson(jsonMap), Charset.forName("UTF-8"))
            }
            // 修改完后文件删除redis中存的扩展服务的扩展点数据
            redisOperation.delete("$serviceItemsRedisKey:$repositoryName")
        }
        return Result(true)
    }
}
