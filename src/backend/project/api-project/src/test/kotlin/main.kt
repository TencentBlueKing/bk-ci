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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.project.pojo.ProjectVO

fun main(array: Array<String>) {
    val p = ProjectVO(
        id = 890,
        projectId = "job",
        projectName = "定时任务",
        projectCode = "job",
        projectType = 1,
        approvalStatus = 1,
        approvalTime = "2019-02-12",
        approver = "fgg",
        ccAppId = 123,
        ccAppName = "XXG",
        createdAt = "2019-02-12",
        creator = "ggg",
        dataId = 768,
        deployType = "n",
        updatedAt = "2019-02-12",
        bgId = 6867,
        bgName = "ssd",
        centerId = 87987,
        centerName = "fghfg",
        deptId = 2,
        deptName = "",
        description = "job project",
        englishName = "job",
        extra = "",
        isOfflined = true,
        isSecrecy = true,
        isHelmChartEnabled = true,
        kind = 1,
        logoAddr = "",
        remark = "",
        useBk = true,
        gray = false,
        enabled = true,
        enableExternal = true
    )
    val message = JsonUtil.toJson(p)
    println(message)
    val to = JsonUtil.to(message, object : TypeReference<ProjectVO>() {})
    println(to)
}