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

package com.tencent.devops.dispatch.bcs.pojo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

/**
 * Bcs job 状态信息
 * @param status job 状态
 * @param deleted true代表job已删除，false代表未被删除
 */
data class BcsJobStatus(
    val status: String,
    val deleted: Boolean
)

enum class BcsJobStatusEnum(
    val realName: String,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "bcsJobStatus", reusePrefixFlag = false)
    val message: String
) {
    PENDING("pending", "pending"), // job正在创建
    RUNNING("running", "running"), // job正在运行
    FAILED("failed", "failed"), // job失败
    SUCCEEDED("succeeded", "succeeded"); // job成功

    companion object {
        fun realNameOf(realName: String?): BcsTaskStatusEnum? {
            if (realName.isNullOrBlank()) {
                return null
            }
            return BcsTaskStatusEnum.values().firstOrNull { it.realName == realName }
        }
    }
}

fun BcsJobStatusEnum.isRunning() = this == BcsJobStatusEnum.RUNNING || this == BcsJobStatusEnum.PENDING

fun BcsJobStatusEnum.isSucceeded() = this == BcsJobStatusEnum.SUCCEEDED
