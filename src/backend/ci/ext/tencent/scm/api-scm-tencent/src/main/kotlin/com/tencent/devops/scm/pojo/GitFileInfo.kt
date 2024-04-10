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

package com.tencent.devops.scm.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
[
    {
        "id":"23c22934e8b4f901dd264cdd100d2f7c339014f5",
        "name":"GroupMilestoneStateStatistics.java",
        "type":"blob",
        "mode":"100644"
    },
    {
        "id":"30a6e86afd0d641b8e9715a6eab5165a71e56e80",
        "name":"GroupMilestoneStatistics.java",
        "type":"blob",
        "mode":"100644"
    },
    {
        "id":"69756319f6abcc246c11c932a3c07c923800544c",
        "name":"GroupStatisticsById.java",
        "type":"blob",
        "mode":"100644"
    },
    {
        "id":"28bdc11874295c5722276a63502010acb92cd9b6",
        "name":"GroupTypeStatistics.java",
        "type":"blob",
        "mode":"100644"
    },
    {
        "id":"b7899b52f700434959e228532171eebdc8a91bd5",
        "name":"TGitErrorStatistics.java",
        "type":"blob",
        "mode":"100644"
    }
]
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GitFileInfo(
    val id: String,
    val name: String,
    val type: String,
    val mode: Long
)
