package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModelWithId
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.apiquery.vo.CheckerDetailVO

interface ICheckerService {

    /**
     * 根据规则集类型查询规则详情
     * @param checkerSetType
     * @return
     */
    fun queryCheckerDetail(checkerSetType: String): List<CheckerDetailModel>

    /**
     * 根据规则集id和版本查询规则详情清单
     */
    fun queryCheckerDetailByCheckerSetId(
        checkerSetId: String,
        version: Int?
    ): List<CheckerDetailModel>

    fun listCheckerDetail(appCode: String, pageNum: Int?, pageSize: Int?, toolName: String?, checkerKey: Set<String>?): List<CheckerDetailModelWithId>

    /**
     * 查询打开的规则
     *
     * @param taskId
     * @param toolName
     * @param paramJson
     * @param codeLang
     * @return
     */
    fun queryAllChecker(
        taskId: Long,
        toolName: String,
        paramJson: String?,
        codeLang: Long
    ): List<CheckerDetailVO>

    /**
     * 查询打开的规则
     *
     * @param taskId
     * @param toolName
     * @return
     */
    fun queryOpenCheckers(
        taskId: Long,
        toolName: String,
        paramJson: String?,
        codeLang: Long
    ): Map<String, CheckerDetailVO>

    /**
     * 查询指定规则包的真实规则名（工具可识别的规则名）
     *
     * @param pkgId
     * @param taskInfoModel
     * @return
     */
    fun queryPkgRealCheckers(pkgId: String?, toolName: String, taskInfoModel: TaskInfoModel): Set<String>

    /**
     * 根据规则集id和版本查询规则集
     */
    fun findByCheckerSetIdAndVersion(checkerSetId: String, version: Int): CheckerSetModel

    /**
     * 查询任务的规则配置
     *
     * @param analyzeConfigInfoVO 告警配置详情
     * @return
     */
    fun getTaskCheckerConfig(analyzeConfigInfoVO: AnalyzeConfigInfoVO): AnalyzeConfigInfoVO

    /**
     * 查询任务配置的圈复杂度阀值
     *
     * @param toolConfigInfoModel 工具配置信息
     */
    fun getCcnThreshold(toolConfigInfoModel: ToolConfigInfoModel): Int

    /**
     * 通过规则集id清单查询规则集信息
     */
    fun getLastestVersionByCheckerSetId(checkerSetIds: Set<String>) : List<CheckerSetModel>
}
