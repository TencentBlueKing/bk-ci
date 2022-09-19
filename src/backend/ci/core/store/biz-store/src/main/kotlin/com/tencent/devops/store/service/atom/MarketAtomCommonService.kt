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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.GetAtomConfigResult
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum

interface MarketAtomCommonService {

    fun validateAtomVersion(
        atomRecord: TAtomRecord,
        releaseType: ReleaseTypeEnum,
        osList: ArrayList<String>,
        version: String
    ): Result<Boolean>

    fun validateReleaseType(
        atomId: String,
        atomCode: String,
        version: String,
        releaseType: ReleaseTypeEnum,
        taskDataMap: Map<String, Any>,
        fieldCheckConfirmFlag: Boolean? = false
    )

    fun parseBaseTaskJson(
        taskJsonStr: String,
        projectCode: String,
        atomCode: String,
        version: String,
        userId: String
    ): GetAtomConfigResult

    fun checkEditCondition(
        atomCode: String
    ): Boolean

    fun getNormalUpgradeFlag(atomCode: String, status: Int): Boolean

    fun handleAtomCache(
        atomId: String,
        atomCode: String,
        version: String,
        releaseFlag: Boolean
    )

    fun updateAtomRunInfoCache(
        atomId: String,
        atomName: String? = null,
        jobType: JobTypeEnum? = null,
        buildLessRunFlag: Boolean? = null,
        latestFlag: Boolean? = null,
        props: String? = null
    )

    fun generateInputTypeInfos(props: String?): Map<String, String>?

    fun isPublicAtom(atomCode: String): Boolean

    fun getValidOsNameFlag(atomEnvRequests: List<AtomEnvRequest>): Boolean

    fun getValidOsArchFlag(atomEnvRequests: List<AtomEnvRequest>): Boolean
}
