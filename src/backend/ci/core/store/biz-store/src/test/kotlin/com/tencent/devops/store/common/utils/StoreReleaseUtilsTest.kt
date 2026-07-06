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

package com.tencent.devops.store.common.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.pojo.common.KEY_INSTALL_PARAMS
import com.tencent.devops.store.pojo.common.KEY_INSTALL_PATH
import com.tencent.devops.store.pojo.common.KEY_INSTALL_TYPE
import com.tencent.devops.store.pojo.common.enums.StoreInstallTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StoreReleaseUtilsTest {

    @Test
    fun `validateDeployExtInfo passes with valid devx deploy fields`() {
        Assertions.assertDoesNotThrow {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf(
                    KEY_INSTALL_TYPE to StoreInstallTypeEnum.SILENT.name,
                    KEY_INSTALL_PARAMS to "/S /D=C:\\Program Files\\App"
                ),
                extBaseFeatureInfo = mapOf(KEY_INSTALL_PATH to "C:\\Program Files\\App"),
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo passes when all deploy fields absent`() {
        Assertions.assertDoesNotThrow {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf("otherField" to "value"),
                extBaseFeatureInfo = null,
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo accepts every defined install type enum value`() {
        StoreInstallTypeEnum.entries.forEach { installType ->
            Assertions.assertDoesNotThrow {
                StoreReleaseUtils.validateDeployExtInfo(
                    extBaseInfo = mapOf(KEY_INSTALL_TYPE to installType.name),
                    extBaseFeatureInfo = null,
                    storeType = StoreTypeEnum.DEVX
                )
            }
        }
    }

    @Test
    fun `validateDeployExtInfo rejects unknown install type`() {
        Assertions.assertThrows(ErrorCodeException::class.java) {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf(KEY_INSTALL_TYPE to "UNKNOWN"),
                extBaseFeatureInfo = null,
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo rejects non-string install type`() {
        Assertions.assertThrows(ErrorCodeException::class.java) {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf(KEY_INSTALL_TYPE to 1),
                extBaseFeatureInfo = null,
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo rejects install params with command injection chars`() {
        listOf(
            "/S && rm -rf /",
            "/S; shutdown",
            "/S | cat",
            "/S `whoami`",
            "/S \$(whoami)",
            "/S \${PATH}"
        ).forEach { dangerousParams ->
            Assertions.assertThrows(ErrorCodeException::class.java, {
                StoreReleaseUtils.validateDeployExtInfo(
                    extBaseInfo = mapOf(KEY_INSTALL_PARAMS to dangerousParams),
                    extBaseFeatureInfo = null,
                    storeType = StoreTypeEnum.DEVX
                )
            }, "expected rejection for params: $dangerousParams")
        }
    }

    @Test
    fun `validateDeployExtInfo rejects too long install params`() {
        Assertions.assertThrows(ErrorCodeException::class.java) {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf(KEY_INSTALL_PARAMS to "a".repeat(1025)),
                extBaseFeatureInfo = null,
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo rejects install path with illegal chars`() {
        Assertions.assertThrows(ErrorCodeException::class.java) {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = null,
                extBaseFeatureInfo = mapOf(KEY_INSTALL_PATH to "C:\\App<>|*?"),
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo rejects too long install path`() {
        Assertions.assertThrows(ErrorCodeException::class.java) {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = null,
                extBaseFeatureInfo = mapOf(KEY_INSTALL_PATH to "a".repeat(513)),
                storeType = StoreTypeEnum.DEVX
            )
        }
    }

    @Test
    fun `validateDeployExtInfo skips validation for non-devx store type`() {
        Assertions.assertDoesNotThrow {
            StoreReleaseUtils.validateDeployExtInfo(
                extBaseInfo = mapOf(
                    KEY_INSTALL_TYPE to "UNKNOWN",
                    KEY_INSTALL_PARAMS to "/S && rm -rf /"
                ),
                extBaseFeatureInfo = mapOf(KEY_INSTALL_PATH to "C:\\App<>|"),
                storeType = StoreTypeEnum.ATOM
            )
        }
    }
}
