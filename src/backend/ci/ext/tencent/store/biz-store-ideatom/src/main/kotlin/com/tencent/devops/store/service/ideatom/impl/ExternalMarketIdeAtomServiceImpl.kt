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
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.IdeAtomEnvInfoDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomDao
import com.tencent.devops.store.pojo.common.enums.StoreOperationTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import com.tencent.devops.store.service.common.OperationLogService
import com.tencent.devops.store.service.common.StoreTotalStatisticService
import com.tencent.devops.store.service.ideatom.ExternalMarketIdeAtomService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExternalMarketIdeAtomServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomDao: MarketIdeAtomDao,
    private val ideAtomEnvInfoDao: IdeAtomEnvInfoDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val storeTotalStatisticService: StoreTotalStatisticService,
    private val operationLogService: OperationLogService
) : ExternalMarketIdeAtomService {
    private val logger = LoggerFactory.getLogger(ExternalMarketIdeAtomServiceImpl::class.java)

    @Value("\${gateway.devnet.url}")
    private lateinit var devnetGatewayUrl: String

    @Value("\${gateway.idc.url}")
    private lateinit var idcGatewayUrl: String

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        categoryCode: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): ExternalIdeAtomResp {
        logger.info("[list]categoryCode=$categoryCode, keyword=$keyword, classifyCode=$classifyCode, " +
                "labelCode=$labelCode, score=$score, page=$page, pageSize=$pageSize")

        val results = mutableListOf<ExternalIdeAtomItem>()
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")

        val count = marketIdeAtomDao.count(
                dslContext = dslContext,
                keyword = keyword,
                categoryCode = categoryCode,
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
        ) ?: return ExternalIdeAtomResp(0, page, pageSize, results)

        val atomCodeList = atoms.map {
            it["ATOM_CODE"] as String
        }.toList()
        val storeType = StoreTypeEnum.IDE_ATOM
        val atomStatisticData = storeTotalStatisticService.getStatisticByCodeList(
            storeType = storeType.type.toByte(),
            storeCodeList = atomCodeList
        )

        atoms.forEach {
            val atomCode = it["ATOM_CODE"] as String
            val statistic = atomStatisticData[atomCode]
            results.add(
                    ExternalIdeAtomItem(
                            atomId = it["ID"] as String,
                            atomName = it["ATOM_NAME"] as String,
                            atomCode = atomCode,
                            version = it["VERSION"] as String,
                            logoUrl = it["LOGO_URL"] as? String,
                            summary = it["SUMMARY"] as? String,
                            publisher = it["PUBLISHER"] as String,
                            downloads = statistic?.downloads ?: 0,
                            score = statistic?.score ?: 0.toDouble(),
                            codeSrc = it["CODE_SRC"] as? String,
                            weight = it["WEIGHT"] as? Int
                    )
            )
        }
        return ExternalIdeAtomResp(count, page, pageSize, results)
    }

    /**
     * 查询插件列表
     */
    override fun list(
        categoryCode: String,
        atomName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): ExternalIdeAtomResp {
        logger.info("[list]categoryCode=$categoryCode, atomName=$atomName, classifyCode=$classifyCode, labelCode=$labelCode, " +
                "score=$score, sortType=$sortType, page=$page, pageSize=$pageSize")

        return doList(
                keyword = atomName,
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

    override fun installIdeAtom(installIdeAtomReq: InstallIdeAtomReq): Result<InstallIdeAtomResp?> {
        logger.info("installIdeAtom installIdeAtomReq is:$installIdeAtomReq")
        val atomCode = installIdeAtomReq.atomCode
        val atomRecord = ideAtomDao.getLatestReleaseAtomByCode(dslContext, atomCode)
            ?: return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode),
                data = null,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, atomRecord.id)
        // 更新安装量
        storeStatisticDao.updateDownloads(
            dslContext = dslContext,
            userId = "",
            storeId = atomRecord.id,
            storeCode = atomCode,
            storeType = StoreTypeEnum.IDE_ATOM.type.toByte(),
            increment = 1
        )
        operationLogService.add(
            storeCode = atomCode,
            storeType = StoreTypeEnum.IDE_ATOM,
            optType = StoreOperationTypeEnum.INSTALL_IDE_PLUGIN,
            optDesc = "",
            optUser = installIdeAtomReq.userName)
        val pkgPath = atomEnvInfoRecord?.pkgPath
        return Result(InstallIdeAtomResp(
            atomFileDevnetUrl = if (null == pkgPath) null else "$devnetGatewayUrl$IDE_ATOM_CONTEXT_NAME/$pkgPath",
            atomFileIdcUrl = if (null == pkgPath) null else "$idcGatewayUrl$IDE_ATOM_CONTEXT_NAME/$pkgPath"
        ))
    }

    companion object {
        private const val IDE_ATOM_CONTEXT_NAME = "ide-plugin"
    }
}
