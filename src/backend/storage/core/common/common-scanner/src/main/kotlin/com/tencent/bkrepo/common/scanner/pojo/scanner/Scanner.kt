/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.scanner.pojo.scanner

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ArrowheadScanner
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.slf4j.LoggerFactory
import kotlin.math.max

@ApiModel("扫描器配置")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ArrowheadScanner::class, name = ArrowheadScanner.TYPE)
)
open class Scanner(
    @ApiModelProperty("扫描器名")
    open val name: String,
    @ApiModelProperty("扫描器类型")
    val type: String,
    @ApiModelProperty("扫描器版本")
    open val version: String,
    @ApiModelProperty("最大允许的1MB文件扫描时间")
    val maxScanDurationPerMb: Long = DEFAULT_MAX_SCAN_DURATION
) {
    /**
     * 获取待扫描文件最大允许扫描时长
     *
     * @param size 待扫描文件大小
     */
    open fun maxScanDuration(size: Long): Long {
        val sizeMib = size / 1024L / 1024L
        if (sizeMib == 0L) {
            return DEFAULT_MIN_SCAN_DURATION
        }
        val maxScanDuration = if (Long.MAX_VALUE / sizeMib > maxScanDurationPerMb) {
            maxScanDurationPerMb * sizeMib
        } else {
            logger.warn("file too large size[$size]")
            throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID)
        }
        return max(DEFAULT_MIN_SCAN_DURATION, maxScanDuration)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Scanner::class.java)
        private const val DEFAULT_MAX_SCAN_DURATION = 6 * 1000L
        /**
         * 默认至少允许扫描的时间
         */
        private const val DEFAULT_MIN_SCAN_DURATION = 3 * 60L * 1000L
    }
}
