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

package com.tencent.devops.store.service.ideatom.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.IdeAtomEnvInfoDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomFeatureDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomVersionLogDao
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.service.ideatom.IdeAtomCategoryService
import com.tencent.devops.store.service.ideatom.IdeAtomLabelService
import com.tencent.devops.store.service.ideatom.IdeAtomService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IdeAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomFeatureDao: MarketIdeAtomFeatureDao,
    private val marketIdeAtomVersionLogDao: MarketIdeAtomVersionLogDao,
    private val ideAtomEnvInfoDao: IdeAtomEnvInfoDao,
    private val atomLabelService: IdeAtomLabelService,
    private val atomCategoryService: IdeAtomCategoryService
) : IdeAtomService {

    override fun getIdeAtomById(atomId: String): Result<IdeAtom?> {
        val atomRecord = ideAtomDao.getIdeAtomById(dslContext, atomId)
        return Result(if (null == atomRecord) {
            null
        } else {
            val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomRecord.atomCode)!!
            val classifyRecord = classifyDao.getClassify(dslContext, atomRecord.classifyId)
            val atomVersionLogRecord = marketIdeAtomVersionLogDao.getIdeAtomVersion(dslContext, atomId)
            val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, atomId)
            val classifyCode = classifyRecord?.classifyCode
            val classifyName = classifyRecord?.classifyName
            val classifyLanName = MessageUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = classifyName,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
            IdeAtom(
                atomId = atomRecord.id,
                atomName = atomRecord.atomName,
                atomCode = atomRecord.atomCode,
                atomType = IdeAtomTypeEnum.getAtomTypeObj(atomFeatureRecord.atomType.toInt()),
                atomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj(atomRecord.atomStatus.toInt())!!,
                classifyCode = classifyCode,
                classifyName = classifyLanName,
                version = atomRecord.version,
                releaseType = ReleaseTypeEnum.getReleaseTypeObj(atomVersionLogRecord.releaseType.toInt())!!,
                versionContent = atomVersionLogRecord.content,
                codeSrc = atomFeatureRecord.codeSrc,
                logoUrl = atomRecord.logoUrl,
                summary = atomRecord.summary,
                description = atomRecord.description,
                publisher = atomRecord.publisher,
                pubTime = if (null != atomRecord.pubTime) DateTimeUtil.toDateTime(atomRecord.pubTime) else null,
                latestFlag = atomRecord.latestFlag,
                publicFlag = atomFeatureRecord.publicFlag,
                recommendFlag = atomFeatureRecord.recommendFlag,
                weight = atomFeatureRecord.weight,
                categoryList = atomCategoryService.getCategorysByAtomId(atomId).data,
                labelList = atomLabelService.getLabelsByAtomId(atomId).data,
                visibilityLevel = VisibilityLevelEnum.geVisibilityLevelObj(atomRecord.visibilityLevel),
                privateReason = atomRecord.privateReason,
                pkgName = atomEnvInfoRecord?.pkgPath?.replace("${atomRecord.atomCode}/${atomRecord.version}/", ""),
                creator = atomRecord.creator,
                createTime = DateTimeUtil.toDateTime(atomRecord.createTime),
                modifier = atomRecord.modifier,
                updateTime = DateTimeUtil.toDateTime(atomRecord.updateTime)
            )
        })
    }

    override fun getIdeAtomByCode(atomCode: String, version: String?): Result<IdeAtom?> {
        val atomRecord = if (null == version) {
            ideAtomDao.getLatestAtomByCode(dslContext, atomCode)
        } else {
            ideAtomDao.getIdeAtom(dslContext, atomCode, version)
        }
        return if (null == atomRecord) {
            Result(data = null)
        } else {
            getIdeAtomById(atomRecord.id)
        }
    }
}
