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

package com.tencent.devops.process.util

import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_USER_NAME
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE

object NotifyTemplateUtils {
    const val COMMON_SHUTDOWN_SUCCESS_CONTENT = "【\${$PROJECT_NAME_CHINESE}】- " +
        "【\${$PIPELINE_NAME}】#\${$PIPELINE_BUILD_NUM} " +
        "执行成功，耗时\${$PIPELINE_TIME_DURATION}, 触发人：\${$PIPELINE_START_USER_NAME}。"
    const val COMMON_SHUTDOWN_FAILURE_CONTENT = "【\${$PROJECT_NAME_CHINESE}】- " +
        "【\${$PIPELINE_NAME}】#\${$PIPELINE_BUILD_NUM} " +
        "执行失败，耗时\${$PIPELINE_TIME_DURATION}, 触发人：\${$PIPELINE_START_USER_NAME}。 "
}
