package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.service.ServiceInfoResource
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.ExtServiceItem
import com.tencent.devops.store.pojo.ExtServiceStatistic
import com.tencent.devops.store.pojo.MarketMainItemService
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
    val client: Client,
    val extServiceDao: ExtServiceDao,
    val extServiceItemRelDao: ExtServiceItemRelDao,
    val storeUserService: StoreUserService,
    val storeVisibleDeptService: StoreVisibleDeptService,
    val storeMemberService: TxExtServiceMemberImpl,
    val classifyService: ClassifyService,
    val storeStatisticDao: StoreStatisticDao
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
        val serviceInfoList = mutableListOf<MarketMainItemService>()
        // 最新标签
        serviceInfoList.add(MarketMainItemService(LATEST, MessageCodeUtil.getCodeLanMessage(LATEST)))
        // 最火标签
        serviceInfoList.add(MarketMainItemService(HOTTEST, MessageCodeUtil.getCodeLanMessage(HOTTEST)))
        val futureList = mutableListOf<SearchExtServiceVO>()

        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                serviceName = null,
                classifyCode = null,
                bkServiceId = null,
                labelCode = null,
                score = null,
                sortType = ExtServiceSortTypeEnum.UPDATE_TIME,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )

        futureList.add(
            doList(
                userId = userId,
                userDeptList = userDeptList,
                serviceName = null,
                classifyCode = null,
                labelCode = null,
                bkServiceId = null,
                score = null,
                sortType = ExtServiceSortTypeEnum.DOWNLOAD_COUNT,
                desc = true,
                page = page,
                pageSize = pageSize
            )
        )

        val bkServiceRecord = client.get(ServiceInfoResource::class).getServiceList(userId).data
        val serviceInfoMap = mutableMapOf<String, String>()
        bkServiceRecord?.forEach {
            serviceInfoMap[it.id.toString()] = it.name
        }

        val bkServiceIdList = extServiceItemRelDao.getBkService(dslContext)
        logger.info("service mainPage bkServiceList: $bkServiceIdList")
        bkServiceIdList.forEach {
            val bkServiceId = it["bkServiceId"] as Long
            val bkServiceName = serviceInfoMap[bkServiceId.toString()] ?: ""
            serviceInfoList.add(MarketMainItemService(bkServiceId.toString(), bkServiceName))
            futureList.add(
                doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    classifyCode = null,
                    serviceName = null,
                    bkServiceId = bkServiceId,
                    labelCode = null,
                    score = null,
                    sortType = ExtServiceSortTypeEnum.DOWNLOAD_COUNT,
                    desc = true,
                    page = page,
                    pageSize = pageSize
                )
            )
        }
        for (index in futureList.indices) {
            val serviceInfo = serviceInfoList[index]
            result.add(
                ExtServiceMainItemVo(
                    key = serviceInfo.key,
                    service = serviceInfo.bkService,
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
        bkServiceId: Long?,
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
            bkServiceId = bkServiceId,
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
        bkServiceId: Long?,
        score: Int?,
        sortType: ExtServiceSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): SearchExtServiceVO {
        val results = mutableListOf<ExtServiceItem>()
        // 获取扩展服务
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
        val count = extServiceDao.count(dslContext, serviceName, classifyCode, bkServiceId, labelCodeList, score)
        logger.info("doList userId[$userId],userDeptList[$userDeptList],serviceName[$serviceName],classifyCode[$classifyCode],labelCode[$labelCode], bkService[$bkServiceId] count[$count]")
        val services = extServiceDao.list(
            dslContext,
            serviceName,
            classifyCode,
            bkServiceId,
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
        val serviceVisibleData =
            storeVisibleDeptService.batchGetVisibleDept(serviceCodeList, StoreTypeEnum.SERVICE).data
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
            val publicFlag = it["PUBLIC_FLAG"] as Boolean
            val members = memberData?.get(serviceCode)
            val flag = generateInstallFlag(publicFlag, members, userId, visibleList, userDeptList)
            val classifyId = it["CLASSIFY_ID"] as String
            results.add(
                ExtServiceItem(
                    id = it["SERVICE_ID"] as String,
                    name = it["SERVICE_NAME"] as String,
                    code = serviceCode,
                    classifyCode = if (classifyMap.containsKey(classifyId)) classifyMap[classifyId] else "",
                    logoUrl = it["LOGO_URL"] as? String,
                    publisher = it["PUBLISHER"] as String,
                    downloads = statistic?.downloads ?: 0,
                    score = statistic?.score ?: 0.toDouble(),
                    summary = it["SUMMARY"] as? String,
                    flag = flag,
                    publicFlag = it["PUBLIC_FLAG"] as Boolean,
                    recommendFlag = it["RECOMMEND_FLAG"] as? Boolean
                )
            )
        }

        logger.info("[list]end")
        return SearchExtServiceVO(count, page, pageSize, results)
    }

    fun generateInstallFlag(
        publicFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean {
        logger.info("generateInstallFlag members is:$members,userId is:$userId")
        logger.info("generateInstallFlag visibleList is:$visibleList,userDeptList is:$userDeptList")
        return if (publicFlag || members != null && members.contains(userId)) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    private fun getStatisticByCodeList(
        serviceCodeList: List<String>,
        statFiledList: List<String>
    ): Result<HashMap<String, ExtServiceStatistic>> {
        val records = storeStatisticDao.batchGetStatisticByStoreCode(
            dslContext,
            serviceCodeList,
            StoreTypeEnum.SERVICE.type.toByte()
        )
        val serviceStatistic = hashMapOf<String, ExtServiceStatistic>()
        records.map {
            if (it.value4() != null) {
                val serviceCode = it.value4()
                serviceStatistic[serviceCode] = formatServiceStatistic(it)
            }
        }
        return Result(serviceStatistic)
    }

    private fun formatServiceStatistic(record: Record4<BigDecimal, BigDecimal, BigDecimal, String>): ExtServiceStatistic {
        val downloads = record.value1()?.toInt()
        val comments = record.value2()?.toInt()
        val score = record.value3()?.toDouble()
        val averageScore: Double =
            if (score != null && comments != null && score > 0 && comments > 0) score.div(comments) else 0.toDouble()

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