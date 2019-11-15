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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.api.pojo.IdValue
import org.slf4j.LoggerFactory

enum class StartType {
    MANUAL,
    TIME_TRIGGER,
    WEB_HOOK,
    SERVICE,
    PIPELINE,
    REMOTE;

    companion object {
        fun toReadableString(type: String, channelCode: ChannelCode?): String {
            return when (type) {
                StartType.MANUAL.name -> "手动"
                StartType.TIME_TRIGGER.name -> "定时"
                StartType.WEB_HOOK.name -> "代码变更"
                StartType.REMOTE.name -> "远程触发"
                StartType.SERVICE.name -> {
                    if (channelCode != null) {
                        if (channelCode == ChannelCode.BS) {
                            "OpenAPI启动"
                        } else {
                            channelCode.name + "启动"
                        }
                    } else {
                        "第三方启动"
                    }
                }
                StartType.PIPELINE.name -> "流水线"
                "" -> ""
                else -> type
            }
        }

        fun toStartType(type: String): StartType {
            values().forEach {
                if (type.equals(it.name, true)) {
                    return it
                }
            }
            logger.warn("Unknown start type($type)")
            return MANUAL
        }

        private val logger = LoggerFactory.getLogger(StartType::class.java)

        fun getStartTypeMap(): List<IdValue> {
            val result = mutableListOf<IdValue>()
            values().forEach {
                result.add(IdValue(it.name, toReadableString(it.name, null)))
            }

            return result
        }
    }
}