package com.tencent.bk.codecc.apiquery.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.tencent.bk.codecc.apiquery.defect.dao.CheckerDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CheckerConfigDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.CheckerSetDao
import com.tencent.bk.codecc.apiquery.defect.dao.mongotemplate.IgnoreCheckerDao
import com.tencent.bk.codecc.apiquery.defect.model.CheckerConfigModel
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModelWithId
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel
import com.tencent.bk.codecc.apiquery.defect.model.CovSubcategoryModel
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.bk.codecc.apiquery.service.ICheckerSetBizService
import com.tencent.bk.codecc.apiquery.task.dao.BaseDataDao
import com.tencent.bk.codecc.apiquery.task.model.TaskInfoModel
import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.bk.codecc.apiquery.vo.AnalyzeConfigInfoVO
import com.tencent.bk.codecc.apiquery.vo.CheckerDetailVO
import com.tencent.bk.codecc.apiquery.vo.CheckerProps
import com.tencent.bk.codecc.apiquery.vo.CovSubcategoryVO
import com.tencent.bk.codecc.defect.vo.enums.CheckerSetSource
import com.tencent.bk.codecc.task.vo.OpenCheckerVO
import com.tencent.devops.common.api.checkerset.CheckerPropVO
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CheckerConstants
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.util.JsonUtil
import net.sf.json.JSONObject
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.collections.MapUtils
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import com.tencent.devops.common.util.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Objects
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap

