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

package com.tencent.devops.process.yaml.modelCreate

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import org.slf4j.LoggerFactory

object ModelCommon {

    private val logger = LoggerFactory.getLogger(ModelCommon::class.java)

    fun getBranchName(ref: String): String {
        return when {
            ref.startsWith("refs/heads/") ->
                ref.removePrefix("refs/heads/")
            ref.startsWith("refs/tags/") ->
                ref.removePrefix("refs/tags/")
            else -> ref
        }
    }

    fun formatVariablesValue(
        value: String?,
        startParams: MutableMap<String, String>
    ): String? {
        if (value == null || value.isEmpty()) {
            return ""
        }
        val settingMap = mutableMapOf<String, String>()
        settingMap.putAll(startParams)
        return ScriptYmlUtils.parseVariableValue(value, settingMap)
    }

    fun installMarketAtom(
        client: Client,
        projectCode: String,
        userId: String,
        atomCode: String,
        channelCode: ChannelCode = ChannelCode.GIT
    ) {
        val projectCodes = ArrayList<String>()
        projectCodes.add(projectCode)
        try {
            client.get(ServiceMarketAtomResource::class).installAtom(
                userId = userId,
                channelCode = channelCode,
                installAtomReq = InstallAtomReq(projectCodes, atomCode)
            )
        } catch (e: Throwable) {
            logger.warn("$projectCode $userId install atom($atomCode) failed, exception:", e)
            // 可能之前安装过，继续执行不退出
        }
    }

    // #7592 支持通过 , 分隔来一次填写多个接收人
    fun parseReceivers(receivers: Collection<String>?): Set<String> {
        if (receivers.isNullOrEmpty()) {
            return emptySet()
        }

        val parseReceivers = mutableSetOf<String>()
        receivers.forEach { re ->
            if (!re.contains(',')) {
                parseReceivers.add(re)
                return@forEach
            }
            parseReceivers.addAll(re.split(",").asSequence().filter { it.isNotBlank() }.map { it.trim() }.toSet())
        }

        return parseReceivers
    }
}
