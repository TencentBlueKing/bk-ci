package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.process.api.service.ServiceMeasurePipelineResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.ExtServiceItem
import com.tencent.devops.store.pojo.ExtServiceStatistic
import com.tencent.devops.store.pojo.atom.MarketMainItemLabel
import com.tencent.devops.store.pojo.atom.enums.AtomCategoryEnum
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.jooq.DSLContext
import org.jooq.Record4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ExtServiceSearchService @Autowired constructor(
    val dslContext: DSLContext,
    val extServiceDao: ExtServiceDao,
    val storeUserService: StoreUserService,
    val storeVisibleDeptService: StoreVisibleDeptService,
    val storeMemberService: TxExtServiceMemberImpl,
    val classifyService: ClassifyService,
    val storeStatisticDao: StoreStatisticDao,
    val client: Client
) {
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<ExtServiceMainItemVo>> {
        val result = mutableListOf<ExtServiceMainItemVo>()
        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("[list]get userDeptList[$userDeptList]")
        val labelInfoList = mutableListOf<MarketMainItemLabel>()
        // 最新标签
        labelInfoList.add(MarketMainItemLabel(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
        // 最后标签
        labelInfoList.add(MarketMainItemLabel(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
        val futureList = mutableListOf<SearchExtServiceVO>()
        val classifyList = extServiceDao.getAllServiceClassify(dslContext)
        classifyList?.forEach {
            val classifyCode = it["classifyCode"] as String
            if (classifyCode != "trigger") {
                val classifyName = it["classifyName"] as String
                val classifyLanName = MessageCodeUtil.getCodeLanMessage(
                    messageCode = "${StoreMessageCode.MSG_CODE_STORE_CLASSIFY_PREFIX}$classifyCode",
                    defaultMessage = classifyName
                )
                labelInfoList.add(MarketMainItemLabel(classifyCode, classifyLanName))
                futureList.add(
                    doList(
                        userId = userId,
                        userDeptList = userDeptList,
                        classifyCode = classifyCode,
                        serviceName = null,
                        labelCode = null,
                        score = null,
                        sortType = ExtServiceSortTypeEnum.DOWNLOAD_COUNT,
                        desc = true,
                        page = page,
                        pageSize = pageSize
                    )
                )
            }
        }
        for (index in futureList.indices) {
            val labelInfo = labelInfoList[index]
            result.add(
                ExtServiceMainItemVo(
                    key = labelInfo.key,
                    label = labelInfo.label,
                    records = futureList[index].records
                )
            )
        }
        return Result(result)
    }

    /**
     * 插件市场，查询插件列表
     */
    fun list(
        userId: String,
        serviceName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        sortType: ExtServiceSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): SearchExtServiceVO {

        // 获取用户组织架构
        val userDeptList = storeUserService.getUserDeptList(userId)
        logger.info("[list]get userDeptList:$userDeptList")

        return doList(
            userId = userId,
            userDeptList = userDeptList,
            serviceName = serviceName,
            classifyCode = classifyCode,
            labelCode = labelCode,
            score = score,
            sortType = sortType,
            desc = true,
            page = page,
            pageSize = pageSize
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        userId: String,
        userDeptList: List<Int>,
        serviceName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        sortType: ExtServiceSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): SearchExtServiceVO {
        val results = mutableListOf<ExtServiceItem>()
        // 获取插件
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
        val count = extServiceDao.count(dslContext, serviceName, classifyCode, labelCodeList, score)
        val services = extServiceDao.list(
            dslContext,
            serviceName,
            classifyCode,
            labelCodeList,
            score,
            sortType,
            desc,
            page,
            pageSize
        ) ?: return SearchExtServiceVO(0, page, pageSize, results)
        logger.info("[list]get services: $services")

        val serviceCodeList = services.map {
            it["SERVICE_ID"] as String
        }.toList()
        // 获取可见范围
        val serviceVisibleData = storeVisibleDeptService.batchGetVisibleDept(serviceCodeList, StoreTypeEnum.SERVICE).data
        logger.info("[list]get serviceVisibleData:$serviceVisibleData")
        // 获取热度
        val statField = mutableListOf<String>()
        statField.add("DOWNLOAD")
        val serviceStatisticData = getStatisticByCodeList(serviceCodeList, statField).data
        // 获取用户
        val memberData = storeMemberService.batchListMember(serviceCodeList, StoreTypeEnum.SERVICE).data

        // 获取分类
        val classifyList = classifyService.getAllClassify(StoreTypeEnum.SERVICE.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        services.forEach {
            val serviceCode = it["SERVICE_CODE"] as String
            val visibleList = serviceVisibleData?.get(serviceCode)
            val statistic = serviceStatisticData?.get(serviceCode)
            val members = memberData?.get(serviceCode)
            val defaultFlag = it["DEFAULT_FLAG"] as Boolean
            val flag = generateInstallFlag(defaultFlag, members, userId, visibleList, userDeptList)
            val classifyId = it["CLASSIFY_ID"] as String
            results.add(
                ExtServiceItem(
                    id = it["ID"] as String,
                    name = it["NAME"] as String,
                    code = serviceCode,
                    type = it["JOB_TYPE"] as String,
                    classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                    category = AtomCategoryEnum.getAtomCategory((it["CATEGROY"] as Byte).toInt()),
                    logoUrl = it["LOGO_URL"] as? String,
                    publisher = it["PUBLISHER"] as String,
                    downloads = statistic?.downloads ?: 0,
                    score = statistic?.score ?: 0.toDouble(),
                    summary = it["SUMMARY"] as? String,
                    flag = flag,
                    publicFlag = it["DEFAULT_FLAG"] as Boolean,
                    recommendFlag = it["RECOMMEND_FLAG"] as? Boolean
                )
            )
        }

        logger.info("[list]end")
        return SearchExtServiceVO(count, page, pageSize, results)

    }

    fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean {
        logger.info("generateInstallFlag defaultFlag is:$defaultFlag,members is:$members,userId is:$userId")
        logger.info("generateInstallFlag visibleList is:$visibleList,userDeptList is:$userDeptList")
        return if (defaultFlag || (members != null && members.contains(userId))) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    private fun getStatisticByCodeList(serviceCodeList: List<String>, statFiledList: List<String>): Result<HashMap<String, ExtServiceStatistic>> {
        val records = storeStatisticDao.batchGetStatisticByStoreCode(dslContext, serviceCodeList, StoreTypeEnum.SERVICE.type.toByte())
        val serviceCodes = serviceCodeList.joinToString(",")
        val isStatPipeline = statFiledList.contains("PIPELINE")
        val pipelineStat = if (isStatPipeline) {
            client.get(ServiceMeasurePipelineResource::class).batchGetPipelineCountByAtomCode(serviceCodes, null).data
        } else {
            mutableMapOf()
        }
        val serviceStatistic = hashMapOf<String, ExtServiceStatistic>()
        records.map {
            if (it.value4() != null) {
                val serviceCode = it.value4()
                serviceStatistic[serviceCode] = formatAtomStatistic(it, pipelineStat?.get(serviceCode) ?: 0)
            }
        }
        return Result(serviceStatistic)
    }

    private fun formatAtomStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>, pipelineCnt: Int): ExtServiceStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double = if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

        return ExtServiceStatistic(
            downloads = downloads ?: 0,
            commentCnt = comments ?: 0,
            score = String.format("%.1f", averageScore).toDoubleOrNull()
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}