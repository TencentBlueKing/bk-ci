package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.CheckerDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CheckerSetDao
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.bk.codecc.apiquery.task.dao.BaseDataDao
import com.tencent.devops.common.constant.ComConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CheckerServiceImpl @Autowired constructor(
    private val checkerDao: CheckerDao,
    private val checkerSetDao: CheckerSetDao,
    private val baseDataDao: BaseDataDao
) : ICheckerService {

    companion object {
        private val logger = LoggerFactory.getLogger(CheckerServiceImpl::class.java)
    }

    override fun queryCheckerDetail(
        checkerSetType: String
    ): List<CheckerDetailModel> {
        // 查询语言参数列表
        val codeLangParams = baseDataDao.findByParamType(ComConstants.KEY_CODE_LANG)
        val checkerDetailModelList = mutableListOf<CheckerDetailModel>()
        codeLangParams.forEach { baseDataModel ->
            if (baseDataModel.openSourceCheckerSets.isNotEmpty()) {
                // 获取符合查询类型的规则集
                val openSourceCheckerSets = baseDataModel.openSourceCheckerSets
                    .filter { openSourceCheckerSet -> checkerSetType.equals(openSourceCheckerSet.checkerSetType, ignoreCase = true) }
                    .toList()
                openSourceCheckerSets.forEach { openSourceCheckerSet ->
                    // 查询规则集
                    val checkerSetModel = if (null == openSourceCheckerSet.version) {
                        checkerSetDao.findLatestVersionByCheckerSetId(openSourceCheckerSet.checkerSetId)
                    } else {
                        checkerSetDao.findByCheckerSetIdAndVersion(openSourceCheckerSet.checkerSetId, openSourceCheckerSet.version)
                    }
                    // 查询规则详情
                    if (null != checkerSetModel && checkerSetModel.checkerProps.isNotEmpty()) {
                        val toolNameList = checkerSetModel.checkerProps.map { it.toolName }.toList()
                        val checkerKeyList = checkerSetModel.checkerProps.map { it.checkerKey }.toList()
                        checkerDetailModelList.addAll(checkerDao.batchFindByToolNameAndCheckerKey(toolNameList, checkerKeyList))
                    }
                }
            }
        }
        return checkerDetailModelList
    }

}