@Service
class CheckerServiceImpl @Autowired constructor(
    private val checkerDao: CheckerDao,
    private val checkerSetDao: CheckerSetDao,
    private val checkerConfigDao: CheckerConfigDao,
    private val ignoreCheckerDao: IgnoreCheckerDao,
    private val iCheckerSetBizService: ICheckerSetBizService,
    private val baseDataDao: BaseDataDao
) : ICheckerService {

    companion object {
        private val logger = LoggerFactory.getLogger(CheckerServiceImpl::class.java)

        // 因为Swift语言的一些规则是默认打开，而其他语言默认不打开，所以针对swift语言，
        // 当用户不打开下面这些规则时，需要将以下规则通过closeDefaultCheckers字段返回给工具侧，让工具侧disable掉
        private val SWIFT_DEFAULT_CHECKERS = Lists.newArrayList("HARDCODED_CREDENTIALS", "RISKY_CRYPTO")
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
                    .filter { openSourceCheckerSet ->
                        checkerSetType.equals(
                            openSourceCheckerSet.checkerSetType,
                            ignoreCase = true
                        )
                    }
                    .toList()
                openSourceCheckerSets.forEach { openSourceCheckerSet ->
                    // 查询规则集
                    val checkerSetModel = if (null == openSourceCheckerSet.version) {
                        checkerSetDao.findLatestVersionByCheckerSetId(openSourceCheckerSet.checkerSetId)
                    } else {
                        checkerSetDao.findByCheckerSetIdAndVersion(
                            openSourceCheckerSet.checkerSetId,
                            openSourceCheckerSet.version
                        )
                    }
                    // 查询规则详情
                    if (null != checkerSetModel && checkerSetModel.checkerProps.isNotEmpty()) {
                        val toolNameList = checkerSetModel.checkerProps.map { it.toolName }.toList()
                        val checkerKeyList = checkerSetModel.checkerProps.map { it.checkerKey }.toList()
                        checkerDetailModelList.addAll(
                            checkerDao.batchFindByToolNameAndCheckerKey(
                                toolNameList,
                                checkerKeyList
                            )
                        )
                    }
                }
            }
        }
        return checkerDetailModelList
    }

    override fun queryCheckerDetailByCheckerSetId(
        checkerSetId: String,
        version: Int?
    ): List<CheckerDetailModel> {
        val checkerSetModel = if (null != version) {
            checkerSetDao.findByCheckerSetIdAndVersion(checkerSetId, version)
        } else {
            checkerSetDao.findLatestVersionByCheckerSetId(checkerSetId)
        }
        return if (null != checkerSetModel && checkerSetModel.checkerProps.isNotEmpty()) {
            val toolAndCheckerMap = checkerSetModel.checkerProps.groupBy({ it.toolName }, { it.checkerKey })
            checkerDao.batchFindCheckerDetailByToolAndCheckerKey(toolAndCheckerMap)
        } else {
            emptyList()
        }
    }

    override fun listCheckerDetail(
        appCode: String,
        pageNum: Int?,
        pageSize: Int?,
        toolName: String?,
        checkerKey: Set<String>?
    ): List<CheckerDetailModelWithId> {
        return checkerDao.listByPage(pageNum, pageSize, toolName, checkerKey).map {
            val item = CheckerDetailModelWithId()
            BeanUtils.copyProperties(it, item)
            item.id = item.toolName + ":" + it.checkerKey
            item
        }
    }

    protected fun filterCheckerDetailByCodeLangAndParamJson(
        toolName: String,
        paramJson: String?,
        codeLang: Long?,
        checkerDetailEntities: List<CheckerDetailModel>
    ): List<CheckerDetailVO> {
        val list = checkerDetailEntities.stream().filter {
            if (it.status == 1) {
                return@filter false
            }
            val parseObj = if (StringUtils.isBlank(paramJson)) JSONObject() else JSONObject.fromObject(paramJson)
            if (ComConstants.Tool.ESLINT.name == toolName) {
                if (parseObj.containsKey("eslint_rc")) {
                    val eslintRc = parseObj.getString("eslint_rc")
                    when {
                        ComConstants.EslintFrameworkType.standard.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.standard.name == it.frameworkType
                        }
                        ComConstants.EslintFrameworkType.vue.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.react.name != it.frameworkType
                        }
                        ComConstants.EslintFrameworkType.react.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.vue.name != it.frameworkType
                        }
                    }
                }
            } else if (ComConstants.Tool.PHPCS.name == toolName) {
                if (parseObj.containsKey("phpcs_standard")) {
                    val phpcsStandard = parseObj.getString("phpcs_standard")
                    if (StringUtils.isNotBlank(phpcsStandard)) {
                        val standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code()
                        return@filter standCode and it.standard != 0
                    }
                }
            } else if (ComConstants.Tool.COVERITY.name == toolName || ComConstants.Tool.KLOCWORK.name == toolName) {
                return@filter (codeLang ?: 0L) and it.language != 0L
            }
            true
        }.collect(Collectors.toList())
        return generateCheckerDetailVo(list)
    }

    private fun generateCheckerDetailVo(entityList: List<CheckerDetailModel>): List<CheckerDetailVO> {
        return entityList.stream().map { CheckerDetailModel ->
            val checkerDetailVO = CheckerDetailVO()
            BeanUtils.copyProperties(CheckerDetailModel, checkerDetailVO)
            if (CollectionUtils.isNotEmpty(CheckerDetailModel.covSubcategory)) {
                checkerDetailVO.covSubcategory = CheckerDetailModel.covSubcategory.stream().map(
                    Function<CovSubcategoryModel, CovSubcategoryVO> { covSubcategoryModel ->
                        val covSubcategoryVO = CovSubcategoryVO()
                        BeanUtils.copyProperties(covSubcategoryModel, covSubcategoryVO)
                        return@Function covSubcategoryVO
                    }).collect(Collectors.toList())
            }
            checkerDetailVO
        }.collect(Collectors.toList())
    }

    /**
     * 获取规则配置参数
     *
     * @param taskId
     * @param toolName
     * @param checkerDetailList
     */
    private fun getCheckerConfigParams(
        taskId: Long,
        toolName: String,
        checkerDetailList: List<CheckerDetailVO>
    ): List<CheckerDetailVO> {
        // 规则参数配置
        val configList = checkerConfigDao.findByTaskIdAndToolName(taskId, toolName)
        val checkerConfigEntityMap = Maps.newHashMap<String, CheckerConfigModel>()
        if (CollectionUtils.isNotEmpty(configList)) {
            for (checkerConfigEntity in configList) {
                checkerConfigEntityMap[checkerConfigEntity.checkerKey] = checkerConfigEntity
            }
        }
        for (checkerVo in checkerDetailList) {
            // 规则是否支持配置
            val config: CheckerConfigModel? = checkerConfigEntityMap[checkerVo.checkerKey]
            if (Objects.isNull(checkerVo.editable) || !checkerVo.editable || config == null) {
                continue
            }
            // 设置配置参数
            checkerVo.props = config.props
            checkerVo.paramValue = config.paramValue
            checkerVo.checkerDesc = config.checkerDesc
        }
        return checkerDetailList
    }

    /**
     * 查询打开的规则
     */
    override fun queryAllChecker(
        taskId: Long,
        toolName: String,
        paramJson: String?,
        codeLang: Long
    ): List<CheckerDetailVO> {
        if (StringUtils.isNotEmpty(paramJson)) {
            paramJson!!.trim { it <= ' ' }
        }
        val checkerDetailEntities: List<CheckerDetailModel> = checkerDao.findByToolName(toolName)

        val checkerDetailList = this.filterCheckerDetailByCodeLangAndParamJson(
            toolName,
            paramJson, codeLang, checkerDetailEntities
        )

        return getCheckerConfigParams(taskId, toolName, checkerDetailList)
    }

    /**
     * 查询打开的规则
     *
     * @param taskId
     * @param toolName
     * @return
     */
    override fun queryOpenCheckers(
        taskId: Long,
        toolName: String,
        paramJson: String?,
        codeLang: Long
    ): Map<String, CheckerDetailVO> {
        val checkerDetailList = queryAllChecker(taskId, toolName, paramJson, codeLang)
        val ignoreChecker = ignoreCheckerDao.findByTaskIdAndToolName(taskId, toolName)

        val openCheckersMap: MutableMap<String, CheckerDetailVO> = Maps.newHashMap()
        if (CollectionUtils.isNotEmpty(checkerDetailList)) {
            for (checkerDetail in checkerDetailList) {
                var isOpen: Boolean
                // 默认规则要判断是否在关闭的默认规则列表中
                if (ComConstants.CheckerPkgKind.DEFAULT.value() == checkerDetail.pkgKind) {
                    isOpen = true
                    if (ignoreChecker != null && CollectionUtils.isNotEmpty(ignoreChecker.closeDefaultCheckers) &&
                        ignoreChecker.closeDefaultCheckers.contains(checkerDetail.checkerKey)
                    ) {
                        isOpen = false
                    }
                } else {
                    isOpen = false
                    if (ignoreChecker != null && CollectionUtils.isNotEmpty(ignoreChecker.openNonDefaultCheckers) &&
                        ignoreChecker.openNonDefaultCheckers.contains(checkerDetail.checkerKey)
                    ) {
                        isOpen = true
                    }
                }
                if (isOpen) {
                    openCheckersMap[checkerDetail.checkerKey] = checkerDetail
                }
            }
        }
        return openCheckersMap
    }

    /**
     * 查询指定规则包的真实规则名（工具可识别的规则名）
     *
     * @param pkgId
     * @param taskInfoModel
     * @return
     */
    override fun queryPkgRealCheckers(pkgId: String?, toolName: String, taskInfoModel: TaskInfoModel): Set<String> {
        val conditionPkgCheckers: MutableSet<String> = Sets.newHashSet()
        if (StringUtils.isNotEmpty(pkgId)) {
            val configInfoModel = taskInfoModel.toolConfigInfoList.firstOrNull {
                toolName.equals(it.toolName, ignoreCase = true)
            } ?: throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)

            val allCheckers = queryOpenCheckers(
                configInfoModel.taskId, configInfoModel.toolName,
                configInfoModel.paramJson, taskInfoModel.codeLang
            )
            if (MapUtils.isNotEmpty(allCheckers)) {
                allCheckers.forEach { (s, checkerDetailVO) ->
                    if (pkgId == checkerDetailVO.pkgKind) {
                        conditionPkgCheckers.add(s)
                    }
                }
            }
        }
        return conditionPkgCheckers
    }

    /**
     * 根据规则集id和版本查询规则集
     */
    override fun findByCheckerSetIdAndVersion(checkerSetId: String, version: Int): CheckerSetModel {
        return checkerSetDao.findByCheckerSetIdAndVersion(checkerSetId, version)
    }

    protected fun filterCheckerDetail(
        toolName: String,
        paramJson: String?,
        codeLang: Long?,
        checkerDetailModels: List<CheckerDetailModel>
    ):
        List<CheckerDetailVO> {
        val list = checkerDetailModels.stream().filter { checkerDetailModel: CheckerDetailModel ->
            if (checkerDetailModel.status == 1) {
                return@filter false
            }
            val parseObj = if (StringUtils.isBlank(
                    paramJson
                )
            ) JSONObject() else JSONObject.fromObject(
                paramJson
            )
            if (ComConstants.Tool.ESLINT.name == toolName) {
                if (parseObj.containsKey("eslint_rc")) {
                    val eslintRc = parseObj.getString("eslint_rc")
                    val frameworkType = checkerDetailModel.frameworkType
                    when {
                        ComConstants.EslintFrameworkType.standard.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.standard.name == frameworkType
                        }
                        ComConstants.EslintFrameworkType.vue.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.react.name != frameworkType
                        }
                        ComConstants.EslintFrameworkType.react.name == eslintRc -> {
                            return@filter ComConstants.EslintFrameworkType.vue.name != frameworkType
                        }
                    }
                }
            } else if (ComConstants.Tool.PHPCS.name == toolName) {
                if (parseObj.containsKey("phpcs_standard")) {
                    val phpcsStandard = parseObj.getString("phpcs_standard")
                    if (StringUtils.isNotBlank(phpcsStandard)) {
                        val standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code()
                        return@filter standCode and checkerDetailModel.standard != 0
                    }
                }
            } else if (ComConstants.Tool.COVERITY.name == toolName || ComConstants.Tool.KLOCWORK.name == toolName) {
                return@filter (codeLang ?: 0L) and checkerDetailModel.language != 0L
            }
            true
        }.collect(Collectors.toList())
        return generateCheckerDetailVo(list)
    }

    protected fun mergeTaskCheckerSets(
        toolName: String,
        checkerSetVOList: List<CheckerSetVO>,
        checkerDetailList: List<CheckerDetailVO>
    ): Map<String, CheckerPropVO?> {

        val checkerDetailMap = checkerDetailList.associateBy { it.checkerKey }
        /*
         * 将所有规则集中的所有规则去重合并成一个当前任务工具打开的规则列表，取各个规则集中的规则的并集，对于重复并且有规则参数配置的规则，合并优先级如下：
         * 1.默认规则集中的规则的参数配置
         * 2.推荐规则集中的规则的参数配置
         * 3.使用率高的规则集中的规则的参数配置
         */
        // 初步合并，保留重复规则的规则集，为下一步判断规则集的优先级做准备
        val preMergeTaskCheckers: MutableMap<String, MutableMap<CheckerSetVO, CheckerPropVO>> = HashMap()
        checkerSetVOList.forEach { checkerSetVO: CheckerSetVO ->
            val checkerSetCheckerPropsList = checkerSetVO.checkerProps
            if (CollectionUtils.isNotEmpty(checkerSetCheckerPropsList)) {
                checkerSetCheckerPropsList.forEach {
                    if (toolName.equals(it.toolName, ignoreCase = true)) {
                        val checkerSetVOCheckerPropVOMap =
                            preMergeTaskCheckers.computeIfAbsent(it.checkerKey) { LinkedHashMap() }
                        val checkerDetailVO = checkerDetailMap[it.checkerKey]
                        if (StringUtils.isEmpty(it.props) && checkerDetailVO != null && StringUtils.isNotEmpty(
                                checkerDetailVO.props
                            )
                        ) {
                            it.props = checkerDetailVO.props
                        }
                        checkerSetVOCheckerPropVOMap[checkerSetVO] = it
                    }
                }
            }
        }
        // 最终合并， 得到规则列表及其规则参数配置，通过一个Map保存
        val mergeTaskCheckers: MutableMap<String, CheckerPropVO?> = HashMap()
        preMergeTaskCheckers.forEach { (checkerKey, checkerSetVOCheckerPropVOMap) ->
            var chooseCheckerPropVO: CheckerPropVO? = null
            var hasRecommended = false
            for ((checkerSetVO, checkerPropVO) in checkerSetVOCheckerPropVOMap) {
                if (null != checkerSetVO.defaultCheckerSet && checkerSetVO.defaultCheckerSet) {
                    chooseCheckerPropVO = checkerPropVO
                    break
                } else if (!hasRecommended &&
                    (CheckerSetSource.DEFAULT.toString() == checkerSetVO.checkerSetSource || CheckerSetSource
                        .RECOMMEND.toString() == checkerSetVO.checkerSetSource)
                ) {
                    chooseCheckerPropVO = checkerPropVO
                    hasRecommended = true
                } else if (chooseCheckerPropVO == null) {
                    chooseCheckerPropVO = checkerPropVO
                }
            }
            mergeTaskCheckers[checkerKey] = chooseCheckerPropVO
        }
        return mergeTaskCheckers
    }

    private fun getRealChecker(checkerDetail: CheckerDetailVO): String {
        return if (ComConstants.Tool.COVERITY.name == checkerDetail.toolName) {
            checkerDetail.checkerName
        } else {
            checkerDetail.checkerKey
        }
    }

    private fun getOpenChecker(
        checker: String,
        checkerProp: CheckerPropVO?,
        checkerDetail: CheckerDetailVO,
        analyzeConfigInfoVO: AnalyzeConfigInfoVO
    ): OpenCheckerVO? {
        val openChecker = OpenCheckerVO()
        openChecker.checkerName = checker
        openChecker.nativeChecker = if (Objects.isNull(
                checkerDetail.nativeChecker
            )
        ) false else checkerDetail.nativeChecker
        if (checkerProp != null && StringUtils.isNotBlank(checkerProp.props)) {
            val checkerPropsList = JsonUtil.to(checkerProp.props, object : TypeReference<List<CheckerProps>>() {})

            val list = checkerPropsList.stream().map { checkerProps ->
                val checkerOptions = OpenCheckerVO.CheckerOptions()
                checkerOptions.checkerOptionName = checkerProps.propName
                checkerOptions.checkerOptionValue = checkerProps.propValue
                checkerOptions
            }.collect(Collectors.toList())
            openChecker.checkerOptions = list
        }
        // 如果是圈复杂度工具，因为他的规则是由后台封装实现的，但对于工具来说，还是根据工具个性化参数来扫描
        if (ComConstants.Tool.CCN.name.equals(analyzeConfigInfoVO.multiToolType, ignoreCase = true)) {
            val toolOptions = AnalyzeConfigInfoVO.ToolOptions()
            analyzeConfigInfoVO.toolOptions = Lists.newArrayList(toolOptions)
            if (CollectionUtils.isNotEmpty(openChecker.checkerOptions)) {
                for (checkerOptions in openChecker.checkerOptions) {
                    if (ComConstants.KEY_CCN_THRESHOLD == checkerOptions.checkerOptionName) {
                        toolOptions.optionName = checkerOptions.checkerOptionName
                        toolOptions.optionValue = checkerOptions.checkerOptionValue
                        break
                    }
                }
            } else {
                toolOptions.optionName = ComConstants.KEY_CCN_THRESHOLD
                toolOptions.optionValue = ComConstants.DEFAULT_CCN_THRESHOLD.toString()
            }
        }
        return openChecker
    }

    /**
     * 查询任务的规则配置
     *
     * @param analyzeConfigInfoVO 告警配置详情
     * @return
     */
    override fun getTaskCheckerConfig(analyzeConfigInfoVO: AnalyzeConfigInfoVO): AnalyzeConfigInfoVO {
        val beginTime = System.currentTimeMillis()
        val taskId = analyzeConfigInfoVO.taskId
        val toolName = analyzeConfigInfoVO.multiToolType
        logger.info("begin getTaskCheckerConfig: taskId={}, toolName={}", taskId, toolName)

        val checkerSetVOList = iCheckerSetBizService.getCheckerSetsByTaskId(taskId)
        if (CollectionUtils.isEmpty(checkerSetVOList)) {
            logger.info("task {} checker set is empty.", taskId)
            return analyzeConfigInfoVO
        }

        val checkerDetailModel = checkerDao.findByToolName(toolName)
        val filterCheckerDetail = this.filterCheckerDetail(
            toolName, analyzeConfigInfoVO.paramJson,
            analyzeConfigInfoVO.language, checkerDetailModel
        )
        if (filterCheckerDetail.isEmpty()) {
            logger.info("tool {} checker detail list is empty.", toolName)
            return analyzeConfigInfoVO
        }

        // 合并任务关联的多个规则集中的规则
        val mergeTaskCheckers = mergeTaskCheckerSets(toolName, checkerSetVOList, filterCheckerDetail)

        val skipCheckers: MutableSet<String> = Sets.newHashSet()
        val covPWCheckers: MutableSet<String> = Sets.newHashSet()
        val covOptions: MutableSet<String> = Sets.newHashSet()
        var openCheckerList: MutableList<OpenCheckerVO?> = ArrayList()
        val tosaCheckers: MutableSet<String> = Sets.newHashSet()
        // 遍历工具所有规则，提取出需要屏蔽的规则和打开的规则
        for (checkerDetail in filterCheckerDetail) { // 如果规则集中配置了该规则，表示规则是打开状态true，否是是关闭状态false
            val isOpen = mergeTaskCheckers.containsKey(checkerDetail.checkerKey)
            val checkerProp = mergeTaskCheckers[checkerDetail.checkerKey]
            val checkerName: String = getRealChecker(checkerDetail)
            if (ComConstants.CheckerPkgKind.DEFAULT.value() == checkerDetail.pkgKind) {
                if (isOpen) {
                    // COVERITY不需要记录打开的默认规则，其他工具记录到打开规则列表
                    if (ComConstants.Tool.COVERITY.name != toolName) {
                        openCheckerList.add(
                            getOpenChecker(
                                checkerName,
                                checkerProp,
                                checkerDetail,
                                analyzeConfigInfoVO
                            )
                        )
                        if (checkerName.endsWith("-tosa")) {
                            tosaCheckers.add(checkerName)
                        }
                    }
                } else {
                    skipCheckers.add(checkerName)
                }
            } else { // 不在默认包的默认规则如果被关闭，则要记录到skipCheckers
                if (SWIFT_DEFAULT_CHECKERS.contains(checkerDetail.checkerKey)) {
                    if (!isOpen) {
                        skipCheckers.add(checkerName)
                    }
                } else { // 其他打开的非默认规则记录到打开规则列表
                    if (isOpen) { // PW规则不加入到OpenCheckers里
                        if (ComConstants.Tool.COVERITY.name == toolName && ComConstants.CheckerPkgKind.COMPILE.value() == checkerDetail.pkgKind) {
                            covPWCheckers.add(checkerName)
                        } else {
                            openCheckerList.add(
                                getOpenChecker(
                                    checkerName,
                                    checkerProp,
                                    checkerDetail,
                                    analyzeConfigInfoVO
                                )
                            )
                        }
                        if (checkerName.endsWith("-tosa")) {
                            tosaCheckers.add(checkerName)
                        }
                    }
                }
            }
            // 记录Coverity规则子选项
            if (isOpen && ComConstants.Tool.COVERITY.name == toolName && CheckerConstants.CheckerProperty.ADVANCED.value() == checkerDetail.covProperty && CollectionUtils.isNotEmpty(
                    checkerDetail.covSubcategory
                )
            ) {
                for (covSubcategoryVO in checkerDetail.covSubcategory) {
                    covOptions.add(
                        covSubcategoryVO.checkerName + ":" + covSubcategoryVO.checkerSubcategoryDetail
                    )
                }
            }
        }

        // 如果有tosa规则，则去掉原有规则，并去掉tosa规则的-tosa字段
        if (CollectionUtils.isNotEmpty(openCheckerList)) {
            val it = openCheckerList.iterator()
            while (it.hasNext()) {
                val openChecker = it.next()
                if (openChecker!!.checkerName.contains("-tosa")) {
                    openChecker.checkerName = openChecker.checkerName.replace("-tosa".toRegex(), "")
                } else if (tosaCheckers.contains(openChecker.checkerName + "-tosa")) {
                    it.remove()
                }
            }
        }
        if (CollectionUtils.isEmpty(openCheckerList)) {
            openCheckerList = ArrayList()
        }

        // 转为分号分隔字符串
        analyzeConfigInfoVO.skipCheckers = skipCheckers.joinToString(ComConstants.SEMICOLON)
        analyzeConfigInfoVO.openCheckers = openCheckerList
        analyzeConfigInfoVO.covOptions = Lists.newArrayList(covOptions)
        analyzeConfigInfoVO.covPWCheckers = covPWCheckers.joinToString(ComConstants.SEMICOLON)
        logger.info("end getTaskCheckerConfig.. cost:{}", System.currentTimeMillis() - beginTime)

        return analyzeConfigInfoVO
    }

    /**
     * 查询任务配置的圈复杂度阀值
     *
     * @param toolConfigInfoModel 工具配置信息
     */
    override fun getCcnThreshold(toolConfigInfoModel: ToolConfigInfoModel): Int {
        var ccnThreshold = ComConstants.DEFAULT_CCN_THRESHOLD

        var analyzeConfigInfoVO = AnalyzeConfigInfoVO()
        analyzeConfigInfoVO.taskId = toolConfigInfoModel.taskId
        analyzeConfigInfoVO.multiToolType = ComConstants.Tool.CCN.name

        // 查询任务规则配置，从配置中获取CCN阀值
        analyzeConfigInfoVO = getTaskCheckerConfig(analyzeConfigInfoVO)

        val openCheckers = analyzeConfigInfoVO.openCheckers
        if (CollectionUtils.isNotEmpty(openCheckers) && CollectionUtils.isNotEmpty(openCheckers[0].checkerOptions)) {
            val ccnThresholdStr = openCheckers[0].checkerOptions[0].checkerOptionValue
            ccnThreshold = if (StringUtils.isEmpty(ccnThresholdStr))
                ComConstants.DEFAULT_CCN_THRESHOLD else ccnThresholdStr.trim { it <= ' ' }.toInt()
        } else {
            // 任务规则配置中获取不到，从个性化参数中获取CCN阀值
            if (StringUtils.isNotEmpty(toolConfigInfoModel.paramJson)) {
                val paramJson = org.json.JSONObject(toolConfigInfoModel.paramJson)

                if (paramJson.has(ComConstants.KEY_CCN_THRESHOLD)) {
                    val ccnThresholdStr = paramJson.getString(ComConstants.KEY_CCN_THRESHOLD)
                    ccnThreshold = if (StringUtils.isEmpty(ccnThresholdStr))
                        ComConstants.DEFAULT_CCN_THRESHOLD else ccnThresholdStr.trim { it <= ' ' }.toInt()
                }
            }
        }

        return ccnThreshold
    }

    override fun getLastestVersionByCheckerSetId(checkerSetIds: Set<String>): List<CheckerSetModel> {
        return checkerDao.findLatestVersionByCheckerSetId(checkerSetIds)
    }
}
