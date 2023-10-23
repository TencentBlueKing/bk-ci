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
 *
 */

package com.tencent.devops.process.pojo.trigger

import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线事件重放操作信息")
data class PipelineEventReplayInfo(
    @ApiModelProperty("触发人")
    val userId: String
) {
    companion object {
        /**
         * 获取触发信息
         * 若为[重放事件]时，触发人为[回放者],且事件描述采用重放相关文案code
         * 反之返回默认用户和触发文案code
         */
        fun getTriggerInfo(
            replayInfo: PipelineEventReplayInfo?,
            defaultUserId: String,
            defaultI18Code: String
        ): Pair<String, String> {
            return if (replayInfo != null) {
                replayInfo.userId to "$defaultI18Code${WebhookI18nConstants.EVENT_REPLAY_SUFFIX}"
            } else {
                defaultUserId to defaultI18Code
            }
        }
    }
}
