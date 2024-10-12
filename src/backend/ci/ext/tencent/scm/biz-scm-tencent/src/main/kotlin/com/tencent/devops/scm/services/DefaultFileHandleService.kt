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
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.store.pojo.common.KEY_STORE_CODE
import org.springframework.stereotype.Service
import java.io.File
import java.nio.charset.Charset

@Service("DEFAULT_FILE_HANDLE")
class DefaultFileHandleService : AbstractFileHandleService {

    /**
     * 处理bk-config.yml文件
     */
    override fun handleFile(
        repositoryName: String,
        fileName: String,
        workspace: File?
    ): Result<Boolean> {
        // 把配置文件中的storeCode修改成用户对应的
        val bkConfigFile = File(workspace, fileName)
        if (bkConfigFile.exists()) {
            val fileContent = bkConfigFile.readText(Charset.forName(Charsets.UTF_8.name()))
            val dataMap = YamlUtil.to(fileContent, object : TypeReference<MutableMap<String, Any>>() {})
            dataMap[KEY_STORE_CODE] = repositoryName
            val deleteFlag = bkConfigFile.delete()
            if (deleteFlag) {
                bkConfigFile.createNewFile()
                bkConfigFile.writeText(YamlUtil.toYaml(dataMap), Charset.forName(Charsets.UTF_8.name()))
            }
        }
        return Result(true)
    }
}
