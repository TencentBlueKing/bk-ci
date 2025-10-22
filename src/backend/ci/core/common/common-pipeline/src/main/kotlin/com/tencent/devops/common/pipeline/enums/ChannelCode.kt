/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.pipeline.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "渠道代码")
enum class ChannelCode {
    @Schema(title = "蓝鲸持续集成平台")
    BS,
    @Schema(title = "蓝鲸流水线插件市场")
    AM,
    @Schema(title = "蓝鲸代码检查平台")
    CODECC,
    @Schema(title = "GCloud")
    GCLOUD,
    @Schema(title = "工蜂")
    GIT,
    @Schema(title = "开源扫描")
    GONGFENGSCAN,
    @Schema(title = "codecc回迁")
    CODECC_EE;

    companion object {
        // Only BS need to check the authentication for now
        fun isNeedAuth(channelCode: ChannelCode) =
                channelCode == BS

        // 页面可见channel
        fun webChannel(channelCode: ChannelCode): Boolean {
            return channelCode == BS || channelCode == GIT
        }

        fun getChannel(channel: String): ChannelCode? {
            values().forEach {
                if (it.name == channel) {
                    return it
                }
            }
            return null
        }
    }
}
