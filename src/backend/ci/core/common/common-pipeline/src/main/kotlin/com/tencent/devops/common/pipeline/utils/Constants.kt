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

package com.tencent.devops.common.pipeline.utils

/**
 * 流水线设置-最大排队数量-默认值
 */
const val PIPELINE_SETTING_MAX_QUEUE_SIZE_DEFAULT = 10

/**
 * 流水线设置-最大并发数量-最大值
 */
const val PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_MAX = 200

/**
 * 流水线设置-最大排队时间-默认值 单位:分钟
 */
const val PIPELINE_SETTING_WAIT_QUEUE_TIME_MINUTE_DEFAULT = 10

/**
 * 流水线设置-CONCURRENCY GROUP 并发组-默认值
 */
const val PIPELINE_SETTING_CONCURRENCY_GROUP_DEFAULT = "\${{ci.pipeline_id}}"

/**
 * 保存流水线编排的最大个数
 */
const val PIPELINE_RES_NUM_MIN = 50
