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

package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.service.ServiceInfoResource
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceItemRelDao
import com.tencent.devops.store.pojo.ExtServiceItem
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

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
    val storeCommonService: StoreCommonService,
    val storeTotalStatisticService: StoreTotalStatisticService
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
        result.add(
            ExtServiceMainItemVo(
                key = LATEST,
                label = MessageCodeUtil.getCodeLanMessage(LATEST),
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    keyword = null,
                    classifyCode = null,
                    bkServiceId = null,
                    labelCode = null,
                    score = null,
                    sortType = ExtServiceSortTypeEnum.UPDATE_TIME,
                    desc = true,
                    page = page,
                    pageSize = pageSize
                ).records
            )
        )
        result.add(
            ExtServiceMainItemVo(
                key = HOTTEST,
                label = MessageCodeUtil.getCodeLanMessage(HOTTEST),
                records = doList(
                    userId = userId,
                    userDeptList = userDeptList,
                    keyword = null,
                    classifyCode = null,
                    labelCode = null,
                    bkServiceId = null,
                    score = null,
                    sortType = ExtServiceSortTypeEnum.DOWNLOAD_COUNT,
                    desc = true,
                    page = page,
                    pageSize = pageSize
                ).records
            )
        )
        val bkServiceRecord = client.get(ServiceInfoResource::class).getServiceList(userId).data
        val serviceInfoMap = mutableMapOf<String, String>()
        bkServiceRecord?.forEach {
            serviceInfoMap[it.id.toString()] = it.name.substringBefore("(")
        }

        val bkServiceIdList = extServiceItemRelDao.getBkService(dslContext)
        logger.info("service mainPage bkServiceList: $bkServiceIdList")
        bkServiceIdList.forEach {
            val bkServiceId = it["bkServiceId"] as Long
            val bkServiceName = serviceInfoMap[bkServiceId.toString()] ?: ""
            result.add(
                ExtServiceMainItemVo(
                    key = bkServiceId.toString(),
                    label = bkServiceName,
                    records = doList(
                        userId = userId,
                        userDeptList = userDeptList,
                        classifyCode = null,
                        keyword = null,
                        bkServiceId = bkServiceId,
                        labelCode = null,
                        score = null,
                        sortType = ExtServiceSortTypeEnum.UPDATE_TIME,
                        desc = true,
                        page = page,
                        pageSize = pageSize
                    ).records
                )
            )
        }
        return Result(result)
    }

    /**
     * 研发商店-扩展服务，查询插件列表
     */
    fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        bkServiceId: Long?,
        score: Int?,
        rdType: ServiceTypeEnum? = null,
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
            keyword = keyword,
            classifyCode = classifyCode,
            labelCode = labelCode,
            bkServiceId = bkServiceId,
            score = score,
            rdType = rdType,
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
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        bkServiceId: Long?,
        score: Int?,
        rdType: ServiceTypeEnum? = null,
        sortType: ExtServiceSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): SearchExtServiceVO {
        val results = mutableListOf<ExtServiceItem>()
        // 获取扩展服务
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")
        val count =
            extServiceDao.count(dslContext, keyword, classifyCode, bkServiceId, rdType, labelCodeList, score)
        logger.info("doList userId[$userId],userDeptList[$userDeptList],keyword[$keyword], rdType[$rdType]," +
            "classifyCode[$classifyCode],labelCode[$labelCode], bkService[$bkServiceId] sortType[$sortType]" +
            " count[$count]")
        val services = extServiceDao.list(
            dslContext = dslContext,
            keyword = keyword,
            classifyCode = classifyCode,
            bkService = bkServiceId,
            labelCodeList = labelCodeList,
            score = score,
            rdType = rdType,
            sortType = sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        ) ?: return SearchExtServiceVO(0, page, pageSize, results)
        logger.info("[list] userId[$userId],userDeptList[$userDeptList],keyword[$keyword],rdType[$rdType]," +
            "bkService[$bkServiceId],labelCode[$labelCode] sortType[$sortType] get services: $services")

        val serviceCodeList = services.map {
            it["SERVICE_CODE"] as String
        }.toList()
        // 获取可见范围
        val storeType = StoreTypeEnum.SERVICE
        val serviceVisibleData =
            storeVisibleDeptService.batchGetVisibleDept(serviceCodeList, storeType).data
        val statisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeType.type.toByte(),
            storeCodeList = serviceCodeList
        )
        // 获取用户
        val memberData = storeMemberService.batchListMember(serviceCodeList, storeType).data

        // 获取分类
        val classifyList = classifyService.getAllClassify(storeType.type.toByte()).data
        val classifyMap = mutableMapOf<String, String>()
        classifyList?.forEach {
            classifyMap[it.id] = it.classifyCode
        }

        services.forEach {
            val serviceCode = it["SERVICE_CODE"] as String
            val visibleList = serviceVisibleData?.get(serviceCode)
            val statistic = statisticData[serviceCode]
            val publicFlag = it["PUBLIC_FLAG"] as Boolean
            val members = memberData?.get(serviceCode)
            val flag = storeCommonService.generateInstallFlag(publicFlag, members, userId, visibleList, userDeptList)
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
                    modifier = it["MODIFIER"] as String,
                    updateTime = DateTimeUtil.toDateTime(it["UPDATE_TIME"] as LocalDateTime),
                    recommendFlag = it["RECOMMEND_FLAG"] as? Boolean
                )
            )
        }
        return SearchExtServiceVO(count, page, pageSize, results)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExtServiceSearchService::class.java)
    }
}
