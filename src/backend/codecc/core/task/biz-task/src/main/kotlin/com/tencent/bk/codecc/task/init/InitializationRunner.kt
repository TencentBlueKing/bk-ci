/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.task.init

import com.tencent.bk.codecc.task.dao.CommonDao
import com.tencent.bk.codecc.task.dao.mongorepository.BaseDataRepository
import com.tencent.bk.codecc.task.dao.mongorepository.TaskRepository
import com.tencent.bk.codecc.task.model.OpenSourceCheckerSet
import com.tencent.bk.codecc.task.model.TaskInfoEntity
import com.tencent.bk.codecc.task.service.code.InitResponseCode
import com.tencent.devops.common.auth.api.pojo.external.KEY_ADMIN_MEMBER
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.common.constant.RedisKeyConstants.STANDARD_LANG
import com.tencent.devops.common.service.ToolMetaCacheService
import com.tencent.devops.common.util.ThreadPoolUtil
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

@Component
class InitializationRunner @Autowired constructor(
        private val initResponseCode: InitResponseCode,
        private val taskRepository: TaskRepository,
        private val baseDataRepository: BaseDataRepository,
        private val redisTemplate: RedisTemplate<String, String>,
        private val toolMetaCacheService: ToolMetaCacheService,
        private val commonDao: CommonDao
) : CommandLineRunner {

    @Value("\${auth.url:#{null}}")
    private var authUrl: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(InitializationRunner::class.java)
    }

    override fun run(vararg arg: String?) {
        val currentVal = redisTemplate.opsForValue().get(RedisKeyConstants.CODECC_TASK_ID)
        if (null == currentVal || currentVal.toLong() < ComConstants.COMMON_NUM_10000L) {
            logger.info("start to initialize redis key!")
            val taskInfoEntity = taskRepository.findFirstByTaskIdExistsOrderByTaskIdDesc(true)
            if (null == taskInfoEntity) {
                redisTemplate.opsForValue()
                    .set(RedisKeyConstants.CODECC_TASK_ID, ComConstants.COMMON_NUM_10000L.toString())
            } else {
                redisTemplate.opsForValue()
                    .set(RedisKeyConstants.CODECC_TASK_ID, (taskInfoEntity.taskId + 1).toString())
            }
        }

        val jedisConnectionFactory: JedisConnectionFactory = redisTemplate.connectionFactory as JedisConnectionFactory
        logger.info("start to init data with redis: {}, {}, {}",
            jedisConnectionFactory.hostName,
            jedisConnectionFactory.port,
            jedisConnectionFactory.database)

        // 国际化操作[ 响应码、操作记录、规则包、规则名称、报表日期、工具参数、工具描述、操作类型 ]
        globalMessage(redisTemplate)

        // 管理员列表
        adminMember(redisTemplate)

        // 所有项目的创建来源
        ThreadPoolUtil.addRunnableTask { taskCreateFrom(redisTemplate) }

        // 初始化工具缓存
        toolMetaCacheService.loadToolDetailCache()

        // 将工具顺序设置在缓存中
        setToolOrder()

        // 缓存 语言-工具 映射关系
        setLangToolMapping()

        // 初始化开源配置规则集
//        updateOpenCheckerSet()
    }

    /**
     * 国际化处理
     */
    fun globalMessage(redisTemplate: RedisTemplate<String, String>) {
        // 响应码、操作记录国际化
        val responseCodeMap = initResponseCode.getGlobalMessageMap()
        for (key in responseCodeMap.keys) {
            redisTemplate.opsForValue().set(key, responseCodeMap[key] ?: "")
        }

        // 规则包国际化
        val checkerPackageMap = initResponseCode.getCheckerPackage()
        redisTemplate.opsForHash<String, String>()
            .putAll(RedisKeyConstants.GLOBAL_CHECKER_PACKAGE_MSG, checkerPackageMap)

        // 数据报表日期国际化
        val dataReportDate = initResponseCode.getDataReportDate()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_DATA_REPORT_DATE, dataReportDate)

        // 工具描述国际化
        val toolDescription = initResponseCode.getToolDescription()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_DESCRIPTION, toolDescription)

        // 工具参数标签[ labelName ]国际化
        val labelName = initResponseCode.getToolParams()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_LABEL_NAME, labelName)
        logger.info("init global params GLOBAL_TOOL_PARAMS_LABEL_NAME: {}", labelName)

        // 工具参数提示[ tips ]国际化
        val tips = initResponseCode.getToolParamsTips()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_TOOL_PARAMS_TIPS, tips)
        logger.info("init global params GLOBAL_TOOL_PARAMS_TIPS: {}", tips)

        // 操作类型国际化
        val operTypeMap = initResponseCode.getOperTypeMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_PREFIX_OPERATION_TYPE, operTypeMap)

        val checkDescMap = initResponseCode.getCheckerDescMap()
        redisTemplate.opsForHash<String, String>().putAll(RedisKeyConstants.GLOBAL_CHECKER_DESC, checkDescMap)
    }

    /**
     * 管理员列表
     */
    private fun adminMember(redisTemplate: RedisTemplate<String, String>) {
        val baseDataEntities = baseDataRepository.findAllByParamTypeAndParamCode("ADMIN_MEMBER", "ADMIN_MEMBER")
        if (!baseDataEntities.isNullOrEmpty()) {
            val baseDataEntity = baseDataEntities[0]
            if (!baseDataEntity.paramValue.isNullOrEmpty()) {
                redisTemplate.opsForValue().set(KEY_ADMIN_MEMBER, baseDataEntity.paramValue)
            }
        }
        // 添加bg管理员清单
        /**
         * 953: CDG
         * 29294: CSIG
         * 956: IEG
         * 29292: PCG
         * 14129: WXG
         * 958: TEG
         * 78: S1
         * 2233: S2
         * 234: S3
         * 955: 其他
         */
        val baseDataEntityList = baseDataRepository.findAllByParamType("BG_ADMIN_MEMBER")
        if (!baseDataEntityList.isNullOrEmpty()) {
            baseDataEntityList.forEach {
                redisTemplate.opsForHash<String, String>()
                    .put(RedisKeyConstants.KEY_USER_BG_ADMIN, it.paramCode, it.paramValue)
            }
        }
    }

    /**
     * 所有项目的创建来源
     */
    private fun taskCreateFrom(redisTemplate: RedisTemplate<String, String>) {
        // 判断是否需要缓存createFrom
        val newestTaskId = redisTemplate.opsForValue().get(RedisKeyConstants.CODECC_TASK_ID)
        val newestTaskCreateFrom = redisTemplate.opsForHash<String, String>()
            .get(PREFIX_TASK_INFO + (newestTaskId!!.toLong() - 1), KEY_CREATE_FROM)
        // 判断是否需要缓存bg映射
        val maxTaskInfoEntity =
            taskRepository.findFirstByCreateFromOrderByTaskIdDesc(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value())
        val latestBgMapping = redisTemplate.opsForHash<String, String>()
            .get(RedisKeyConstants.KEY_TASK_BG_MAPPING, maxTaskInfoEntity.taskId.toString())
        if (StringUtils.isNotEmpty(newestTaskCreateFrom) && !latestBgMapping.isNullOrBlank()) {
            return
        }
        var pageable: Pageable = PageRequest.of(0, 1000)
        var pageTasks: List<TaskInfoEntity>
        do {
            val taskInfoEntityPage = taskRepository.findTasksByPage(pageable)
            if (taskInfoEntityPage.hasContent()) {
                pageTasks = taskInfoEntityPage.content
                var needCache = false
                var needBgCache = false
                val lastTask = pageTasks.last()
                if (lastTask != null) {
                    val lastTaskCreateFrom = redisTemplate.opsForHash<String, String>()
                        .get(PREFIX_TASK_INFO + lastTask.taskId, KEY_CREATE_FROM)
                    if (lastTaskCreateFrom.isNullOrEmpty()) {
                        needCache = true
                    }
                }
                val lastGongfengTask =
                    pageTasks.findLast { it.createFrom == ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() }
                if (lastGongfengTask != null) {
                    val lastGongfengBgId = redisTemplate.opsForHash<String, String>()
                        .get(RedisKeyConstants.KEY_TASK_BG_MAPPING, lastGongfengTask.taskId.toString())
                    if (lastGongfengBgId.isNullOrBlank()) {
                        needBgCache = true
                    }
                }
                if (needCache) {
                    redisTemplate.execute { connection ->
                        for (task in pageTasks) {
                            if (!task.createFrom.isNullOrEmpty()) {
                                connection.hSet(
                                    (PREFIX_TASK_INFO + task.taskId).toByteArray(),
                                    KEY_CREATE_FROM.toByteArray(),
                                    task.createFrom.toByteArray()
                                )
                            }
                        }
                    }
                }

                if (needBgCache) {
                    redisTemplate.execute { connection ->
                        for (task in pageTasks) {
                            // 如果是工蜂，则要加映射信息
                            if (ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == task.createFrom) {
                                connection.hSet(
                                    RedisKeyConstants.KEY_TASK_BG_MAPPING.toByteArray(),
                                    task.taskId.toString().toByteArray(),
                                    task.bgId.toString().toByteArray()
                                )
                            }
                        }
                    }
                }

                if (taskInfoEntityPage.hasNext()) {
                    pageable = pageable.next()
                }
            }
        } while (taskInfoEntityPage.hasNext())
    }

    private fun setToolOrder() {
        val toolOrder = commonDao.toolOrder
        val langOrder = commonDao.langOrder

        logger.info("start to set tool order: {}", commonDao.toolOrder)
        logger.info("start to set lang order: {}", commonDao.langOrder)

        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_TOOL_ORDER, toolOrder)
        redisTemplate.opsForValue().set(RedisKeyConstants.KEY_LANG_ORDER, langOrder)
    }

    /**
     * 添加开源扫描规则集配置
     */
    private fun updateOpenCheckerSet() {
        logger.info("start to init open checker set!")
        val baseDataEntityList = baseDataRepository.findAllByParamType("LANG")
        baseDataEntityList.forEach {
            val checkerSetList = mutableListOf<OpenSourceCheckerSet>()
            when (it.paramName) {
                "C#" -> {
                    val formatCheckerSet1 = OpenSourceCheckerSet()
                    formatCheckerSet1.checkerSetId = "standard_csharp"
                    formatCheckerSet1.toolList = setOf("STYLECOP")
                    formatCheckerSet1.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet1.version = 9
                    checkerSetList.add(formatCheckerSet1)
                    val formatCheckerSet11 = OpenSourceCheckerSet()
                    formatCheckerSet11.checkerSetId = "codecc_default_ccn_csharp"
                    formatCheckerSet11.toolList = setOf("CCN")
                    formatCheckerSet11.version = 3
                    checkerSetList.add(formatCheckerSet11)
                    val formatCheckerSet12 = OpenSourceCheckerSet()
                    formatCheckerSet12.checkerSetId = "codecc_default_dupc_csharp"
                    formatCheckerSet12.toolList = setOf("DUPC")
                    formatCheckerSet12.version = 1
                    checkerSetList.add(formatCheckerSet12)
                    val formatCheckerSet13 = OpenSourceCheckerSet()
                    formatCheckerSet13.checkerSetId = "standard_csharp_simplify"
                    formatCheckerSet13.toolList = setOf("STYLECOP")
                    formatCheckerSet13.version = 3
                    formatCheckerSet13.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    checkerSetList.add(formatCheckerSet13)
                    val securityCheckerSet1 = OpenSourceCheckerSet()
                    securityCheckerSet1.checkerSetId = "pecker_csharp_no_coverity"
                    securityCheckerSet1.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet1.version = 3
                    checkerSetList.add(securityCheckerSet1)
                }
                "C/C++" -> {
                    val formatCheckerSet2 = OpenSourceCheckerSet()
                    formatCheckerSet2.checkerSetId = "standard_cpp"
                    formatCheckerSet2.toolList = setOf("CPPLINT")
                    formatCheckerSet2.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet2.version = 10
                    checkerSetList.add(formatCheckerSet2)
                    val formatCheckerSet21 = OpenSourceCheckerSet()
                    formatCheckerSet21.checkerSetId = "codecc_default_ccn_cpp"
                    formatCheckerSet21.toolList = setOf("CCN")
                    formatCheckerSet21.version = 3
                    checkerSetList.add(formatCheckerSet21)
                    val formatCheckerSet22 = OpenSourceCheckerSet()
                    formatCheckerSet22.checkerSetId = "codecc_default_dupc_cpp"
                    formatCheckerSet22.toolList = setOf("DUPC")
                    formatCheckerSet22.version = 1
                    checkerSetList.add(formatCheckerSet22)
                    val formatCheckerSet23 = OpenSourceCheckerSet()
                    formatCheckerSet23.checkerSetId = "standard_cpp_stockproj"
                    formatCheckerSet23.toolList = setOf("CPPLINT")
                    formatCheckerSet23.version = 4
                    formatCheckerSet23.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    checkerSetList.add(formatCheckerSet23)
                    val securityCheckerSet2 = OpenSourceCheckerSet()
                    securityCheckerSet2.checkerSetId = "pecker_cpp_no_coverity"
                    securityCheckerSet2.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet2.version = 4
                    checkerSetList.add(securityCheckerSet2)
                }
                "JAVA" -> {
                    val formatCheckerSet4 = OpenSourceCheckerSet()
                    formatCheckerSet4.checkerSetId = "standard_java"
                    formatCheckerSet4.toolList = setOf("CHECKSTYLE")
                    formatCheckerSet4.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet4.version = 7
                    checkerSetList.add(formatCheckerSet4)
                    val formatCheckerSet41 = OpenSourceCheckerSet()
                    formatCheckerSet41.checkerSetId = "codecc_default_ccn_java"
                    formatCheckerSet41.toolList = setOf("CCN")
                    formatCheckerSet41.version = 3
                    checkerSetList.add(formatCheckerSet41)
                    val formatCheckerSet42 = OpenSourceCheckerSet()
                    formatCheckerSet42.checkerSetId = "codecc_default_dupc_java"
                    formatCheckerSet42.toolList = setOf("DUPC")
                    formatCheckerSet42.version = 1
                    checkerSetList.add(formatCheckerSet42)
                    val formatCheckerSet43 = OpenSourceCheckerSet()
                    formatCheckerSet43.checkerSetId = "standard_java_stockproj"
                    formatCheckerSet43.toolList = setOf("CHECKSTYLE")
                    formatCheckerSet43.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    formatCheckerSet43.version = 7
                    checkerSetList.add(formatCheckerSet43)
                    val securityCheckerSet4 = OpenSourceCheckerSet()
                    securityCheckerSet4.checkerSetId = "pecker_java_no_coverity"
                    securityCheckerSet4.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet4.version = 10
                    checkerSetList.add(securityCheckerSet4)
                }
                "PHP" -> {
                    val formatCheckerSet51 = OpenSourceCheckerSet()
                    formatCheckerSet51.checkerSetId = "codecc_default_ccn_php"
                    formatCheckerSet51.toolList = setOf("CCN")
                    formatCheckerSet51.version = 3
                    checkerSetList.add(formatCheckerSet51)
                    val securityCheckerSet5 = OpenSourceCheckerSet()
                    securityCheckerSet5.checkerSetId = "pecker_php"
                    securityCheckerSet5.toolList = setOf("WOODPECKER_SENSITIVE", "HORUSPY", "RIPS")
                    securityCheckerSet5.version = 14
                    checkerSetList.add(securityCheckerSet5)
                }
                "OC/OC++" -> {
                    val formatCheckerSet6 = OpenSourceCheckerSet()
                    formatCheckerSet6.checkerSetId = "bkcheck_oc_rule"
                    formatCheckerSet6.toolList = setOf("BKCHECK-OC")
                    formatCheckerSet6.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet6.version = 2
                    checkerSetList.add(formatCheckerSet6)
                    val formatCheckerSet61 = OpenSourceCheckerSet()
                    formatCheckerSet61.checkerSetId = "codecc_default_ccn_oc"
                    formatCheckerSet61.toolList = setOf("CCN")
                    formatCheckerSet61.version = 3
                    checkerSetList.add(formatCheckerSet61)
                    val formatCheckerSet62 = OpenSourceCheckerSet()
                    formatCheckerSet62.checkerSetId = "codecc_default_dupc_oc"
                    formatCheckerSet62.toolList = setOf("DUPC")
                    formatCheckerSet62.version = 1
                    checkerSetList.add(formatCheckerSet62)
                    val formatCheckerSet63 = OpenSourceCheckerSet()
                    formatCheckerSet63.checkerSetId = "standard_oc_simplify_bkcheck"
                    formatCheckerSet63.toolList = setOf("BKCHECK-OC")
                    formatCheckerSet63.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    formatCheckerSet63.version = 2
                    checkerSetList.add(formatCheckerSet63)
                    val securityCheckerSet6 = OpenSourceCheckerSet()
                    securityCheckerSet6.checkerSetId = "pecker_oc_no_coverity"
                    securityCheckerSet6.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet6.version = 4
                    checkerSetList.add(securityCheckerSet6)
                }
                "Python" -> {
                    val formatCheckerSet8 = OpenSourceCheckerSet()
                    formatCheckerSet8.checkerSetId = "standard_python"
                    formatCheckerSet8.toolList = setOf("PYLINT")
                    formatCheckerSet8.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet8.version = 4
                    checkerSetList.add(formatCheckerSet8)
                    val formatCheckerSet81 = OpenSourceCheckerSet()
                    formatCheckerSet81.checkerSetId = "codecc_default_ccn_python"
                    formatCheckerSet81.toolList = setOf("CCN")
                    formatCheckerSet81.version = 3
                    checkerSetList.add(formatCheckerSet81)
                    val formatCheckerSet82 = OpenSourceCheckerSet()
                    formatCheckerSet82.checkerSetId = "codecc_default_dupc_python"
                    formatCheckerSet82.toolList = setOf("DUPC")
                    formatCheckerSet82.version = 1
                    checkerSetList.add(formatCheckerSet82)
                    val formatCheckerSet83 = OpenSourceCheckerSet()
                    formatCheckerSet83.checkerSetId = "standard_python_stockproj"
                    formatCheckerSet83.toolList = setOf("PYLINT")
                    formatCheckerSet83.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    formatCheckerSet83.version = 5
                    checkerSetList.add(formatCheckerSet83)
                    val securityCheckerSet8 = OpenSourceCheckerSet()
                    securityCheckerSet8.checkerSetId = "pecker_python"
                    securityCheckerSet8.toolList = setOf("WOODPECKER_SENSITIVE", "HORUSPY")
                    securityCheckerSet8.version = 24
                    checkerSetList.add(securityCheckerSet8)
                    /*val toolConfigParamJsonVO = ToolConfigParamJsonVO()
                    toolConfigParamJsonVO.toolName = "PYLINT"
                    toolConfigParamJsonVO.varName = "py_version"
                    toolConfigParamJsonVO.chooseValue = "py3"
                    paramJsonVOList.add(toolConfigParamJsonVO)*/
                }
                "JS" -> {
                    val formatCheckerSet9 = OpenSourceCheckerSet()
                    formatCheckerSet9.checkerSetId = "standard_js"
                    formatCheckerSet9.toolList = setOf("ESLINT")
                    formatCheckerSet9.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet9.version = 5
                    checkerSetList.add(formatCheckerSet9)
                    val formatCheckerSet91 = OpenSourceCheckerSet()
                    formatCheckerSet91.checkerSetId = "codecc_default_ccn_js"
                    formatCheckerSet91.toolList = setOf("CCN")
                    formatCheckerSet91.version = 3
                    checkerSetList.add(formatCheckerSet91)
                    val formatCheckerSet92 = OpenSourceCheckerSet()
                    formatCheckerSet92.checkerSetId = "codecc_default_dupc_js"
                    formatCheckerSet92.toolList = setOf("DUPC")
                    formatCheckerSet92.version = 1
                    checkerSetList.add(formatCheckerSet92)
                    val formatCheckerSet93 = OpenSourceCheckerSet()
                    formatCheckerSet93.checkerSetId = "standard_js_stockproj"
                    formatCheckerSet93.toolList = setOf("ESLINT")
                    formatCheckerSet93.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    formatCheckerSet93.version = 2
                    checkerSetList.add(formatCheckerSet93)
                    val securityCheckerSet9 = OpenSourceCheckerSet()
                    securityCheckerSet9.checkerSetId = "pecker_js"
                    securityCheckerSet9.toolList = setOf("WOODPECKER_SENSITIVE", "HORUSPY")
                    securityCheckerSet9.version = 7
                    checkerSetList.add(securityCheckerSet9)
                }
                "Ruby" -> {
                    val formatCheckerSet101 = OpenSourceCheckerSet()
                    formatCheckerSet101.checkerSetId = "codecc_default_ccn_ruby"
                    formatCheckerSet101.toolList = setOf("CCN")
                    formatCheckerSet101.version = 3
                    checkerSetList.add(formatCheckerSet101)
                    val securityCheckerSet10 = OpenSourceCheckerSet()
                    securityCheckerSet10.checkerSetId = "pecker_ruby"
                    securityCheckerSet10.toolList = setOf("WOODPECKER_SENSITIVE", "HORUSPY")
                    securityCheckerSet10.version = 5
                    checkerSetList.add(securityCheckerSet10)
                }
                "Golang" -> {
                    val formatCheckerSet11 = OpenSourceCheckerSet()
                    formatCheckerSet11.checkerSetId = "standard_go"
                    formatCheckerSet11.toolList = setOf("GOML")
                    formatCheckerSet11.checkerSetType = ComConstants.OpenSourceCheckerSetType.FULL.name
                    formatCheckerSet11.version = 4
                    checkerSetList.add(formatCheckerSet11)
                    val formatCheckerSet111 = OpenSourceCheckerSet()
                    formatCheckerSet111.checkerSetId = "codecc_default_ccn_go"
                    formatCheckerSet111.toolList = setOf("CCN")
                    formatCheckerSet111.version = 3
                    checkerSetList.add(formatCheckerSet111)
                    val formatCheckerSet112 = OpenSourceCheckerSet()
                    formatCheckerSet112.checkerSetId = "codecc_default_dupc_go"
                    formatCheckerSet112.toolList = setOf("DUPC")
                    formatCheckerSet112.version = 1
                    checkerSetList.add(formatCheckerSet112)
                    val formatCheckerSet113 = OpenSourceCheckerSet()
                    formatCheckerSet113.checkerSetId = "standard_go_stockproj"
                    formatCheckerSet113.toolList = setOf("GOML")
                    formatCheckerSet113.checkerSetType = ComConstants.OpenSourceCheckerSetType.SIMPLIFIED.name
                    formatCheckerSet113.version = 3
                    checkerSetList.add(formatCheckerSet113)
                    val securityCheckerSet11 = OpenSourceCheckerSet()
                    securityCheckerSet11.checkerSetId = "pecker_go"
                    securityCheckerSet11.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet11.version = 6
                    checkerSetList.add(securityCheckerSet11)
                }
                // swift 因为只包含coverity工具 所以注释掉
                /*"Swift" -> {
                    val securityCheckerSet12 = CheckerSetVO()
                    securityCheckerSet12.checkerSetId = "pecker_swift"
                    securityCheckerSet12.toolList = setOf("COVERITY")
                    checkerSetVOList.add(securityCheckerSet12)
                }*/
                "TypeScript" -> {
                    val securityCheckerSet12 = OpenSourceCheckerSet()
                    securityCheckerSet12.checkerSetId = "pecker_ts"
                    securityCheckerSet12.toolList = setOf("WOODPECKER_SENSITIVE")
                    securityCheckerSet12.version = 9
                    checkerSetList.add(securityCheckerSet12)
                }
            }
            it.openSourceCheckerSets = checkerSetList
            baseDataRepository.save(it)
        }
    }

    fun setLangToolMapping() {
        val langtoolMapping = baseDataRepository.findAllByParamType(STANDARD_LANG)
        langtoolMapping.forEach {
            redisTemplate.opsForHash<String, String>().put(RedisKeyConstants.STANDARD_LANG, it.paramCode, it.paramValue)
        }
    }
}
