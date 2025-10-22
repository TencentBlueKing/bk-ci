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

package com.tencent.devops.misc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Component

@Component
@RefreshScope
class MiscBuildDataClearConfig {

    @Value("\${build.data.clear.switch:false}")
    val switch: String = "false"

    @Value("\${build.data.clear.maxEveryProjectHandleNum:5}")
    val maxEveryProjectHandleNum: Int = 5

    @Value("\${build.data.clear.monthRange:-1}")
    val bsMonthRange: Int = -1

    @Value("\${build.data.clear.maxKeepNum:10000}")
    val bsMaxKeepNum: Int = 10000

    @Value("\${build.data.clear.codeccDayRange:-14}")
    val codeccDayRange: Int = -14

    @Value("\${build.data.clear.codeccMaxKeepNum:14}")
    val codeccMaxKeepNum: Int = 14

    @Value("\${build.data.clear.otherMonthRange:-1}")
    val otherMonthRange: Int = -1

    @Value("\${build.data.clear.otherMaxKeepNum:500}")
    val otherMaxKeepNum: Int = 500

    @Value("\${build.data.clear.clearChannelCodes:BS}")
    val clearChannelCodes: String = "BS"

    @Value("\${build.data.clear.maxThreadHandleProjectNum:5}")
    val maxThreadHandleProjectNum: Int = 5
}
