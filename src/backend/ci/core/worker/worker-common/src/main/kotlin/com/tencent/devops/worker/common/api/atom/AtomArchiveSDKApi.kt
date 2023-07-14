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

package com.tencent.devops.worker.common.api.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.store.pojo.atom.AtomDevLanguageEnvVar
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.StorePkgRunEnvInfo
import com.tencent.devops.worker.common.api.WorkerRestApiSDK
import java.io.File

interface AtomArchiveSDKApi : WorkerRestApiSDK {

    /**
     * 获取插件信息
     */
    fun getAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomStatus: Byte? = null,
        osName: String? = null,
        osArch: String? = null,
        convertOsFlag: Boolean? = true
    ): Result<AtomEnv>

    /**
     * 更新插件执行环境信息
     */
    fun updateAtomEnv(
        projectCode: String,
        atomCode: String,
        atomVersion: String,
        atomEnvRequest: AtomEnvRequest
    ): Result<Boolean>

    /**
     * 获取插件插件敏感信息
     */
    fun getAtomSensitiveConf(atomCode: String): Result<List<SensitiveConfResp>?>

    fun archiveAtom(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    ): String

    fun uploadAtomPkgFile(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    )

    fun uploadAtomStaticFile(
        atomCode: String,
        atomVersion: String,
        file: File,
        destPath: String,
        buildVariables: BuildVariables
    )

    fun downloadAtom(
        projectId: String,
        atomFilePath: String,
        file: File,
        authFlag: Boolean
    )

    fun getAtomDevLanguageEnvVars(
        language: String,
        buildHostType: String,
        buildHostOs: String
    ): Result<List<AtomDevLanguageEnvVar>?>

    fun addAtomDockingPlatforms(
        atomCode: String,
        platformCodes: Set<String>
    ): Result<Boolean>

    fun getStorePkgRunEnvInfo(
        language: String,
        osName: String,
        osArch: String,
        runtimeVersion: String
    ): Result<StorePkgRunEnvInfo?>
}
