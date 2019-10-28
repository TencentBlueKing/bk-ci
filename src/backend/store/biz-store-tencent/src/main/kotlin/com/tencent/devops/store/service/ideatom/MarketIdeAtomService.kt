package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomClassifyDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomFeatureDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomVersionLogDao
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
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
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class MarketIdeAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketIdeAtomDao: MarketIdeAtomDao,
    private val marketIdeAtomClassifyDao: MarketIdeAtomClassifyDao,
    private val marketIdeAtomFeatureDao: MarketIdeAtomFeatureDao,
    private val marketIdeAtomVersionLogDao: MarketIdeAtomVersionLogDao,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomStatisticService: MarketIdeAtomStatisticService,
    private val classifyService: ClassifyService,
    private val ideAtomCategoryService: IdeAtomCategoryService,
    private val ideAtomLabelService: IdeAtomLabelService,
    private val storeCommentService: StoreCommentService
) {
    private val logger = LoggerFactory.getLogger(MarketIdeAtomService::class.java)

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        userId: String,
        ideAtomName: String?,
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
        logger.info("[list]userId=$userId, ideAtomName=$ideAtomName, classifyCode=$classifyCode, " +
                "labelCode=$labelCode, score=$score, page=$page, pageSize=$pageSize")

        val results = mutableListOf<MarketIdeAtomItem>()
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")

        val count = marketIdeAtomDao.count(
                dslContext = dslContext,
                categoryCode = categoryCode,
                ideAtomName = ideAtomName,
                classifyCode = classifyCode,
                labelCodeList = labelCodeList,
                score = score,
                rdType = rdType
                )
        val atoms = marketIdeAtomDao.list(
                dslContext = dslContext,
                ideAtomName = ideAtomName,
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

        logger.info("[list]get atoms: $atoms")

        val atomCodeList = atoms.map {
            it["ATOM_CODE"] as String
        }.toList()
        // 获取热度
        val statField = mutableListOf<String>()
        statField.add("DOWNLOAD")
        val atomStatisticData = marketIdeAtomStatisticService.getStatisticByCodeList(atomCodeList, statField).data
        logger.info("[list]get atomStatisticData")

        // 获取分类
        val classifyList = classifyService.getAllClassify(StoreTypeEnum.IDE_ATOM.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        atoms.forEach {
            val atomCode = it["ATOM_CODE"] as String
            val statistic = atomStatisticData?.get(atomCode)
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

        logger.info("[list]end")
        return MarketIdeAtomResp(count, page, pageSize, results)
    }

    /**
     * 研发商店，首页
     */
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<MarketIdeAtomMainItem>> {
        logger.info("[mainPageList]userId=$userId, page=$page, pageSize=$pageSize")
        val result = mutableListOf<MarketIdeAtomMainItem>()

        result.add(MarketIdeAtomMainItem(
                key = "latest",
                label = "最新",
                records = doList(
                        userId = userId,
                        ideAtomName = null,
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
                key = "hottest",
                label = "最热",
                records = doList(
                        userId = userId,
                        ideAtomName = null,
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
            result.add(MarketIdeAtomMainItem(
                    key = classifyCode,
                    label = it["classifyName"] as String,
                    records = doList(
                            userId = userId,
                            ideAtomName = null,
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
    fun list(
        userId: String,
        atomName: String?,
        categoryCode: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): MarketIdeAtomResp {
        logger.info("[list]userId=$userId, atomName=$atomName, classifyCode=$classifyCode, labelCode=$labelCode, " +
                "score=$score, sortType=$sortType, page=$page, pageSize=$pageSize")

        return doList(
                userId = userId,
                ideAtomName = atomName,
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
    fun getAtomByCode(userId: String, atomCode: String): Result<IdeAtomDetail?> {
        logger.info("[getAtomByCode]userId=$userId, atomCode=$atomCode")
        val record = ideAtomDao.getLatestAtomByCode(dslContext, atomCode)
                ?: return Result(0, "atomCode=$atomCode, not exist", null)

        val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomCode)
                ?: return Result(0, "atomCode=$atomCode, feature not exist", null)

        val atomId = record["ID"] as String
        val atomReleaseRecord = marketIdeAtomVersionLogDao.getIdeAtomVersion(dslContext, atomId)

        val classify = classifyService.getClassify(record.classifyId).data
        val labelList = ideAtomLabelService.getLabelsByAtomId(atomId).data // 查找标签列表
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, atomCode, StoreTypeEnum.IDE_ATOM)
        val pubTime = record.pubTime
        return Result(IdeAtomDetail(
                atomId = atomId,
                atomCode = record.atomCode,
                atomName = record.atomName,
                logoUrl = record.logoUrl,
                classifyCode = classify!!.classifyCode,
                classifyName = classify.classifyName,
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
    fun judgeAtomExistByIdAndCode(atomId: String, atomCode: String): Result<Boolean> {
        logger.info("the atomId is:$atomId, atomCode is:$atomCode")
        val count = ideAtomDao.countByIdAndCode(dslContext, atomId, atomCode)
        if (count < 1) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("atomId:$atomId,atomCode:$atomCode"), false)
        }
        return Result(true)
    }
}