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

package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.quality.constant.BK_AFTER_POSITION
import com.tencent.devops.quality.constant.BK_BEFORE_POSITION

class ControlPointPosition private constructor(
    val code: String,
    val name: String
) {
    companion object {
        const val BEFORE_POSITION = "BEFORE"
        const val AFTER_POSITION = "AFTER"

        fun create(code: String): ControlPointPosition {
            val name = POSITION_NAME_MAP[code]?.let { I18nUtil.getCodeLanMessage(it) } ?: code
            return ControlPointPosition(code, name)
        }

        private val POSITION_NAME_MAP = mapOf(
            BEFORE_POSITION to BK_BEFORE_POSITION,
            AFTER_POSITION to BK_AFTER_POSITION
        )
    }
}
