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

package com.tencent.devops.worker.common.task.market

import com.tencent.devops.common.api.constant.GOLANG
import com.tencent.devops.common.api.constant.JAVA
import com.tencent.devops.common.api.constant.NODEJS
import com.tencent.devops.common.api.constant.PYTHON
import com.tencent.devops.worker.common.service.AtomRunConditionHandleService
import com.tencent.devops.worker.common.service.impl.CommonAtomRunConditionHandleServiceImpl
import com.tencent.devops.worker.common.service.impl.GolangAtomRunConditionHandleServiceImpl
import com.tencent.devops.worker.common.service.impl.JavaAtomRunConditionHandleServiceImpl
import com.tencent.devops.worker.common.service.impl.NodeJsAtomRunConditionHandleServiceImpl
import com.tencent.devops.worker.common.service.impl.PythonAtomRunConditionHandleServiceImpl
import java.util.concurrent.ConcurrentHashMap

object AtomRunConditionFactory {

    private val atomRunConditionMap = ConcurrentHashMap<String, AtomRunConditionHandleService>()

    fun createAtomRunConditionHandleService(
        language: String
    ): AtomRunConditionHandleService {
        var atomRunConditionHandleService = atomRunConditionMap[language]
        when (language) {
            JAVA -> {
                if (atomRunConditionHandleService == null) {
                    atomRunConditionHandleService = JavaAtomRunConditionHandleServiceImpl()
                    atomRunConditionMap[language] = atomRunConditionHandleService
                }
            }
            NODEJS -> {
                if (atomRunConditionHandleService == null) {
                    atomRunConditionHandleService = NodeJsAtomRunConditionHandleServiceImpl()
                    atomRunConditionMap[language] = atomRunConditionHandleService
                }
            }
            PYTHON -> {
                if (atomRunConditionHandleService == null) {
                    atomRunConditionHandleService = PythonAtomRunConditionHandleServiceImpl()
                    atomRunConditionMap[language] = atomRunConditionHandleService
                }
            }
            GOLANG -> {
                if (atomRunConditionHandleService == null) {
                    atomRunConditionHandleService = GolangAtomRunConditionHandleServiceImpl()
                    atomRunConditionMap[language] = atomRunConditionHandleService
                }
            }
            else -> {
                if (atomRunConditionHandleService == null) {
                    atomRunConditionHandleService = CommonAtomRunConditionHandleServiceImpl()
                    atomRunConditionMap[language] = atomRunConditionHandleService
                }
            }
        }
        return atomRunConditionHandleService
    }
}
