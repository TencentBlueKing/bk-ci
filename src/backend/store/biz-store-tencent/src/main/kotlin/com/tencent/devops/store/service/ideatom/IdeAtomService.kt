package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.dao.ideatom.IdeAtomEnvInfoDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomFeatureDao
import com.tencent.devops.store.dao.ideatom.MarketIdeAtomVersionLogDao
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomStatusEnum
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class IdeAtomService @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao,
    private val ideAtomDao: IdeAtomDao,
    private val marketIdeAtomFeatureDao: MarketIdeAtomFeatureDao,
    private val marketIdeAtomVersionLogDao: MarketIdeAtomVersionLogDao,
    private val ideAtomEnvInfoDao: IdeAtomEnvInfoDao,
    private val atomLabelService: IdeAtomLabelService,
    private val atomCategoryService: IdeAtomCategoryService
) {
    private val logger = LoggerFactory.getLogger(IdeAtomService::class.java)

    fun getIdeAtomById(atomId: String): Result<IdeAtom?> {
        logger.info("deleteIdeAtomById atomId is :$atomId")
        val atomRecord = ideAtomDao.getIdeAtomById(dslContext, atomId)
        logger.info("the atomRecord is :$atomRecord")
        return Result(if (null == atomRecord) {
            null
        } else {
            val atomFeatureRecord = marketIdeAtomFeatureDao.getIdeAtomFeature(dslContext, atomRecord.atomCode)!!
            val classifyRecord = classifyDao.getClassify(dslContext, atomRecord.classifyId)
            val atomVersionLogRecord = marketIdeAtomVersionLogDao.getIdeAtomVersion(dslContext, atomId)
            val atomEnvInfoRecord = ideAtomEnvInfoDao.getIdeAtomEnvInfo(dslContext, atomId)
            IdeAtom(
                atomId = atomRecord.id,
                atomName = atomRecord.atomName,
                atomCode = atomRecord.atomCode,
                atomType = IdeAtomTypeEnum.getAtomTypeObj(atomFeatureRecord.atomType.toInt()),
                atomStatus = IdeAtomStatusEnum.getIdeAtomStatusObj(atomRecord.atomStatus.toInt())!!,
                classifyCode = classifyRecord?.classifyCode,
                classifyName = classifyRecord?.classifyName,
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

    fun getIdeAtomByCode(atomCode: String, version: String? = null): Result<IdeAtom?> {
        logger.info("getIdeAtomByCode atomCode is :$atomCode,version is :$version")
        val atomRecord = if (null == version) {
            ideAtomDao.getLatestAtomByCode(dslContext, atomCode)
        } else {
            ideAtomDao.getIdeAtom(dslContext, atomCode, version)
        }
        logger.info("the atomRecord is :$atomRecord")
        return if (null == atomRecord) {
            Result(data = null)
        } else {
            getIdeAtomById(atomRecord.id)
        }
    }
}
