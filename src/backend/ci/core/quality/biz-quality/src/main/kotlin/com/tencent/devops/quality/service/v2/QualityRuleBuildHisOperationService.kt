package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisOperationDao
import com.tencent.devops.quality.pojo.QualityRuleBuildHisOpt
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class QualityRuleBuildHisOperationService constructor(
    private val qualityRuleBuildHisOperationDao: QualityRuleBuildHisOperationDao,
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val dslContext: DSLContext
) {
    fun listQualityRuleBuildOpt(ruleBuildIds: Collection<Long>): List<QualityRuleBuildHisOpt> {
        val ruleBuildHisOpts = qualityRuleBuildHisOperationDao.list(dslContext, ruleBuildIds)
        return ruleBuildHisOpts.map {
            val ruleBuildHisOpt = QualityRuleBuildHisOpt(
                HashUtil.encodeLongId(it.ruleId),
                null,
                it.stageId,
                it.gateOptUser,
                it.gateOptTime.toString()
            )
            ruleBuildHisOpt
        }
    }
}
