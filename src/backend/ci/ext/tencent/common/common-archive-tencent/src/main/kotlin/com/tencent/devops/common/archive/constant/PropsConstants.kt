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

package com.tencent.devops.common.archive.constant

// Codecc元数据
const val ARCHIVE_PROPS_CODECC_VS_LEAK_COUNT = "codecc.coverity.warning.count"
const val ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_COUNT = "codecc.coverity.warning.high.count"
const val ARCHIVE_PROPS_CODECC_VS_LEAK_HIGH_AND_LIGHT_COUNT = "codecc.coverity.warning.highAndLight.count"

// 漏洞扫描元数据
const val ARCHIVE_PROPS_VS_LEAK_HIGH_COUNT = "vs.leak.high.count"
const val ARCHIVE_PROPS_VS_LEAK_MIDDLE_COUNT = "vs.leak.middle.count"
const val ARCHIVE_PROPS_VS_LEAK_LIGHT_COUNT = "vs.leak.light.count"
const val ARCHIVE_PROPS_VS_LEAK_COUNT = "vs.leak.count"
const val ARCHIVE_PROPS_VS_RISK_COUNT = "vs.risk.count"

// apk加固元数据
const val ARCHIVE_PROPS_APK_SHELL_STATUS = "apk.shell.status"

// 企业签名元数据
const val ARCHIVE_PROPS_IPA_SIGN_STATUS = "ipa.sign.status"
