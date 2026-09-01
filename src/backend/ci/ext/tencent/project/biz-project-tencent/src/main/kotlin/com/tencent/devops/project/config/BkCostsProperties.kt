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

package com.tencent.devops.project.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * bkCosts 货币化相关配置
 *
 * 配置示例:
 * ```yaml
 * bk:
 *   costs:
 *     bgId:
 *       - 956
 *     businessLineId:
 *       - 57238
 *     deptId:
 *       - 55767
 *       - 56519
 *     centerId:
 *       - 12345
 *     excludeDeptId:
 *       - 56494
 * ```
 */
@Component
@ConfigurationProperties(prefix = "bk.costs")
class BkCostsProperties {
    /**
     * 需要货币化的事业群ID列表
     */
    var bgId: List<String> = emptyList()

    /**
     * 需要货币化的业务线ID列表
     */
    var businessLineId: List<String> = emptyList()

    /**
     * 需要货币化的部门ID列表
     */
    var deptId: List<String> = emptyList()

    /**
     * 需要货币化的中心ID列表
     */
    var centerId: List<String> = emptyList()

    /**
     * 排除的部门ID列表（在此列表中的不进行货币化）
     */
    var excludeDeptId: List<String> = emptyList()
}
