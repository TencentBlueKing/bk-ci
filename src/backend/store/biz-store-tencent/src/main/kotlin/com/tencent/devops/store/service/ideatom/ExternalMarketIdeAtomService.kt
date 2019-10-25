package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.IdeAtomEnvInfoDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomItem
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExternalMarketIdeAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomDao: MarketIdeAtomDao,
    private val ideAtomEnvInfoDao: IdeAtomEnvInfoDao,
    private val storeStatisticDao: StoreStatisticDao,
    private val marketIdeAtomStatisticService: MarketIdeAtomStatisticService
) {
    private val logger = LoggerFactory.getLogger(ExternalMarketIdeAtomService::class.java)

    @Value("\${gateway.devnet.url}")
    private lateinit var devnetGatewayUrl: String

    @Value("\${gateway.idc.url}")
    private lateinit var idcGatewayUrl: String

    @Suppress("UNCHECKED_CAST")
    private fun doList(
        categoryCode: String,
        ideAtomName: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): ExternalIdeAtomResp {
        logger.info("[list]categoryCode=$categoryCode, ideAtomName=$ideAtomName, classifyCode=$classifyCode, " +
                "labelCode=$labelCode, score=$score, page=$page, pageSize=$pageSize")

        val results = mutableListOf<ExternalIdeAtomItem>()
        val labelCodeList = if (labelCode.isNullOrEmpty()) listOf() else labelCode?.split(",")

        val count = marketIdeAtomDao.count(
                dslContext = dslContext,
                ideAtomName = ideAtomName,
                categoryCode = categoryCode,
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
        ) ?: return ExternalIdeAtomResp(0, page, pageSize, results)

        logger.info("[list]get atoms: $atoms")

        val atomCodeList = atoms.map {
            it["ATOM_CODE"] as String
        }.toList()
        // 获取热度
        val statField = mutableListOf<String>()
        statField.add("DOWNLOAD")
        val atomStatisticData = marketIdeAtomStatisticService.getStatisticByCodeList(atomCodeList, statField).data
        logger.info("[list]get atomStatisticData")

        atoms.forEach {
            val atomCode = it["ATOM_CODE"] as String
            val statistic = atomStatisticData?.get(atomCode)
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

        logger.info("[list]end")
        return ExternalIdeAtomResp(count, page, pageSize, results)
    }

    /**
     * 查询插件列表
     */
    fun list(
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

    fun installIdeAtom(installIdeAtomReq: InstallIdeAtomReq): Result<InstallIdeAtomResp?> {
        logger.info("installIdeAtom installIdeAtomReq is:$installIdeAtomReq")
        val atomCode = installIdeAtomReq.atomCode
        val atomRecord = ideAtomDao.getLatestReleaseAtomByCode(dslContext, atomCode)
        logger.info("atomRecord is:$atomRecord")
        if (null == atomRecord) {
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(atomCode), null)
        }
        val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, atomRecord.id)
        logger.info("atomEnvInfoRecord is:$atomEnvInfoRecord")
        // 更新安装量
        storeStatisticDao.updateDownloads(dslContext, "", atomRecord.id, atomCode, StoreTypeEnum.IDE_ATOM.type.toByte(), 1)
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