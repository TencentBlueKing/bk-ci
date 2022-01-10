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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.pojo.common.EXTENSION_JSON_NAME
import com.tencent.devops.store.pojo.common.TASK_JSON_NAME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@Service
class SampleProjectGitFileService {

    private val logger = LoggerFactory.getLogger(SampleProjectGitFileService::class.java)

    /**
     * 处理示例工程文件
     */
    fun handleSampleProjectGitFile(
        nameSpaceName: String,
        repositoryName: String,
        workspace: File? = null
    ): Result<Boolean> {
        logger.info(
            "handleSampleProjectGitFile workspace is:${workspace?.absolutePath}, nameSpaceName is :$nameSpaceName")
        // 根据groupName推断出要处理的文件
        var type: String? = null
        var fileName: String? = null
        val name = nameSpaceName.split("/")[0]
        if (name.contains("bkdevops-plugins")) {
            // 处理插件示例工程的git文件
            type = StoreTypeEnum.ATOM.name
            fileName = TASK_JSON_NAME
        } else if (name.contains("bkdevops-extension-service")) {
            // 处理扩展服务示例工程的git文件
            type = StoreTypeEnum.SERVICE.name
            fileName = EXTENSION_JSON_NAME
        }
        logger.info("handleSampleProjectGitFile type is:$type, fileName is :$fileName")
        return if (type != null && fileName != null) {
            val fileHandleService =
                SpringContextUtil.getBean(AbstractFileHandleService::class.java, "${type}_FILE_HANDLE")
            fileHandleService.handleFile(repositoryName, fileName, workspace)
        } else {
            Result(true)
        }
    }
}
