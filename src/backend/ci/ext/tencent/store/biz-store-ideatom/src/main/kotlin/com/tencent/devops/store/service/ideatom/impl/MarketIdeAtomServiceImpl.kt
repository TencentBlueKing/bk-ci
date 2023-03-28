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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomClassifyDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomFeatureDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomVersionLogDao
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.IdeAtomDetail
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomMainItem
import com.tencent.devops.store.pojo.ideatom.MarketIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.ideatom.IdeAtomCategoryService
import com.tencent.devops.store.service.ideatom.IdeAtomLabelService
import com.tencent.devops.store.service.ideatom.MarketIdeAtomService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MarketIdeAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketIdeAtomDao: MarketIdeAtomDao,
    private val marketIdeAtomClassifyDao: MarketIdeAtomClassifyDao,
    private val marketIdeAtomFeatureDao: MarketIdeAtomFeatureDao,
    private val marketIdeAtomVersionLogDao: MarketIdeAtomVersionLogDao,
    private val ideAtomDao: IdeAtomDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val classifyService: ClassifyService,
    private val ideAtomCategoryService: IdeAtomCategoryService,
    private val ideAtomLabelService: IdeAtomLabelService,
    private val storeCommentService: StoreCommentService
) : MarketIdeAtomService {

    private val logger = LoggerFactory.getLogger(MarketIdeAtomServiceImpl::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        userId: String,
        keyword: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): MarketIdeAtomResp {
        logger.info("[list]userId=$userId, keyword=$keyword, classifyCode=$classifyCode, " +
                "labelCode=$labelCode, score=$score, page=$page, pageSize=$pageSize")

        val results = mutableListOf<MarketIdeAtomItem>()
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")

        val count = marketIdeAtomDao.count(
                dslContext = dslContext,
                categoryCode = categoryCode,
                keyword = keyword,
                classifyCode = classifyCode,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType
                )
        val atoms = marketIdeAtomDao.list(
                dslContext = dslContext,
                keyword = keyword,
                categoryCode = categoryCode,
                classifyCode = classifyCode,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType,
                sortType = sortType,
                desc = desc,
                page = page,
                pageSize = pageSize
        ) ?: return MarketIdeAtomResp(0, page, pageSize, results)

        val atomCodeList = atoms.map {
            it["ATOM_CODE"] as String
        }.toList()
        val storeType = StoreTypeEnum.IDE_ATOM
        val atomStatisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeType.type.toByte(),
            storeCodeList = atomCodeList
        )

        // 获取分类
        val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        atoms.forEach {
            val atomCode = it["ATOM_CODE"] as String
            val statistic = atomStatisticData[atomCode]
            val classifyId = it["CLASSIFY_ID"] as String
            val pubTime = it["PUB_TIME"] as? LocalDateTime
            results.add(
                    MarketIdeAtomItem(
                            id = it["ID"] as String,
                            name = it["ATOM_NAME"] as String,
                            code = atomCode,
                            rdType = IdeAtomTypeEnum.getAtomTypeObj((it["ATOM_TYPE"] as Byte).toInt()),
                            classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                            logoUrl = it["LOGO_URL"] as? String,
                            summary = it["SUMMARY"] as? String,
                            publisher = it["PUBLISHER"] as String,
                            pubTime = if (pubTime == null) "" else DateTimeUtil.toDateTime(pubTime),
                            latestFlag = it["LATEST_FLAG"] as Boolean,
                            publicFlag = it["PUBLIC_FLAG"] as Boolean,
                            recommendFlag = it["RECOMMEND_FLAG"] as Boolean,
                            flag = true,
                            creator = it["CREATOR"] as String,
                            createTime = DateTimeUtil.toDateTime(it["CREATE_TIME"] as LocalDateTime),
                            modifier = it["MODIFIER"] as String,
                            updateTime = DateTimeUtil.toDateTime(it["UPDATE_TIME"] as LocalDateTime),
                            downloads = statistic?.downloads ?: 0,
                            score = statistic?.score ?: 0.toDouble(),
                            weight = it["WEIGHT"] as? Int
                    )
            )
        }
        return MarketIdeAtomResp(count, page, pageSize, results)
    }

    /**
     * 研发商店，首页
     */
    override fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketIdeAtomMainItem>> {
        logger.info("[mainPageList]userId=$userId, page=$page, pageSize=$pageSize")
        val result = mutableListOf<MarketIdeAtomMainItem>()

        result.add(MarketIdeAtomMainItem(
                key = LATEST,
                label = MessageUtil.getCodeLanMessage(messageCode = LATEST,
                    language = I18nUtil.getLanguage(userId)),
                records = doList(
                        userId = userId,
                        keyword = null,
                        categoryCode = null,
                        classifyCode = null,
                        labelCode = null,
                        score = null,
                        rdType = null,
                        sortType = MarketIdeAtomSortTypeEnum.UPDATE_TIME,
                        desc = true,
                        page = page,
                        pageSize = pageSize
                ).records
        ))

        result.add(MarketIdeAtomMainItem(
                key = HOTTEST,
                label = MessageUtil.getCodeLanMessage(messageCode = HOTTEST,
                    language = I18nUtil.getLanguage(userId)),
                records = doList(
                        userId = userId,
                        keyword = null,
                        categoryCode = null,
                        classifyCode = null,
                        labelCode = null,
                        score = null,
                        rdType = null,
                        sortType = MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT,
                        desc = true,
                        page = page,
                        pageSize = pageSize
                ).records
        ))

        val classifyList = marketIdeAtomClassifyDao.getAllAtomClassify(dslContext)
        classifyList?.forEach {
            val classifyCode = it["classifyCode"] as String
            val classifyName = it["classifyName"] as String
            val classifyLanName = MessageUtil.getCodeLanMessage(
                messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                defaultMessage = classifyName,
                language = I18nUtil.getLanguage(userId)
            )
            result.add(MarketIdeAtomMainItem(
                    key = classifyCode,
                    label = classifyLanName,
                    records = doList(
                            userId = userId,
                            keyword = null,
                            categoryCode = null,
                            classifyCode = classifyCode,
                            labelCode = null,
                            score = null,
                            rdType = null,
                            sortType = MarketIdeAtomSortTypeEnum.DOWNLOAD_COUNT,
                            desc = true,
                            page = page,
                            pageSize = pageSize
                    ).records
            ))
        }

        return Result(result)
    }

    /**
     * 研发商店，查询插件列表
     */
    override fun list(
        userId: String,
        keyword: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): MarketIdeAtomResp {
        logger.info("[list]userId=$userId, keyword=$keyword, classifyCode=$classifyCode, labelCode=$labelCode, " +
                "score=$score, sortType=$sortType, page=$page, pageSize=$pageSize")

        return doList(
                userId = userId,
                keyword = keyword,
                categoryCode = categoryCode,
                classifyCode = classifyCode,
                labelCode = labelCode,
                score = score,
                rdType = rdType,
                sortType = sortType,
                desc = true,
                page = page,
                pageSize = pageSize
        )
    }

    /**
     * 根据插件标识获取插件最新、正式版本信息
     */
    override fun getAtomByCode(userId: String, atomCode: String): Result<IdeAtomDetail?> {
        logger.info("[getAtomByCode]userId=$userId, atomCode=$atomCode")
        val record = ideAtomDao.getLatestAtomByCode(dslContext, atomCode)
                ?: return Result(0, "atomCode=$atomCode, not exist", null)

        val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomCode)
                ?: return Result(0, "atomCode=$atomCode, feature not exist", null)

        val atomId = record["ID"] as String
        val atomReleaseRecord = marketIdeAtomVersionLogDao.getIdeAtomVersion(dslContext, atomId)
        val storeStatistic = storeTotalStatisticService.getStatisticByCode(
            userId = userId,
            storeCode = atomCode,
            storeType = StoreTypeEnum.IDE_ATOM.type.toByte()
        )
        val classify = classifyService.getClassify(record.classifyId).data
        val labelList = ideAtomLabelService.getLabelsByAtomId(atomId).data // 查找标签列表
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.IDE_ATOM)
        val pubTime = record.pubTime
        return Result(IdeAtomDetail(
                atomId = atomId,
                atomCode = record.atomCode,
                atomName = record.atomName,
                logoUrl = record.logoUrl,
                classifyCode = classify?.classifyCode,
                classifyName = classify?.classifyName,
                downloads = storeStatistic.downloads,
                score = storeStatistic.score,
                categoryList = ideAtomCategoryService.getCategorysByAtomId(atomId).data,
                atomType = IdeAtomTypeEnum.getAtomType((atomFeatureRecord.atomType).toInt()),
                summary = record.summary,
                description = record.description,
                version = record.version,
                atomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj(record.atomStatus.toInt())!!,
                releaseType = ReleaseTypeEnum.getReleaseType(atomReleaseRecord.releaseType.toInt()),
                versionContent = atomReleaseRecord.content,
                codeSrc = atomFeatureRecord.codeSrc,
                publisher = record.publisher,
                pubTime = if (pubTime == null) "" else DateTimeUtil.toDateTime(pubTime),
                latestFlag = record.latestFlag,
                publicFlag = atomFeatureRecord.publicFlag,
                recommendFlag = atomFeatureRecord.recommendFlag,
                flag = true,
                labelList = labelList,
                userCommentInfo = userCommentInfo,
                visibilityLevel = VisibilityLevelEnum.geVisibilityLevelObj(record.visibilityLevel),
                privateReason = record.privateReason,
                creator = record.creator,
                createTime = DateTimeUtil.toDateTime(record.createTime),
                modifier = record.modifier,
                updateTime = DateTimeUtil.toDateTime(record.updateTime)
        ))
    }

    /**
     * 根据插件ID和插件代码判断插件是否存在
     */
    override fun judgeAtomExistByIdAndCode(atomId: String, atomCode: String): Result<Boolean> {
        val count = ideAtomDao.countByIdAndCode(dslContext, atomId, atomCode)
        if (count < 1) {
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf("atomId:$atomId,atomCode:$atomCode"),
                data = false,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        }
        return Result(true)
    }
}
