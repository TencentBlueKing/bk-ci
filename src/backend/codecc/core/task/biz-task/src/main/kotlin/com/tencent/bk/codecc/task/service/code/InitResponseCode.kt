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

package com.tencent.bk.codecc.task.service.code

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.xml.internal.ws.util.JAXWSUtils.getUUID
import com.tencent.bk.codecc.defect.constant.DefectMessageCode.*
import com.tencent.devops.common.api.pojo.GlobalMessage
import com.tencent.devops.common.constant.ComConstants.*
import com.tencent.devops.common.constant.RedisKeyConstants
import org.bouncycastle.asn1.x500.style.RFC4519Style.name
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.script.ScriptEngine.FILENAME

/**
 * 初始化响应码
 *
 * @date 2019/6/24
 */
@Component
class InitResponseCode @Autowired constructor(private val objectMapper: ObjectMapper) {


    /**
     * 获取国际化信息Map
     */
    fun getGlobalMessageMap(): Map<String, String> {
        val codeMap = HashMap<String, String>()
        // 任务响应码
        getTaskMessageCode(codeMap)
        // 告警响应码
        getDefectMessageCode(codeMap)
        // 公用响应码
        getCommonMessageCode(codeMap)
        // 操作记录国际化
        getOperationHistory(codeMap)

        return codeMap
    }


    /**
     * 告警响应码
     */
    fun getDefectMessageCode(codeMap: HashMap<String, String>): Map<String, String> {
        val defectCode = GlobalMessage(getUUID(), TYPE_NOT_EXITS, "03", "{0}类型服务不存在", null, "{0} type service does not exist")
        val defectCode1 = GlobalMessage(getUUID(), NOT_FIND_CHECKER_PACKAGE, "03", "找不到规则包{0}", null, "Rule package not found {0}")
        val defectCode2 = GlobalMessage(getUUID(), NOT_FIND_CHECKER, "03", "找不到规则{0}", null, "Checker not found {0}")
        val defectCode3 = GlobalMessage(getUUID(), SAME_CHECKER_PARAM, "03", "{0}规则值参数相同， 请重试", null, "{0} checker parameters value same, please try again.")
        val defectCode4 = GlobalMessage(getUUID(), CHECKER_SET_PARAMETER_IS_INVALID, "03", "规则集参数非法，{0}", null, "Parameter is invalid.")
        codeMap[TYPE_NOT_EXITS] = objectMapper.writeValueAsString(defectCode)
        codeMap[NOT_FIND_CHECKER_PACKAGE] = objectMapper.writeValueAsString(defectCode1)
        codeMap[NOT_FIND_CHECKER] = objectMapper.writeValueAsString(defectCode2)
        codeMap[SAME_CHECKER_PARAM] = objectMapper.writeValueAsString(defectCode3)
        codeMap[CHECKER_SET_PARAMETER_IS_INVALID] = objectMapper.writeValueAsString(defectCode4)
        return codeMap
    }


    /**
     * 任务响应码
     */
    fun getTaskMessageCode(codeMap: HashMap<String, String>): Map<String, String> {
        val taskCode1 = GlobalMessage(
                getUUID(), "2301001", "01", "添加工具失败", null, "Add tool failed")
        val taskCode2 = GlobalMessage(
                getUUID(), "2301002", "01", "部分添加工具失败", null, "Partially add tool failed")
        val taskCode3 = GlobalMessage(
                getUUID(), "2301003", "01", "注册任务失败", null, "Registration task failed")
        val taskCode4 = GlobalMessage(
                getUUID(), "2301004", "01", "创建项目失败，流名称已存在错误码", null, "Failed to create project, stream name already has error code")
        val taskCode5 = GlobalMessage(
                getUUID(), "2301005", "01", "未发现相应的元素", null, "No corresponding elements found")
        val taskCode6 = GlobalMessage(
                getUUID(), "2301006", "01", "任务已经开启，不能重复操作", null, "Task already started and cannot be repeated")
        val taskCode7 = GlobalMessage(
                getUUID(), "2301007", "01", "任务已经关闭，不能重复操作", null, "Task already ended and cannot be repeated")
        val taskCode8 = GlobalMessage(
                getUUID(), "2301008", "01", "流水线执行定时时间为空", null, "Pipeline execution timing is empty")
        val taskCode9 = GlobalMessage(
                getUUID(), "2301009", "01", "失效任务失败", null, "Close task failed")
        val taskCode13 = GlobalMessage(
                getUUID(), "2301013", "01", "{0}", null, "{0}")
        codeMap["2301001"] = objectMapper.writeValueAsString(taskCode1)
        codeMap["2301002"] = objectMapper.writeValueAsString(taskCode2)
        codeMap["2301003"] = objectMapper.writeValueAsString(taskCode3)
        codeMap["2301004"] = objectMapper.writeValueAsString(taskCode4)
        codeMap["2301005"] = objectMapper.writeValueAsString(taskCode5)
        codeMap["2301006"] = objectMapper.writeValueAsString(taskCode6)
        codeMap["2301007"] = objectMapper.writeValueAsString(taskCode7)
        codeMap["2301008"] = objectMapper.writeValueAsString(taskCode8)
        codeMap["2301009"] = objectMapper.writeValueAsString(taskCode9)
        codeMap["2301013"] = objectMapper.writeValueAsString(taskCode13)

        return codeMap
    }


    /**
     * 公用模块响应码
     */
    fun getCommonMessageCode(codeMap: HashMap<String, String>): Map<String, String> {
        val commonCode = GlobalMessage(
                getUUID(), "0", "00", "成功", null, "Succeed")
        val commonCode1 = GlobalMessage(
                getUUID(), "2300001", "00", "系统内部繁忙，请稍后再试", null, "The system is busy inside. Please try again later")
        val commonCode2 = GlobalMessage(
                getUUID(), "2300002", "00", "{0}不能为空", null, "{0} cannot be null")
        val commonCode3 = GlobalMessage(
                getUUID(), "2300003", "00", "{0}已经存在系统，请换一个再试", null, "{0} already exists. Please change one and try again")
        val commonCode4 = GlobalMessage(
                getUUID(), "2300004", "00", "{0}为非法数据", null, "{0} is illegal data")
        val commonCode5 = GlobalMessage(
                getUUID(), "2300005", "00", "无效的token，请先oauth认证", null, "Invalid token, please do oauth authentication first")
        val commonCode6 = GlobalMessage(
                getUUID(), "2300006", "00", "{0}暂无权限，请前往设置-人员权限进行处理。", null, "{0} has no permission")
        val commonCode7 = GlobalMessage(
                getUUID(), "2300007", "00", "[{0}{1}]记录不存在", null, "[{0}{1}] record does not exist")
        val commonCode8 = GlobalMessage(
                getUUID(), "2300008", "00", "{0}记录已经存在", null, "{0} record already exists")
        val commonCode9 = GlobalMessage(
                getUUID(), "2300009", "00", "调用第三方接口失败，请查询日志", null, "Failed to call the third-party interface. Please query the log")
        val commonCode10 = GlobalMessage(
                getUUID(), "2300010", "00", "调用内部服务接口失败，请查询日志", null, "Failed to call the internal service interface. Please query the log")
        val commonCode11 = GlobalMessage(
                getUUID(), "2300011", "00", "调用蓝盾接口失败，请查询日志", null, "Failed to call the Blue Shield interface. Please query the log")
        val commonCode12 = GlobalMessage(
                getUUID(), "2300012", "00", "代码运行失败，请查看日志", null, "The code failed to run. Please check the log")
        val commonCode13 = GlobalMessage(
                getUUID(), "2300013", "00", "找不到对应的处理器", null, "Failed to find the corresponding processor")
        val commonCode14 = GlobalMessage(
                getUUID(), "2300014", "00", "工具{0}是无效工具，检查参数是否正确或者稍后重试", null, "Tool {0} is an invalid tool. Check if the parameters are correct or try again later")
        val commonCode15 = GlobalMessage(
                getUUID(), "2300015", "00", "找不到任何有效的{0}服务提供者", null, "Failed to find valid {0} service provider")
        val commonCode18 = GlobalMessage(
                getUUID(), "2300018", "00", "无法从工蜂获取你的代码片段信息。请检查你的代码库是否被删除或设为私有。", null, "Fail to get code content from git, please check if that your repository is deleted or switched to private")
        val commonCode19 = GlobalMessage(
                getUUID(), "2300019", "00", "无法从工蜂获取代码片段。请检查你是否对该仓库有权限。", null, "Fail to get code content from git, please check if that your repository is deleted or switched to private")

        // 公用响应码
        codeMap["0"] = objectMapper.writeValueAsString(commonCode)
        codeMap["2300001"] = objectMapper.writeValueAsString(commonCode1)
        codeMap["2300002"] = objectMapper.writeValueAsString(commonCode2)
        codeMap["2300003"] = objectMapper.writeValueAsString(commonCode3)
        codeMap["2300004"] = objectMapper.writeValueAsString(commonCode4)
        codeMap["2300005"] = objectMapper.writeValueAsString(commonCode5)
        codeMap["2300006"] = objectMapper.writeValueAsString(commonCode6)
        codeMap["2300007"] = objectMapper.writeValueAsString(commonCode7)
        codeMap["2300008"] = objectMapper.writeValueAsString(commonCode8)
        codeMap["2300009"] = objectMapper.writeValueAsString(commonCode9)
        codeMap["2300010"] = objectMapper.writeValueAsString(commonCode10)
        codeMap["2300011"] = objectMapper.writeValueAsString(commonCode11)
        codeMap["2300012"] = objectMapper.writeValueAsString(commonCode12)
        codeMap["2300013"] = objectMapper.writeValueAsString(commonCode13)
        codeMap["2300014"] = objectMapper.writeValueAsString(commonCode14)
        codeMap["2300015"] = objectMapper.writeValueAsString(commonCode15)
        codeMap["2300018"] = objectMapper.writeValueAsString(commonCode18)
        codeMap["2300019"] = objectMapper.writeValueAsString(commonCode19)

        return codeMap
    }


    /**
     * 获取操作记录国际化信息
     */
    fun getOperationHistory(codeMap: HashMap<String, String>): Map<String, String> {
        val operMsg1 = GlobalMessage(
                getUUID(), FUNC_REGISTER_TOOL, "00",
                "{0}添加工具{1}", null, "{0} registered tools {1}")
        val operMsg2 = GlobalMessage(
                getUUID(), FUNC_TASK_INFO, "00",
                "{0}修改任务信息", null, "{0} modified task information")
        val operMsg3 = GlobalMessage(
                getUUID(), FUNC_TASK_SWITCH, "00",
                "{0}修改任务状态", null, "{0} switched task status")
        val operMsg4 = GlobalMessage(
                getUUID(), FUNC_TRIGGER_ANALYSIS, "00",
                "{0}触发代码扫描", null, "{0} triggered immediate analysis")
        val operMsg5 = GlobalMessage(
                getUUID(), FUNC_TOOL_SWITCH, "00",
                "{0}修改工具状态", null, "{0} switched tool status")
        val operMsg6 = GlobalMessage(
                getUUID(), FUNC_SCAN_SCHEDULE, "00",
                "{0}修改扫描触发时间", null, "{0} modified scan schedule")
        val operMsg7 = GlobalMessage(
                getUUID(), FUNC_FILTER_PATH, "00",
                "{0}修改屏蔽路径", null, "{0} modified filtered path")
        val operMsg8 = GlobalMessage(
                getUUID(), FUNC_DEFECT_MANAGE, "00",
                "{0}进行{1}工具的批量作者转换,原作者清单：{2},现作者清单：{3}", null,
                "{0} batch transfer authors for tool {1}, previous author list: {2}, current author list: {3}")
        val operMsg9 = GlobalMessage(
                getUUID(), FUNC_CODE_REPOSITORY, "00",
                "{0}更新任务代码库信息", null, "{0} updated task repository information")
        val operMsg10 = GlobalMessage(
                getUUID(), FUNC_CHECKER_CONFIG, "00",
                "{0}更新规则配置信息", null, "{0} updated checker config information")

        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_REGISTER_TOOL"] = objectMapper.writeValueAsString(operMsg1)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_TASK_INFO"] = objectMapper.writeValueAsString(operMsg2)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_TASK_SWITCH"] = objectMapper.writeValueAsString(operMsg3)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_TRIGGER_ANALYSIS"] = objectMapper.writeValueAsString(operMsg4)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_TOOL_SWITCH"] = objectMapper.writeValueAsString(operMsg5)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_SCAN_SCHEDULE"] = objectMapper.writeValueAsString(operMsg6)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_FILTER_PATH"] = objectMapper.writeValueAsString(operMsg7)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_DEFECT_MANAGE"] = objectMapper.writeValueAsString(operMsg8)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_CODE_REPOSITORY"] = objectMapper.writeValueAsString(operMsg9)
        codeMap["${RedisKeyConstants.PREFIX_OPERATION_HISTORY_MSG}$FUNC_CHECKER_CONFIG"] = objectMapper.writeValueAsString(operMsg10)

        return codeMap
    }


    /**
     * 规则包国际化
     */
    fun getCheckerPackage(): Map<String, String> {
        val package1 = GlobalMessage(
                getUUID(), NODE, "00", "Node规则包", null, "Node rules package")
        val package2 = GlobalMessage(
                getUUID(), STYLISTIC, "00", "风格规则包", null, "Style rules package")
        val package3 = GlobalMessage(
                getUUID(), STRICT_MODE, "00", "严格模式包", null, "Strict mode package")
        val package4 = GlobalMessage(
                getUUID(), LOGICA, "00", "逻辑规则包", null, "Logic rules package")
        val package5 = GlobalMessage(
                getUUID(), DEFAULT, "00", "默认规则包", null, "Default rules package")
        val package7 = GlobalMessage(
                getUUID(), VARIABLE, "00", "变量规则包", null, "Variable rules package")
        val package8 = GlobalMessage(
                getUUID(), ES6, "00", "ES6规则包", null, "ES6 rules package")
        val package9 = GlobalMessage(
                getUUID(), BEST_PRACTICES, "00", "最佳实践包", null, "Best practice package")
        val package10 = GlobalMessage(
                getUUID(), HEADER_FILE, "00", "头文件规则包", null, "Header file rules package")
        val package11 = GlobalMessage(
                getUUID(), SYS_API, "00", "系统API包", null, "System API package")
        val package13 = GlobalMessage(
                getUUID(), SECURITY, "00", "安全规则包", null, "Security rules package")
        val package14 = GlobalMessage(
                getUUID(), NAMING, "00", "命名规范包", null, "Naming specification package")
        val package15 = GlobalMessage(
                getUUID(), COMMENT, "00", "注释规则包", null, "Comment specification package")
        val package16 = GlobalMessage(
                getUUID(), FORMAT, "00", "格式规范包", null, "Format specification package")

        // 舍去的旧规则包
        //val package12 = GlobalMessage(getUUID(), ONESDK, "00", "OneSDK规则包", null, "OneSDK rules package")

        return mapOf(
                NODE to objectMapper.writeValueAsString(package1),
                STYLISTIC to objectMapper.writeValueAsString(package2),
                STRICT_MODE to objectMapper.writeValueAsString(package3),
                LOGICA to objectMapper.writeValueAsString(package4),
                DEFAULT to objectMapper.writeValueAsString(package5),
                VARIABLE to objectMapper.writeValueAsString(package7),
                ES6 to objectMapper.writeValueAsString(package8),
                BEST_PRACTICES to objectMapper.writeValueAsString(package9),
                HEADER_FILE to objectMapper.writeValueAsString(package10),
                SYS_API to objectMapper.writeValueAsString(package11),
                // ONESDK to objectMapper.writeValueAsString(package12),
                SECURITY to objectMapper.writeValueAsString(package13),
                NAMING to objectMapper.writeValueAsString(package14),
                COMMENT to objectMapper.writeValueAsString(package15),
                FORMAT to objectMapper.writeValueAsString(package16)
        )
    }


    /**
     * 数据报表日期国际化
     */
    fun getDataReportDate(): Map<String, String> {
        val today = GlobalMessage(
                getUUID(), DATE_TODAY, "00", "今天", null, "today")
        val yesterday = GlobalMessage(
                getUUID(), DATE_YESTERDAY, "00", "昨天", null, "yesterday")
        val monday = GlobalMessage(
                getUUID(), DATE_MONDAY, "00", "周一", null, "monday")
        val lastMonday = GlobalMessage(
                getUUID(), DATE_LAST_MONDAY, "00", "上周一", null, "last monday")

        return mapOf(
                DATE_TODAY to objectMapper.writeValueAsString(today),
                DATE_YESTERDAY to objectMapper.writeValueAsString(yesterday),
                DATE_MONDAY to objectMapper.writeValueAsString(monday),
                DATE_LAST_MONDAY to objectMapper.writeValueAsString(lastMonday)
        )
    }


    /**
     * 工具描述国际化
     */
    fun getToolDescription(): Map<String, String> {
        val tool1 = GlobalMessage(
                getUUID(), Tool.CHECKSTYLE.name, "00", "用于检查Java源代码是否符合编码规范。它可以找到类和方法设计问题，还能够检查代码布局和格式问题", null,
                "Used to check if the Java source code conforms to the encoding specification. It can find class and method design problems, as well as check code layout and format problems.")
        val tool2 = GlobalMessage(
                getUUID(), Tool.CPPLINT.name, "00", "谷歌开源的C++代码风格检查工具，可确保C++代码符合谷歌编码规范，并能检查语法错误", null,
                "A Google's open source C++ code style inspection tool, which can ensure that C++ codes conform to the Google encoding specification and can check for syntax errors.")
        val tool3 = GlobalMessage(
                getUUID(), Tool.DETEKT.name, "00", "Kotlin语言代码分析工具，除了能扫出编码的风格规范问题之外，还能检查出代码的复杂度、某些潜在逻辑错误以及性能问题，告警类型多达152种", null,
                "A Kotlin language code analysis tool, which, in addition to finding out the style and specification problems of a code, can also check out the complexity, some potential logic errors and performance problems of the code, providing up to 152 types of alarms")
        val tool4 = GlobalMessage(
                getUUID(), Tool.STYLECOP.name, "00", "微软的开源静态代码分析工具，它检查C＃代码是否符合StyleCop推荐的编码样式和Microsoft .NET Framework设计指南", null,
                "A Microsoft's open source static code analysis tool, which checks if C# code conforms to the coding style suggested by StyleCop and the Microsoft .NET Framework design guide.")
        val tool5 = GlobalMessage(
                getUUID(), Tool.ESLINT.name, "00", "开源的JavaScript代码检查工具，可以在开发阶段发现代码问题，支持最新的ES6语法标准，支持前端框架Vue和React等", null,
                "An open source JavaScript code inspection tool, which can detect code issues during development, supporting the latest ES6 syntax standards, and front-end frameworks Vue and React, etc.")
        val tool6 = GlobalMessage(
                getUUID(), Tool.GOML.name, "00", "一款开源的Golang代码检查工具，支持检查代码规范、死代码、语法错误和安全漏洞等问题", null,
                "An open source Golang code inspection tool, which can detect code specification problems, dead code, syntax errors, and security vulnerabilities")
        val tool7 = GlobalMessage(
                getUUID(), Tool.PYLINT.name, "00", "Python代码风格检查工具，可检查代码行的长度、变量命名是否符合编码规范或声明的接口是否被真正的实现等", null,
                "A Python code style inspection tool, which can check if the length of the line of code and the variable naming conform to the encoding specification or if the declared interface is actually implemented, etc.")
        val tool8 = GlobalMessage(
                getUUID(), Tool.CCN.name, "00", "通过计算函数的节点个数来衡量代码复杂性。复杂度越高代码存在缺陷的风险越大", null,
                "The code complexity is measured by counting the number of nodes in the function. The higher the complexity, the greater the risk of code defects")
        val tool9 = GlobalMessage(
                getUUID(), Tool.DUPC.name, "00", "可以检测项目中复制粘贴和重复开发相同功能等问题，帮助开发者发现冗余代码，以便代码抽象和重构", null,
                "Can detect problems such as copy and paste and duplicate development of the same function in the project, help developers find redundant codes, so as to realize code abstraction and reconstruction")
        val tool10 = GlobalMessage(
                getUUID(), Tool.SPOTBUGS.name, "00", "SpotBugs是一款静态分析工具，可查找Java代码中的缺陷", null,
                "SpotBugs is a static analysis tool for finding defects in Java code")
        val tool11 = GlobalMessage(
                getUUID(), Tool.KLOCWORK.name, "00", "业界广泛使用的商用代码检查工具，可与Coverity互为补充，通过覆盖更多的逻辑路径，能更全面地扫描空指针、内存泄漏、越界溢出等问题", null,
                "A commercial code inspection tool widely used in the industry, which can complement Covery, and provide more complete scanning for null pointers, memory leaks, out-of-bounds and genoverflows by covering more logical paths.")
        val tool12 = GlobalMessage(
                getUUID(), Tool.SENSITIVE.name, "00", "由互娱运营安全中心打造的工具，可扫描对外代码中有安全风险的敏感信息，如密码泄露、内部IP泄露等", null,
                "A tool created by the IEG Operation Security Center, which can scan sensitive information in external code that has security risks, such as password leakage, internal IP leakage, etc")
        val tool13 = GlobalMessage(
                getUUID(), Tool.PHPCS.name, "00", "PHP_CodeSniffer用于检查PHP的编码规范。PHPCS支持包括PEAR、PSR-1、PSR-2、PSR-12等5类代码规范标准，涵盖257种告警类型", null,
                "PHP_CodeSniffer is used to check PHP's encoding specifications. PHPCS supports 5 types of code specifications including PEAR, PSR-1, PSR-2, PSR-12, etc., covering 257 alarm types")
        val tool14 = GlobalMessage(
                getUUID(), Tool.COVERITY.name, "00", "斯坦福大学科学家研究成果，被第三方权威调查机构VDC评为静态源代码分析领域的领导者", null,
                "The research results of scientists in Stanford University, rated by the third-party authoritative research organization VDC as the leader in the field static source code analysis")
        val tool15 = GlobalMessage(
                getUUID(), Tool.OCCHECK.name, "00", "OCCheck是一款基于ANTLR4的Objective-C代码分析工具，可检查Obj-C常见的风格规范问题，目前支持11种告警类型", null,
                "OCCheck is an Objective-C code analysis tool based on ANTLR4. It can check common style and specification problems in Obj-C. Currently it supports 11 types of alarms.")
        val tool16 = GlobalMessage(
                getUUID(), Tool.WOODPECKER_SENSITIVE.name, "00", "由TEG安全平台部打造的工具，专注于检测代码及配置文件中存在的敏感信息，包括密钥证书、员工信息、可能对公司造成负面影响的不和谐内容等", null,
                "A tool created by the TEG Security Platform Department, which focuses on detecting sensitive information in code and configuration files, including key certificates, employee information, and discordant content that may negatively affect the company, etc.")
        val tool17 = GlobalMessage(
                getUUID(), Tool.HORUSPY.name, "00", "由TEG安全平台部打造的工具，专注于检测代码中存在的开源组件漏洞", null,
                "A tool created by the TEG Security Platform Department, focused on detecting open source component vulnerabilities in code")
        val tool18 = GlobalMessage(
                getUUID(), Tool.PINPOINT.name, "00", "提供深度静态分析能力，可快速发现代码库中的质量和安全问题。广泛支持make、bazel等多种构建方式", null,
                "Provides deep static analysis capabilities to quickly discover quality and security issues in the code base. Extensive support for multiple build methods such as make and bazel")
        val tool19 = GlobalMessage(
                getUUID(), Tool.RIPS.name, "00", "由TEG安全平台部提供的工具，业界最流行的PHP静态代码检查工具之一，可自动检测PHP应用程序中的安全漏洞", null,
                "A tool provided by the TEG Security Platform Department, one of the most popular PHP static code inspection tools in the industry, can automatically detect security vulnerabilities in PHP applications")
        return mapOf(
                Tool.CHECKSTYLE.name to objectMapper.writeValueAsString(tool1),
                Tool.CPPLINT.name to objectMapper.writeValueAsString(tool2),
                Tool.DETEKT.name to objectMapper.writeValueAsString(tool3),
                Tool.STYLECOP.name to objectMapper.writeValueAsString(tool4),
                Tool.ESLINT.name to objectMapper.writeValueAsString(tool5),
                Tool.GOML.name to objectMapper.writeValueAsString(tool6),
                Tool.PYLINT.name to objectMapper.writeValueAsString(tool7),
                Tool.CCN.name to objectMapper.writeValueAsString(tool8),
                Tool.DUPC.name to objectMapper.writeValueAsString(tool9),
                Tool.SPOTBUGS.name to objectMapper.writeValueAsString(tool10),
                Tool.KLOCWORK.name to objectMapper.writeValueAsString(tool11),
                Tool.SENSITIVE.name to objectMapper.writeValueAsString(tool12),
                Tool.PHPCS.name to objectMapper.writeValueAsString(tool13),
                Tool.COVERITY.name to objectMapper.writeValueAsString(tool14),
                Tool.OCCHECK.name to objectMapper.writeValueAsString(tool15),
                Tool.WOODPECKER_SENSITIVE.name to objectMapper.writeValueAsString(tool16),
                Tool.HORUSPY.name to objectMapper.writeValueAsString(tool17),
                Tool.PINPOINT.name to objectMapper.writeValueAsString(tool18),
                Tool.RIPS.name to objectMapper.writeValueAsString(tool19)
        )
    }


    /**
     * 工具参数国际化
     */
    fun getToolParams(): Map<String, String> {
        val eslintKey = "${Tool.ESLINT.name}:$PARAM_ESLINT_RC"
        val gomlGoKey = "${Tool.GOML.name}:$PARAM_GOML_GO_PATH"
        val gomlRelKey = "${Tool.GOML.name}:$PARAM_GOML_REL_PATH"
        val pylintKey = "${Tool.PYLINT.name}:$PARAM_PYLINT_PY_VERSION"
        val spTypeKey = "${Tool.SPOTBUGS.name}:$PARAM_SPOTBUGS_SCRIPT_TYPE"
        val spContentKey = "${Tool.SPOTBUGS.name}:$PARAM_SPOTBUGS_SCRIPT_CONTENT"
        val phpcsKey = "${Tool.PHPCS.name}:$PARAM_PHPCS_XX"

        val param1 = GlobalMessage(
                getUUID(), eslintKey, "00", "项目框架", null, "Project framework")
        val param2 = GlobalMessage(
                getUUID(), gomlGoKey, "00", "go_path", null, "go_path")
        val param3 = GlobalMessage(
                getUUID(), gomlRelKey, "00", "代码存放路径", null, "Code storage path")
        val param4 = GlobalMessage(
                getUUID(), pylintKey, "00", "Python版本", null, "Python version")
        val param5 = GlobalMessage(
                getUUID(), spTypeKey, "00", "脚本类型", null, "Script type")
        val param6 = GlobalMessage(
                getUUID(), spContentKey, "00", "SpotBugs脚本", null, "SpotBugs script")
        val param7 = GlobalMessage(
                getUUID(), phpcsKey, "00", "PHP规范", null, "PHP specification")

        return mapOf(
                eslintKey to objectMapper.writeValueAsString(param1),
                gomlGoKey to objectMapper.writeValueAsString(param2),
                gomlRelKey to objectMapper.writeValueAsString(param3),
                pylintKey to objectMapper.writeValueAsString(param4),
                spTypeKey to objectMapper.writeValueAsString(param5),
                spContentKey to objectMapper.writeValueAsString(param6),
                phpcsKey to objectMapper.writeValueAsString(param7)
        )
    }


    /**
     * 工具参数国际化
     */
    fun getToolParamsTips(): Map<String, String> {
        val gomlGoKey = "${Tool.GOML.name}:$PARAM_GOML_GO_PATH"
        val gomlRelKey = "${Tool.GOML.name}:$PARAM_GOML_REL_PATH"
        val spContentKey = "${Tool.SPOTBUGS.name}:$PARAM_SPOTBUGS_SCRIPT_CONTENT"

        val tip1 = GlobalMessage(
                getUUID(), gomlGoKey, "00", "若正确配置GOPATH要求代码拉取到指定目录下，请填写。", null, "If the GOPATH is correctly configured and the code is pulled to the specified directory, please fill it out.")
        val tip2 = GlobalMessage(
                getUUID(), gomlRelKey, "00", "帮助Gometalinter查找代码依赖库，更好地扫描告警。支持输入多条，请用英文分号分隔。默认为\$workspace。", null, "Help Gometalinter find code dependencies to better scan for alarms. Support input of multiple items. Please separate them with semicolons. The default is \$workspace.")
        val tip3 = GlobalMessage(
                getUUID(), spContentKey, "00", "# 您可以通过setEnv函数设置原子间传递的参数\\n# setEnv \\FILENAME\\ \\package.zip\\\\n# 然后在后续的原子的表单中使用${FILENAME}引用这个变量\\n# 请使用依赖的构建工具如maven/cmake等写一个编译脚本build.sh\\n# 确保build.sh能够编译代码\\n# cd path/to/build.sh\\n# sh build.sh", null,
                "# You can use the setEnv function to set the parameters passed between the atoms \\n# setEnv \\FILENAME\\ \\package.zip\\\\n# and then use ${FILENAME} in the subsequent atomic form to reference the variable\\n#  Please use the dependent build tools such as maven/cmake, etc. to write a compile script build.sh\\n# and make sure build.sh can compile code\\n# cd path/to/build.sh\\n# sh build.sh")

        return mapOf(
                gomlGoKey to objectMapper.writeValueAsString(tip1),
                gomlRelKey to objectMapper.writeValueAsString(tip2),
                spContentKey to objectMapper.writeValueAsString(tip3)
        )
    }


    /**
     * 获取操作类型
     */
    fun getOperTypeMap(): Map<String, String> {
        val operType1 = GlobalMessage(
                getUUID(), REGISTER_TOOL, "00", "注册工具", null, "registering tool")
        val operType2 = GlobalMessage(
                getUUID(), MODIFY_INFO, "00", "修改信息", null, "modifying information")
        val operType3 = GlobalMessage(
                getUUID(), ENABLE_ACTION, "00", "启用", null, "enabling")
        val operType4 = GlobalMessage(
                getUUID(), DISABLE_ACTION, "00", "禁用", null, "disabling")
        val operType5 = GlobalMessage(
                getUUID(), TRIGGER_ANALYSIS, "00", "触发分析", null, "triggering analysis")
        val operType6 = GlobalMessage(
                getUUID(), AUTHOR_TRANSFER, "00", "作者转换", null, "author transfering")
        val operType7 = GlobalMessage(
                getUUID(), OPEN_CHECKER, "00", "打开规则", null, "open checker")
        val operType8 = GlobalMessage(
                getUUID(), CLOSE_CHECKER, "00", "关闭规则", null, "close checker")

        return mapOf(
                REGISTER_TOOL to objectMapper.writeValueAsString(operType1),
                MODIFY_INFO to objectMapper.writeValueAsString(operType2),
                ENABLE_ACTION to objectMapper.writeValueAsString(operType3),
                DISABLE_ACTION to objectMapper.writeValueAsString(operType4),
                TRIGGER_ANALYSIS to objectMapper.writeValueAsString(operType5),
                AUTHOR_TRANSFER to objectMapper.writeValueAsString(operType6),
                OPEN_CHECKER to objectMapper.writeValueAsString(operType7),
                CLOSE_CHECKER to objectMapper.writeValueAsString(operType8)
        )
    }


    fun getCheckerDescMap(): Map<String, String> {
        val param1 = GlobalMessage(getUUID(), "for-direction", "00", "禁止 for 循环出现方向错误的循环，比如 for (i = 0; i < 10; i--)", null, "It is forbidden to have a loop with incorrect direction in for loops, such as for (i = 0; i < 10; i--)")
        val param2 = GlobalMessage(getUUID(), "id-length", "00", "限制变量名长度", null, "Limit the length of variable name")
        val param3 = GlobalMessage(getUUID(), "vue/max-attributes-per-line", "00", "限制每行允许的最多属性数量", null, "Limit the maximum number of attributes allowed per line")
        val param4 = GlobalMessage(getUUID(), "vue/no-reservered-keys", "00", "禁止覆盖保留字", null, "It is forbidden to cover reserved word")
        val param5 = GlobalMessage(getUUID(), "react/no-array-index-key", "00", "禁止使用数组的 index 作为 key", null, "It is forbidden to use the index of an array as the key.")
        val param6 = GlobalMessage(getUUID(), "semi-style", "00", "分号必须写在行尾，禁止在行首出现", null, "Semicolon must be written at the end of the line, and is forbidden to appear at the beginning of the line.")
        val param7 = GlobalMessage(getUUID(), "unicode-bom", "00", "文件开头禁止有 BOM", null, "BOM is forbidden at the beginning of the file")
        val param8 = GlobalMessage(getUUID(), "vue/valid-v-bind", "00", "v-bind 指令必须合法", null, "The v-bind instructions must be legal")
        val param9 = GlobalMessage(getUUID(), "consistent-return", "00", "禁止函数在不同分支返回不同类型的值", null, "Prevent functions from returning different types of values on different branches")
        val param10 = GlobalMessage(getUUID(), "no-const-assign", "00", "禁止对使用 const 定义的常量重新赋值", null, "It is forbidden to reassign constants defined using const")
        val param11 = GlobalMessage(getUUID(), "computed-property-spacing", "00", "用作对象的计算属性时，中括号内的首尾禁止有空格", null, "When it is used as the computed property of an object, spaces are not allowed at the beginning and end in the bracket")
        val param12 = GlobalMessage(getUUID(), "consistent-this", "00", "限制 this 的别名", null, "Limit the alias of this")
        val param13 = GlobalMessage(getUUID(), "vue/no-parsing-error", "00", "禁止出现语法错误", null, "It is forbidden to have grammatical errors")
        val param14 = GlobalMessage(getUUID(), "react/no-render-return-value", "00", "禁止使用 ReactDOM.render 的返回值", null, "It is forbidden to use the return value of ReactDOM.render")
        val param15 = GlobalMessage(getUUID(), "no-constant-condition", "00", "禁止将常量作为 if 或三元表达式的测试条件，比如 if (true), let foo = 0 ? 'foo' : 'bar'", null, "It is forbidden to use constants as test conditions for if or ternary expressions, such as if (true), let foo = 0 ? 'foo' : 'bar'")
        val param16 = GlobalMessage(getUUID(), "no-confusing-arrow", "00", "禁止出现难以理解的箭头函数，比如 let x = a => 1 ? 2 : 3", null, "It is forbidden to have an incomprehensible arrow function, such as let x = a => 1 ? 2 : 3")
        val param17 = GlobalMessage(getUUID(), "react/jsx-max-props-per-line", "00", "限制每行的 props 数量", null, "Limit the number of props per line")
        val param18 = GlobalMessage(getUUID(), "no-continue", "00", "禁止使用 continue", null, "It is forbidden to use continue")
        val param19 = GlobalMessage(getUUID(), "vue/jsx-uses-vars", "00", "定义了的 jsx element 必须使用", null, "The defined jsx element must be used")
        val param20 = GlobalMessage(getUUID(), "no-param-reassign", "00", "禁止对函数的参数重新赋值", null, "It is forbidden to reassign the parameters of a function")
        val param21 = GlobalMessage(getUUID(), "callback-return", "00", "callback 之后必须立即 return", null, "return immediately after callback")
        val param22 = GlobalMessage(getUUID(), "default-case", "00", "switch 语句必须有 default", null, "The switch statement must have a default")
        val param23 = GlobalMessage(getUUID(), "linebreak-style", "00", "限制换行符为 LF 或 CRLF", null, "Limit line breaks to be LF or CRLF")
        val param24 = GlobalMessage(getUUID(), "spaced-comment", "00", "注释前后必须有空格", null, "There must be spaces in front and rear of an comment")
        val param25 = GlobalMessage(getUUID(), "array-bracket-newline", "00", "配置数组的中括号内前后的换行格式", null, "Configure the front and rear line feed format in the square bracket of array")
        val param26 = GlobalMessage(getUUID(), "license", "00", "文件头需要包含开源协议信息", null, "The file header needs to contain open source protocol information")
        val param27 = GlobalMessage(getUUID(), "operator-linebreak", "00", "需要换行的时候，操作符必须放在行末", null, "When a line feed is required, the operator must be placed at the end of the line")
        val param28 = GlobalMessage(getUUID(), "array-bracket-spacing", "00", "数组的括号内的前后禁止有空格", null, "Spaces are not allowed in front and rear of the parentheses of an array")
        val param29 = GlobalMessage(getUUID(), "vue/no-shared-component-data", "00", "组件的 data 属性的值必须是一个函数", null, "The value of data attribute of a component must be a function")
        val param30 = GlobalMessage(getUUID(), "sort-keys", "00", "对象字面量的键名必须排好序", null, "The key names of object literals must be sorted")
        val param31 = GlobalMessage(getUUID(), "no-script-url", "00", "禁止出现 location.href = 'javascript:void(0)';", null, "It is forbidden to have location.href = 'javascript:void(0)';")
        val param32 = GlobalMessage(getUUID(), "no-undef", "00", "禁止使用未定义的变量", null, "It is forbidden to use undefined variables")
        val param33 = GlobalMessage(getUUID(), "comma-spacing", "00", "逗号前禁止有空格，逗号后必须要有空格", null, "No spaces are allowed in front of the comma, and there must be a space in rear of the comma")
        val param34 = GlobalMessage(getUUID(), "vue/v-bind-style", "00", "限制 v-bind 的风格", null, "Limit the style of v-bind")
        val param35 = GlobalMessage(getUUID(), "vue/require-valid-default-prop", "00", "prop 的默认值必须匹配它的类型", null, "The default value of prop must match its type")
        val param36 = GlobalMessage(getUUID(), "no-useless-constructor", "00", "禁止出现没必要的 constructor，比如 constructor(value) { super(value) }", null, "It is forbidden to have unnecessary constructors, such as constructor(value) { super(value) }")
        val param37 = GlobalMessage(getUUID(), "prefer-spread", "00", "必须使用 ... 而不是 apply，比如 foo(...args)", null, "Must use ... instead of apply, such as foo(...args)")
        val param38 = GlobalMessage(getUUID(), "no-labels", "00", "禁止使用 label", null, "It is forbidden to use label")
        val param39 = GlobalMessage(getUUID(), "no-undefined", "00", "禁止对 undefined 重新赋值", null, "It is forbidden to reassign undefined")
        val param40 = GlobalMessage(getUUID(), "no-unused-vars", "00", "定义过的变量必须使用", null, "The defined variables must be used")
        val param41 = GlobalMessage(getUUID(), "vue/no-async-in-computed-properties", "00", "计算属性禁止包含异步方法", null, "The computed properties are forbidden to contain asynchronous methods")
        val param42 = GlobalMessage(getUUID(), "vue/require-render-return", "00", "render 函数必须有返回值", null, "The render function must have a return value")
        val param43 = GlobalMessage(getUUID(), "vue/name-property-casing", "00", "限制组件的 name 属性的值的风格", null, "Limit the style of the value of name attribute of a component")
        val param44 = GlobalMessage(getUUID(), "no-new-object", "00", "禁止直接 new Object", null, "Direct new object is forbidden")
        val param45 = GlobalMessage(getUUID(), "no-trailing-spaces", "00", "禁止行尾有空格", null, "It is forbidden to have spaces at the end of a line")
        val param47 = GlobalMessage(getUUID(), "block-spacing", "00", "代码块如果在一行内，那么大括号内的首尾必须有空格，比如 function () { alert('Hello') }", null, "If the code block is on a line, there must be spaces in the beginning and end of the brace, such as function () { alert('Hello') }")
        val param48 = GlobalMessage(getUUID(), "react/no-typos", "00", "禁止拼写错误", null, "Spelling mistakes are forbidden")
        val param49 = GlobalMessage(getUUID(), "prefer-numeric-literals", "00", "必须使用 0b11111011 而不是 parseInt('111110111', 2)", null, "Must use 0b11111011 instead of parseInt('111110111', 2)")
        val param50 = GlobalMessage(getUUID(), "react/jsx-boolean-value", "00", "布尔值的属性必须显式的写 someprop={true}", null, "Boolean value properties must be explicitly written as someprop={true}")
        val param51 = GlobalMessage(getUUID(), "space-before-blocks", "00", "if, function 等的大括号之前必须要有空格，比如 if (a) {", null, "There must be a space in front of the braces of if, function, etc., such as if (a) {")
        val param52 = GlobalMessage(getUUID(), "func-name-matching", "00", "函数赋值给变量的时候，函数名必须与变量名一致", null, "When a function is assigned to a variable, the function name must match the variable name")
        val param53 = GlobalMessage(getUUID(), "no-sync", "00", "禁止使用 node 中的同步的方法，比如 fs.readFileSync", null, "It is forbidden to use the synchronization method in node, such as fs.readFileSync")
        val param54 = GlobalMessage(getUUID(), "no-self-compare", "00", "禁止将自己与自己比较", null, "It is forbidden to compare yourself with yourself")
        val param55 = GlobalMessage(getUUID(), "no-void", "00", "禁止使用 void", null, "It is forbidden to use void")
        val param56 = GlobalMessage(getUUID(), "react/no-did-mount-set-state", "00", "禁止在 componentDidMount 里面使用 setState", null, "It is forbidden to use setState in componentDidMount")
        val param57 = GlobalMessage(getUUID(), "react/no-unused-prop-types", "00", "禁止出现未使用的 propTypes", null, "   It is forbidden to have unused propTypes")
        val param58 = GlobalMessage(getUUID(), "react/jsx-closing-bracket-location", "00", "自闭和标签的反尖括号必须与尖括号的那一行对齐", null, "   The opposite angle bracket of the self-closing label must be aligned with the line of angle brackets")
        val param59 = GlobalMessage(getUUID(), "dot-location", "00", "链式调用的时候，点号必须放在第二行开头处，禁止放在第一行结尾处", null, "At method chaining, the dot must be placed at the beginning of the second line, and it is forbidden to be placed at the end of the first line")
        val param60 = GlobalMessage(getUUID(), "react/no-unescaped-entities", "00", "禁止在组件的内部存在未转义的 >, \", \' 或 }", null, "It is forbidden to have unescaped >, ”, ' or } inside the component")
        val param61 = GlobalMessage(getUUID(), "react/jsx-uses-react", "00", "jsx 文件必须 import React", null, "import React is a must for jsx file")
        val param62 = GlobalMessage(getUUID(), "no-useless-return", "00", "禁止没必要的 return", null, "It is forbidden to have unnecessary return")
        val param63 = GlobalMessage(getUUID(), "max-statements-per-line", "00", "限制一行中的语句数量", null, "Limit the number of statements in a line")
        val param64 = GlobalMessage(getUUID(), "react/jsx-closing-tag-location", "00", "结束标签必须与开始标签的那一行对齐", null, "The closing tag must be aligned with the line of the opening tag")
        val param65 = GlobalMessage(getUUID(), "complexity", "00", "禁止函数的循环复杂度超过 20", null, "The loop complexity of function is forbidden to exceed 20")
        val param66 = GlobalMessage(getUUID(), "vue/valid-v-else-if", "00", "v-else-if 指令必须合法", null, "The v-else-if instruction must be legal")
        val param67 = GlobalMessage(getUUID(), "no-underscore-dangle", "00", "禁止变量名出现下划线", null, "Underlining variable names is forbidden")
        val param68 = GlobalMessage(getUUID(), "no-with", "00", "禁止使用 with", null, "It is forbidden to use with")
        val param69 = GlobalMessage(getUUID(), "new-parens", "00", "new 后面的类必须有小括号", null, "The class after new must have parentheses")
        val param70 = GlobalMessage(getUUID(), "no-implicit-coercion", "00", "禁止使用 !! ~ 等难以理解的运算符", null, "It is forbidden to use !! ~ and other incomprehensible operators")
        val param71 = GlobalMessage(getUUID(), "vue/valid-v-if", "00", "v-if 指令必须合法", null, "The v-if instruction must be legal")
        val param72 = GlobalMessage(getUUID(), "no-useless-escape", "00", "禁止出现没必要的转义", null, "Unnecessary escaping is forbidden")
        val param73 = GlobalMessage(getUUID(), "object-shorthand", "00", "必须使用 a = {b} 而不是 a = {b: b}", null, "Must use a = {b} instead of a = {b: b}")
        val param74 = GlobalMessage(getUUID(), "no-alert", "00", "禁止使用 alert", null, "It is forbidden to use alert")
        val param75 = GlobalMessage(getUUID(), "no-obj-calls", "00", "禁止将 Math, JSON 或 Reflect 直接作为函数调用，必须作为类使用", null, "It is forbidden to call Math, JSON or Reflect directly as a function, and they must be used as a class")
        val param76 = GlobalMessage(getUUID(), "no-proto", "00", "禁止使用 __proto__", null, "It is forbidden to use __proto__")
        val param77 = GlobalMessage(getUUID(), "no-multi-str", "00", "禁止使用 \\ 来换行字符串", null, "It is forbidden to use \\ for linefeed of character strings.")
        val param78 = GlobalMessage(getUUID(), "no-control-regex", "00", "禁止在正则表达式中出现 Ctrl 键的 ASCII 表示，即禁止使用 /\\x1f/", null, "It is forbidden to have the ASCII representation of Ctrl key in regular expressions, that is, it is forbidden to use /\\x1f/")
        val param79 = GlobalMessage(getUUID(), "prefer-rest-params", "00", "必须使用 ...args 而不是 arguments", null, "Must use ...args instead of arguments")
        val param80 = GlobalMessage(getUUID(), "comma-dangle", "00", "对象的最后一个属性末尾必须有逗号", null, "The last attribute of the object must have a comma at the end")
        val param81 = GlobalMessage(getUUID(), "no-self-assign", "00", "禁止将自己赋值给自己", null, "It is forbidden to assign yourself to yourself")
        val param82 = GlobalMessage(getUUID(), "lines-around-comment", "00", "注释前后必须有空行", null, "There must be a blank line in front and rear of an comment")
        val param83 = GlobalMessage(getUUID(), "comma-style", "00", "禁止在行首写逗号", null, "It is forbidden to write a comma at the beginning of the line")
        val param84 = GlobalMessage(getUUID(), "react/no-danger-with-children", "00", "禁止在使用了 dangerouslySetInnerHTML 的组建内添加 children", null, "It is forbidden to add children to a component that uses dangerouslySetInnerHTML")
        val param85 = GlobalMessage(getUUID(), "one-var-declaration-per-line", "00", "变量申明必须每行一个", null, "The variable declaration must be one per line")
        val param86 = GlobalMessage(getUUID(), "react/jsx-tag-spacing", "00", "jsx 的开始和闭合处禁止有空格", null, "Spaces are not allowed at the beginning and end of jsx")
        val param87 = GlobalMessage(getUUID(), "no-octal", "00", "禁止使用 0 开头的数字表示八进制数", null, "It is forbidden to use numbers starting with 0 to indicate octal numbers")
        val param88 = GlobalMessage(getUUID(), "react/no-is-mounted", "00", "禁止使用 isMounted", null, "It is forbidden to use isMounted")
        val param89 = GlobalMessage(getUUID(), "no-mixed-operators", "00", "禁止混用不同的操作符，比如 let foo = a && b < 0 || c > 0 || d + 1 === 0", null, "It is forbidden to mix different operators, such as let foo = a && b < 0 || c > 0 || d + 1 === 0")
        val param90 = GlobalMessage(getUUID(), "no-eval", "00", "禁止使用 eval", null, "It is forbidden to eval")
        val param91 = GlobalMessage(getUUID(), "no-mixed-spaces-and-tabs", "00", "禁止混用空格和缩进", null, "It is forbidden to mix spaces and indents")
        val param92 = GlobalMessage(getUUID(), "max-nested-callbacks", "00", "回调函数嵌套禁止超过 3 层，多了请用 async await 替代", null, "The callback function nesting is forbidden to exceed 3 layers. If it exceeds 3 layers, use async await instead")
        val param93 = GlobalMessage(getUUID(), "newline-per-chained-call", "00", "链式调用必须换行", null, "Linefeed is a must for method chaining")
        val param94 = GlobalMessage(getUUID(), "no-compare-neg-zero", "00", "禁止与负零进行比较", null, "It is forbidden to compare with negative zero")
        val param95 = GlobalMessage(getUUID(), "react/jsx-sort-props", "00", "props 必须排好序", null, "props must be sorted")
        val param96 = GlobalMessage(getUUID(), "eol-last", "00", "文件最后一行必须有一个空行", null, "The last line of the file must be followed by a blank line")
        val param97 = GlobalMessage(getUUID(), "react/forbid-foreign-prop-types", "00", "禁止直接使用别的组建的 propTypes", null, "It is forbidden to directly use the propTypes of other component")
        val param98 = GlobalMessage(getUUID(), "no-label-var", "00", "禁止 label 名称与定义过的变量重复", null, "The label name is forbidden to be duplicated with the defined variable")
        val param99 = GlobalMessage(getUUID(), "react/prefer-es6-class", "00", "必须使用 Class 的形式创建组件", null, "Components must be created in the form of a Class")
        val param100 = GlobalMessage(getUUID(), "vue/valid-v-show", "00", "v-show 指令必须合法", null, "The v-show instruction must be legal")
        val param101 = GlobalMessage(getUUID(), "quotes", "00", "必须使用单引号，禁止使用双引号", null, "Single quotes must be used, double quotes are forbidden.")
        val param102 = GlobalMessage(getUUID(), "no-unreachable", "00", "禁止在 return, throw, break 或 continue 之后还有代码", null, "It is forbidden to have code after return, throw, break or continue")
        val param103 = GlobalMessage(getUUID(), "wrap-regex", "00", "正则表达式必须有括号包起来", null, "Regular expressions must be enclosed in parentheses")
        val param104 = GlobalMessage(getUUID(), "no-invalid-regexp", "00", "禁止出现非法的正则表达式", null, "It is forbidden to have illegal regular expressions")
        val param105 = GlobalMessage(getUUID(), "generator-star-spacing", "00", "generator 的 * 前面禁止有空格，后面必须有空格", null, "Spaces are forbidden before the * of generator, and there must be spaces after it")
        val param106 = GlobalMessage(getUUID(), "no-bitwise", "00", "禁止使用位运算", null, " It is forbidden to use bit operations")
        val param107 = GlobalMessage(getUUID(), "react/jsx-indent", "00", "jsx 的 children 缩进必须为四个空格", null, "The children indentation of jsx must be four spaces")
        val param108 = GlobalMessage(getUUID(), "vars-on-top", "00", "var 必须在作用域的最前面", null, "var must be at the forefront of the scope")
        val param109 = GlobalMessage(getUUID(), "vue/no-textarea-mustache", "00", "禁止在 <textarea> 中出现 {{message}}", null, "It is forbidden to have {{message}} in <textarea>")
        val param110 = GlobalMessage(getUUID(), "vue/html-quotes", "00", "html 属性值必须用双引号括起来", null, "The html attribute value must be enclosed in double quotes")
        val param111 = GlobalMessage(getUUID(), "vue/valid-v-once", "00", "v-once 指令必须合法", null, "The v-once instruction must be legal")
        val param112 = GlobalMessage(getUUID(), "react/no-set-state", "00", "禁止使用 setState", null, "It is forbidden to use setState")
        val param113 = GlobalMessage(getUUID(), "prefer-template", "00", "必须使用模版字面量而不是字符串连接", null, "A template literal must be used instead of a string concatenation")
        val param114 = GlobalMessage(getUUID(), "no-tabs", "00", "禁止使用 tabs", null, "It is forbidden to use tabs")
        val param115 = GlobalMessage(getUUID(), "vue/no-template-key", "00", "禁止 <template> 使用 key 属性", null, "<template> is not allowed to use key attribute")
        val param116 = GlobalMessage(getUUID(), "react/jsx-curly-spacing", "00", "大括号内前后禁止有空格", null, "Spaces are not allowed at the beginning and end in braces")
        val param117 = GlobalMessage(getUUID(), "react/no-redundant-should-component-update", "00", "禁止在 PureComponent 中使用 shouldComponentUpdate", null, "It is forbidden to use shouldComponentUpdate in PureComponent")
        val param118 = GlobalMessage(getUUID(), "vue/attribute-hyphenation", "00", "限制自定义组件的属性风格", null, "Limit the attribute style of user-defined components")
        val param119 = GlobalMessage(getUUID(), "no-throw-literal", "00", "禁止 throw 字面量，必须 throw 一个 Error 对象", null, "It is forbidden to throw literals, while it is necessary to throw an Error object")
        val param120 = GlobalMessage(getUUID(), "template-tag-spacing", "00", "模版字符串的 tag 之后禁止有空格，", null, "Spaces are not allowed after the tag of the template string")
        val param121 = GlobalMessage(getUUID(), "no-unsafe-finally", "00", "禁止在 finally 中出现 return, throw, break 或 continue", null, "   It is forbidden to have return, throw, break or continue in finally")
        val param122 = GlobalMessage(getUUID(), "no-nested-ternary", "00", "禁止使用嵌套的三元表达式，比如 a ? b : c ? d : e", null, "It is forbidden to use nested ternary expressions, such as a ? b : c ? d : e")
        val param123 = GlobalMessage(getUUID(), "space-unary-ops", "00", "new, typeof 等后面必须有空格，++, -- 等禁止有空格", null, "New, typeof, etc. must be followed by a space, ++, -- etc. must not be followed by a space")
        val param124 = GlobalMessage(getUUID(), "vue/valid-v-for", "00", "v-for 指令必须合法", null, "The v-for instruction must be legal")
        val param125 = GlobalMessage(getUUID(), "indent", "00", "一个缩进必须用四个空格替代", null, "An indentation must be replaced by four spaces")
        val param126 = GlobalMessage(getUUID(), "react/require-render-return", "00", "render 方法中必须有返回值", null, "There must be a return value in the render method")
        val param127 = GlobalMessage(getUUID(), "jsx-quotes", "00", "jsx 中的属性必须用双引号", null, "Attributes in jsx must be enclosed in double quotation marks")
        val param128 = GlobalMessage(getUUID(), "no-shadow", "00", "禁止变量名与上层作用域内的定义过的变量重复", null, "Variable names are not allowed to be duplicated with defined variables in the upper scope")
        val param129 = GlobalMessage(getUUID(), "react/no-string-refs", "00", "禁止使用字符串", null, " It is forbidden to use strings")
        val param130 = GlobalMessage(getUUID(), "no-dupe-args", "00", "禁止在函数参数中出现重复名称的参数", null, "It is forbidden to have parameters with duplicate names in function parameters")
        val param131 = GlobalMessage(getUUID(), "max-statements", "00", "限制函数块中的语句数量", null, "Limit the number of statements in a function block")
        val param132 = GlobalMessage(getUUID(), "no-restricted-modules", "00", "禁止使用指定的模块", null, "It is forbidden to use the specified module")
        val param133 = GlobalMessage(getUUID(), "require-jsdoc", "00", "必须使用 jsdoc 风格的注释", null, "Must use jsdoc-style comments")
        val param134 = GlobalMessage(getUUID(), "one-var", "00", "禁止变量申明时用逗号一次申明多个", null, "When declaring banned variables, use commas to declare more once")
        val param135 = GlobalMessage(getUUID(), "vue/valid-v-pre", "00", "v-pre 指令必须合法", null, "The v-pre instruction must be legal")
        val param136 = GlobalMessage(getUUID(), "keyword-spacing", "00", "关键字前后必须有空格", null, "There must be spaces before and after the keyword")
        val param137 = GlobalMessage(getUUID(), "handle-callback-err", "00", "callback 中的 error 必须被处理", null, "The error in the callback must be processed")
        val param138 = GlobalMessage(getUUID(), "react/no-unknown-property", "00", "禁止出现 HTML 中的属性，如 class", null, "It is forbidden to have attributes in HTML, such as class")
        val param139 = GlobalMessage(getUUID(), "no-regex-spaces", "00", "禁止在正则表达式中出现连续的空格，必须使用 /foo {3}bar/ 代替", null, "It is forbidden to have consecutive spaces in regular expressions, /foo {3}bar/ must be used instead")
        val param140 = GlobalMessage(getUUID(), "radix", "00", "parseInt 必须传入第二个参数", null, "The second parameter must be passed in parseInt")
        val param141 = GlobalMessage(getUUID(), "accessor-pairs", "00", "有 setter 的地方必须有 getter，有 getter 的地方可以没有 setter", null, "There must be a getter at the place with setter, while a setter may not be at the place with getter")
        val param142 = GlobalMessage(getUUID(), "symbol-description", "00", "创建 Symbol 时必须传入参数", null, "Parameters must be passed in when creating Symbol")
        val param143 = GlobalMessage(getUUID(), "switch-colon-spacing", "00", "case 的冒号前禁止有空格，冒号后必须有空格", null, "Spaces are not allowed before the colon of case, and there must be spaces after the colon")
        val param144 = GlobalMessage(getUUID(), "no-useless-rename", "00", "禁止解构时出现同样名字的的重命名，比如 let { foo: foo } = bar;", null, "It is forbidden to have renaming of the same name when destructuring, such as let { foo: foo } = bar;")
        val param145 = GlobalMessage(getUUID(), "no-iterator", "00", "禁止使用 __iterator__", null, "It is forbidden to use __iterator__")
        val param146 = GlobalMessage(getUUID(), "no-restricted-properties", "00", "禁止使用指定的对象属性", null, "It is forbidden to use specified object properties")
        val param147 = GlobalMessage(getUUID(), "yield-star-spacing", "00", "yield* 后面必须要有空格", null, "There must be spaces after yield*")
        val param148 = GlobalMessage(getUUID(), "react/jsx-pascal-case", "00", "禁止使用 pascal 写法的 jsx，比如 <TEST_COMPONENT>", null, "It is forbidden to use jsx written in pascal, such as <TEST_COMPONENT>")
        val param149 = GlobalMessage(getUUID(), "func-style", "00", "必须只使用函数申明或只使用函数表达式", null, " It is necessary to only use function declaration or only use function expression")
        val param150 = GlobalMessage(getUUID(), "no-lonely-if", "00", "禁止 else 中只有一个单独的 if", null, "It is forbidden to have only a single if in else")
        val param151 = GlobalMessage(getUUID(), "no-unneeded-ternary", "00", "必须使用 !a 替代 a ? false : true", null, "!a must be used instead of a ? false : true")
        val param152 = GlobalMessage(getUUID(), "no-ternary", "00", "禁止使用三元表达式", null, "It is forbidden to use ternary expressions")
        val param153 = GlobalMessage(getUUID(), "no-extra-label", "00", "禁止出现没必要的 label", null, "It is forbidden to have a unnecessary label")
        val param154 = GlobalMessage(getUUID(), "react/no-danger", "00", "禁止使用 dangerouslySetInnerHTML", null, "It is forbidden to use dangerouslySetInnerHTML")
        val param155 = GlobalMessage(getUUID(), "no-implied-eval", "00", "禁止在 setTimeout 或 setInterval 中传入字符串，如 setTimeout('alert(\"Hi!\")', 100);", null, "It is forbidden to use pass a string in setTimeout or setInterval, such as setTimeout('alert(Hi!)', 100);")
        val param156 = GlobalMessage(getUUID(), "no-array-constructor", "00", "禁止使用 Array 构造函数", null, "It is forbidden to use the Array constructor")
        val param157 = GlobalMessage(getUUID(), "no-return-await", "00", "禁止在 return 语句里使用 await", null, "It is forbidden to use await in the return statement")
        val param158 = GlobalMessage(getUUID(), "no-unused-labels", "00", "禁止出现没用的 label", null, "It is forbidden to use a useless label")
        val param159 = GlobalMessage(getUUID(), "no-new-require", "00", "禁止直接 new require('foo')", null, "Direct new require('foo') is forbidden")
        val param160 = GlobalMessage(getUUID(), "vue/return-in-computed-property", "00", "计算属性必须有返回值", null, "The computed properties must have a return value")
        val param161 = GlobalMessage(getUUID(), "no-implicit-globals", "00", "禁止在全局作用域下定义变量或申明函数", null, "It is forbidden to define variables or declaration functions under the global scope")
        val param162 = GlobalMessage(getUUID(), "no-lone-blocks", "00", "禁止使用没必要的 {} 作为代码块", null, "It is forbidden to use {} as a block of code")
        val param163 = GlobalMessage(getUUID(), "no-template-curly-in-string", "00", "禁止在普通字符串中出现模版字符串的变量形式，如 'Hello ${name}!'", null, "It is forbidden to have variable forms of template strings in ordinary strings, such as 'Hello ${name}!'")
        val param164 = GlobalMessage(getUUID(), "react/forbid-component-props", "00", "禁止在自定义组件中使用一些指定的 props", null, "It is forbidden to use some specified props in user-defined components")
        val param165 = GlobalMessage(getUUID(), "template-curly-spacing", "00", "${name} 内的首尾禁止有空格", null, "It is forbidden to type spaces at the beginning and end of ${name}")
        val param166 = GlobalMessage(getUUID(), "constructor-super", "00", "constructor 中必须有 super", null, "There must be super in the constructor")
        val param167 = GlobalMessage(getUUID(), "block-scoped-var", "00", "将 var 定义的变量视为块作用域，禁止在块外使用", null, "Treat variables defined by var as block scope and It is forbidden to use outside the block")
        val param168 = GlobalMessage(getUUID(), "no-whitespace-before-property", "00", "禁止属性前有空格，比如 foo. bar()", null, "It is forbidden to type spaces before a property, such as foo. bar()")
        val param169 = GlobalMessage(getUUID(), "react/sort-prop-types", "00", "propTypes 的熟悉必须按照字母排序", null, "The familiarity of propTypes must be sorted alphabetically")
        val param170 = GlobalMessage(getUUID(), "no-cond-assign", "00", "禁止在 if, for, while 里使用赋值语句，除非这个赋值语句被括号包起来了", null, "It is forbidden to use an assignment statement in if, for, while unless the assignment statement is enclosed in parentheses")
        val param171 = GlobalMessage(getUUID(), "no-prototype-builtins", "00", "禁止使用 hasOwnProperty, isPrototypeOf 或 propertyIsEnumerable", null, "It is forbidden to use hasOwnProperty, isPrototypeOf or propertyIsEnumerable")
        val param172 = GlobalMessage(getUUID(), "sort-imports", "00", "mport 必须按规则排序", null, "mport must be sorted by rule.")
        val param173 = GlobalMessage(getUUID(), "react/sort-comp", "00", "组件内方法必须按照一定规则排序", null, "In-component methods must be sorted according to certain rules")
        val param174 = GlobalMessage(getUUID(), "strict", "00", "禁止使用 'strict';", null, "It is forbidden to use 'strict';")
        val param175 = GlobalMessage(getUUID(), "init-declarations", "00", "变量必须在定义的时候赋值", null, "Variables must be assigned at the time of definition")
        val param176 = GlobalMessage(getUUID(), "prefer-promise-reject-errors", "00", "Promise 的 reject 中必须传入 Error 对象，而不是字面量", null, "An Error object must be passed in the reject of Promise instead of a literal")
        val param177 = GlobalMessage(getUUID(), "dot-notation", "00", "禁止出现 foo['bar']，必须写成 foo.bar", null, "It is forbidden to have Foo['bar'] and it must be written as foo.bar")
        val param178 = GlobalMessage(getUUID(), "global-require", "00", "require 必须在全局作用域下", null, "require must be under the global scope")
        val param179 = GlobalMessage(getUUID(), "no-inline-comments", "00", "禁止在代码后添加内联注释", null, "It is forbidden to add inline comments after the code")
        val param180 = GlobalMessage(getUUID(), "no-irregular-whitespace", "00", "禁止使用特殊空白符（比如全角空格），除非是出现在字符串、正则表达式或模版字符串中", null, "It is forbidden to use special whitespace characters (such as full-width spaces) unless they appear in a string, regular expression, or template string")
        val param181 = GlobalMessage(getUUID(), "no-useless-concat", "00", "禁止出现没必要的字符串连接", null, "It is forbidden to have a unnecessary string connection")
        val param182 = GlobalMessage(getUUID(), "no-sequences", "00", "禁止使用逗号操作符", null, "   It is forbidden to use the comma operator")
        val param183 = GlobalMessage(getUUID(), "no-use-before-define", "00", "变量必须先定义后使用", null, "Variables must be defined before use")
        val param184 = GlobalMessage(getUUID(), "max-lines", "00", "限制一个文件最多的行数", null, "Limit the maximum number of lines in a file")
        val param185 = GlobalMessage(getUUID(), "no-loop-func", "00", "禁止在循环内的函数中出现循环体条件语句中定义的变量，", null, "It is forbidden to have variables defined in the loop body conditional statement in the function in a loop")
        val param186 = GlobalMessage(getUUID(), "no-mixed-requires", "00", "相同类型的 require 必须放在一起", null, "The same type of require must be put together")
        val param187 = GlobalMessage(getUUID(), "max-params", "00", "函数的参数禁止超过 7 个", null, "The number of parameters of function are forbidden to exceed 7")
        val param188 = GlobalMessage(getUUID(), "no-global-assign", "00", "禁止对全局变量赋值", null, "It is forbidden to assign values to global variables")
        val param189 = GlobalMessage(getUUID(), "vue/html-self-closing", "00", "没有内容时，组件必须自闭和", null, "Components must be self-closed when there is no content")
        val param190 = GlobalMessage(getUUID(), "no-empty-function", "00", "不允许有空函数，除非是将一个空函数设置为某个项的默认值", null, "Empty functions are not allowed unless an empty function is set to the default value of a certain item")
        val param191 = GlobalMessage(getUUID(), "react/no-find-dom-node", "00", "禁止使用 findDOMNode", null, "It is forbidden to use findDOMNode")
        val param192 = GlobalMessage(getUUID(), "react/prop-types", "00", "组件必须写 propTypes", null, "propTypes must be written in components")
        val param193 = GlobalMessage(getUUID(), "semi-spacing", "00", "一行有多个语句时，分号前面禁止有空格，分号后面必须有空格", null, "When there are multiple statements in a line, spaces are forbidden in front of the semicolon, and a space must be followed by a semicolon")
        val param194 = GlobalMessage(getUUID(), "line-comment-position", "00", "单行注释必须写在上一行", null, "Single line comments must be written on the previous line")
        val param195 = GlobalMessage(getUUID(), "no-unsafe-negation", "00", "禁止在 in 或 instanceof 操作符的左侧使用感叹号，如 if (!key in object)", null, "It is forbidden to use an exclamation point on the left side of the in or instanceof operator, such as if (!key in object)")
        val param196 = GlobalMessage(getUUID(), "no-shadow-restricted-names", "00", "禁止使用保留字作为变量名", null, "It is forbidden to use reserved words as variable names")
        val param197 = GlobalMessage(getUUID(), "quote-props", "00", "对象字面量的键名禁止用引号括起来", null, "It is forbidden to enclose the key name of object literal in quotes")
        val param198 = GlobalMessage(getUUID(), "nonblock-statement-body-position", "00", "禁止 if 后面不加大括号而写两行代码", null, "It is forbidden to write two lines of code without adding a brace after if")
        val param199 = GlobalMessage(getUUID(), "no-new", "00", "禁止直接 new 一个类而不赋值", null, "It is forbidden to directly new a class without assigning a value.")
        val param200 = GlobalMessage(getUUID(), "react/prefer-stateless-function", "00", "必须使用 pure function", null, "pure function must be used")
        val param201 = GlobalMessage(getUUID(), "object-curly-spacing", "00", "对象字面量只有一行时，大括号内的首尾必须有空格", null, "When there is only one line of object literals, spaces must be typed at the beginning and end in the braces")
        val param202 = GlobalMessage(getUUID(), "react/jsx-wrap-multilines", "00", "多行的 jsx 必须有括号包起来", null, "Multi-line jsx must be enclosed in parentheses")
        val param203 = GlobalMessage(getUUID(), "no-ex-assign", "00", "禁止将 catch 的第一个参数 error 重新赋值", null, "It is forbidden to reassign the first catched argument error")
        val param204 = GlobalMessage(getUUID(), "react/jsx-no-undef", "00", "禁止使用未定义的 jsx elemet", null, "It is forbidden to use undefined jsx elemet")
        val param205 = GlobalMessage(getUUID(), "react/no-children-prop", "00", "禁止使用 children 做 props", null, "It is forbidden to use children as props")
        val param206 = GlobalMessage(getUUID(), "no-var", "00", "禁止出现 var ", null, "It is forbidden to have var ")
        val param207 = GlobalMessage(getUUID(), "vue/no-duplicate-attributes", "00", "禁止出现重复的属性", null, "It is forbidden to have duplicate properties")
        val param208 = GlobalMessage(getUUID(), "vue/valid-template-root", "00", "template 的根节点必须合法", null, "The root node of template must be legal")
        val param209 = GlobalMessage(getUUID(), "react/no-direct-mutation-state", "00", "禁止直接修改 this.state", null, "It is forbidden to directly modify this.state")
        val param210 = GlobalMessage(getUUID(), "no-delete-var", "00", "禁止使用 delete", null, "It is forbidden to use delete")
        val param211 = GlobalMessage(getUUID(), "react/default-props-match-prop-types", "00", "一个 defaultProps 必须有对应的 propTypes", null, "A defaultProps must have a corresponding propTypes")
        val param212 = GlobalMessage(getUUID(), "space-in-parens", "00", "小括号内的首尾禁止有空格", null, "It is forbidden to type spaces at the beginning and end in the parentheses")
        val param213 = GlobalMessage(getUUID(), "vue/no-dupe-keys", "00", "禁止重复的二级键名", null, "Duplicate secondary key names are forbidden")
        val param214 = GlobalMessage(getUUID(), "react/void-dom-elements-no-children", "00", "HTML 中的自闭和标签禁止有 children", null, "It is forbidden to have children in self-closing tags in HTML")
        val param215 = GlobalMessage(getUUID(), "no-fallthrough", "00", "switch 的 case 内必须有 break, return 或 throw", null, "There must be a break, return or throw in the case of switch")
        val param216 = GlobalMessage(getUUID(), "react/jsx-no-literals", "00", "禁止在 jsx 中出现字符串", null, "It is forbidden to have strings in jsx")
        val param217 = GlobalMessage(getUUID(), "no-empty-character-class", "00", "禁止在正则表达式中使用空的字符集", null, "It is forbidden to use an empty characters set in regular expressions")
        val param218 = GlobalMessage(getUUID(), "no-multi-spaces", "00", "禁止出现连续的多个空格，除非是注释前，或对齐对象的属性、变量定义、import 等", null, "It is forbidden to have multiple consecutive spaces, unless they are before the comment, or the properties of object, variable definition, import, etc. are aligned")
        val param219 = GlobalMessage(getUUID(), "prefer-destructuring", "00", "必须使用解构", null, "Deconstruction must be used")
        val param220 = GlobalMessage(getUUID(), "react/jsx-no-bind", "00", "jsx 中禁止使用 bind", null, "It is forbidden to use bind in jsx")
        val param221 = GlobalMessage(getUUID(), "vue/valid-v-text", "00", "v-text 指令必须合法", null, "The v-text instruction must be legal")
        val param222 = GlobalMessage(getUUID(), "react/jsx-indent-props", "00", "jsx 的 props 缩进必须为四个空格", null, "The props indentation of jsx must be four spaces")
        val param223 = GlobalMessage(getUUID(), "class-methods-use-this", "00", "在类的非静态方法中，必须存在对 this 的引用", null, "There must be a reference to this in a non-static method of class")
        val param224 = GlobalMessage(getUUID(), "capitalized-comments", "00", "注释的首字母必须大写", null, "The first letter of the comment must be capitalized")
        val param225 = GlobalMessage(getUUID(), "no-class-assign", "00", "禁止对定义过的 class 重新赋值", null, "It is forbidden to reassign a defined class")
        val param226 = GlobalMessage(getUUID(), "react/jsx-key", "00", "数组中的 jsx 必须有 key", null, "The jsx in the array must have a key")
        val param227 = GlobalMessage(getUUID(), "vue/valid-v-cloak", "00", "v-cloak 指令必须合法", null, "The v-cloak instruction must be legal.")
        val param228 = GlobalMessage(getUUID(), "multiline-ternary", "00", "三元表达式必须得换行", null, "Linefeed is a must for the ternary expression")
        val param229 = GlobalMessage(getUUID(), "guard-for-in", "00", "for in 内部必须有 hasOwnProperty", null, "There must be hasOwnProperty inside for in")
        val param230 = GlobalMessage(getUUID(), "vue/valid-v-model", "00", "v-model 指令必须合法", null, "The v-model instruction must be legal")
        val param231 = GlobalMessage(getUUID(), "valid-typeof", "00", "typeof 表达式比较的对象必须是 'undefined', 'object', 'boolean', 'number', 'string', 'function' 或 'symbol'", null, "The objects compared by typeof expression must be 'undefined', 'object', 'boolean', 'number', 'string', 'function' or 'symbol'")
        val param232 = GlobalMessage(getUUID(), "vue/require-prop-types", "00", "prop 必须有类型限制", null, "prop must have a type limit")
        val param233 = GlobalMessage(getUUID(), "new-cap", "00", "new 后面的类名必须首字母大写", null, "The class name after new must be capitalized")
        val param234 = GlobalMessage(getUUID(), "react/jsx-equals-spacing", "00", "props 与 value 之间的等号前后禁止有空格", null, "It is forbidden to type spaces before and after the equal sign between props and value")
        val param235 = GlobalMessage(getUUID(), "react/jsx-first-prop-new-line", "00", "第一个 prop 必须得换行", null, "The first prop must have linefeed")
        val param236 = GlobalMessage(getUUID(), "no-multi-assign", "00", "禁止连续赋值，比如 a = b = c = 5", null, "Continuous assignment is forbidden, such as a = b = c = 5")
        val param237 = GlobalMessage(getUUID(), "no-extend-native", "00", "禁止修改原生对象", null, "It is forbidden to modify the native object")
        val param238 = GlobalMessage(getUUID(), "no-invalid-this", "00", "禁止在类之外的地方使用 this", null, "It is forbidden to use this outside of the class")
        val param239 = GlobalMessage(getUUID(), "no-func-assign", "00", "禁止将一个函数申明重新赋值", null, "It is forbidden to reassign a function declaration")
        val param240 = GlobalMessage(getUUID(), "no-octal-escape", "00", "禁止使用八进制的转义符", null, "It is forbidden to use octal escape characters")
        val param241 = GlobalMessage(getUUID(), "no-duplicate-case", "00", "禁止在 switch 语句中出现重复测试表达式的 case", null, "It is forbidden to have case of repeated test expressions in a switch statement")
        val param242 = GlobalMessage(getUUID(), "no-inner-declarations", "00", "禁止在 if 内出现函数申明或使用 var 定义变量", null, "It is forbidden to have function declaration or use var to define variables in if")
        val param243 = GlobalMessage(getUUID(), "no-unused-expressions", "00", "禁止无用的表达式", null, "Useless expressions are forbidden")
        val param244 = GlobalMessage(getUUID(), "vue/no-confusing-v-for-v-if", "00", "禁止出现难以理解的 v-if 和 v-for", null, "It is forbidden to have v-if and v-for that are difficult to understand")
        val param245 = GlobalMessage(getUUID(), "yoda", "00", "必须使用 if (foo === 5) 而不是 if (5 === foo)", null, "if (foo === 5) must be used instead of if (5 === foo)")
        val param246 = GlobalMessage(getUUID(), "semi", "00", "结尾必须有分号", null, "There must be a semicolon at the end")
        val param247 = GlobalMessage(getUUID(), "no-extra-semi", "00", "禁止出现多余的分号", null, "It is forbidden to have excessive semicolons")
        val param248 = GlobalMessage(getUUID(), "react/jsx-handler-names", "00", "handler 的名称必须是 onXXX 或 handleXXX", null, "The name of handler must be onXXX or handleXXX")
        val param249 = GlobalMessage(getUUID(), "rest-spread-spacing", "00", "... 的后面禁止有空格", null, " It is forbidden to have space after ...")
        val param250 = GlobalMessage(getUUID(), "react/no-unused-state", "00", "定义过的 state 必须使用", null, "The defined state must be used")
        val param251 = GlobalMessage(getUUID(), "no-dupe-keys", "00", "禁止在对象字面量中出现重复名称的键名", null, " It is forbidden to have duplicated key names in object literals")
        val param252 = GlobalMessage(getUUID(), "no-floating-decimal", "00", "表示小数时，禁止省略 0，比如 .5", null, "When a decimal is indicated, it is forbidden to omit 0, such as .5")
        val param253 = GlobalMessage(getUUID(), "no-duplicate-imports", "00", "禁止重复 import 模块", null, " It is forbidden to repeat the import module")
        val param254 = GlobalMessage(getUUID(), "no-path-concat", "00", "禁止对 __dirname 或 __filename 使用字符串连接", null, " It is forbidden to use string concatenation for __dirname or __filename")
        val param255 = GlobalMessage(getUUID(), "key-spacing", "00", "对象字面量中冒号前面禁止有空格，后面必须有空格", null, " It is forbidden to have a space before a colon in the object literal, but there must be a space after it")
        val param256 = GlobalMessage(getUUID(), "react/self-closing-comp", "00", "组件内没有 children 时，必须使用自闭和写法", null, "When there are no children in the component, self-closing must be used")
        val param257 = GlobalMessage(getUUID(), "padding-line-between-statements", "00", "限制语句之间的空行规则，比如变量定义完之后必须要空行", null, "Limit empty line rules between statements, e.g a space muse typed after variables are defined")
        val param258 = GlobalMessage(getUUID(), "no-debugger", "00", "禁止使用 debugger", null, "It is forbidden to use debugger")
        val param259 = GlobalMessage(getUUID(), "vue/require-v-for-key", "00", "v-for 指令的元素必须有 v-bind:key", null, "The elements of v-for instruction must have v-bind:key")
        val param260 = GlobalMessage(getUUID(), "array-callback-return", "00", "数组的一些方法（map, reduce 等）的回调函数中，必须有返回值", null, "There must be a return value in the callback function of some methods of array (map, reduce, etc,)")
        val param261 = GlobalMessage(getUUID(), "max-depth", "00", "代码块嵌套的深度禁止超过 5 层", null, "The nesting depth of code block is forbidden to exceed 5 layers")
        val param262 = GlobalMessage(getUUID(), "arrow-spacing", "00", "箭头函数的箭头前后必须有空格", null, "There must be spaces before and after the arrow of arrow function")
        val param263 = GlobalMessage(getUUID(), "react/require-default-props", "00", "非 required 的 prop 必须有 defaultProps", null, "Non-required prop must have defaultProps")
        val param264 = GlobalMessage(getUUID(), "func-call-spacing", "00", "函数名和执行它的括号之间禁止有空格", null, "It is forbidden to have space between the function name and the parentheses that execute it")
        val param265 = GlobalMessage(getUUID(), "brace-style", "00", "if 与 else 的大括号风格必须一致", null, "The braces of if and else must have the same style")
        val param266 = GlobalMessage(getUUID(), "no-empty-pattern", "00", "禁止解构中出现空 {} 或 []", null, "It is forbidden to have empty {} or [] in deconstruction")
        val param267 = GlobalMessage(getUUID(), "space-before-function-paren", "00", "命名函数表达式括号前禁止有空格，箭头函数表达式括号前面要有一个空格", null, "It is forbidden to have spaces in front of the bracket of named function expressions, but there should be a space in front of the bracket of arrow function expression")
        val param268 = GlobalMessage(getUUID(), "object-property-newline", "00", "对象字面量内的属性每行必须只有一个", null, "The attribute within the object literal must have only one per line")
        val param269 = GlobalMessage(getUUID(), "arrow-parens", "00", "箭头函数只有一个参数的时候，必须加括号", null, "When the arrow function has only one parameter, it must be bracketed")
        val param270 = GlobalMessage(getUUID(), "no-sparse-arrays", "00", "禁止在数组中出现连续的逗号，如 let foo = [,,]", null, "It is forbidden to have consecutive commas in the array, such as let foo = [,,]")
        val param271 = GlobalMessage(getUUID(), "func-id-match", "00", "函数名字符数不超过35个", null, "The function name shall have no more than 35 characters")
        val param272 = GlobalMessage(getUUID(), "getter-return", "00", "getter 必须有返回值，并且禁止返回空，比如 return;", null, "The getter must have a return value and it is forbidden to return null, such as return;")
        val param273 = GlobalMessage(getUUID(), "react/jsx-no-duplicate-props", "00", "禁止出现重复的 props", null, "It is forbidden to have repeated props")
        val param274 = GlobalMessage(getUUID(), "no-div-regex", "00", "禁止在正则表达式中出现没必要的转义符", null, "It is forbidden to have unnecessary escape characters in regular expressions")
        val param275 = GlobalMessage(getUUID(), "object-curly-newline", "00", "大括号内的首尾必须有换行", null, "There must be a new line at the beginning and end of the braces")
        val param276 = GlobalMessage(getUUID(), "no-new-symbol", "00", "禁止使用 new 来生成 Symbol", null, "It is forbidden to use new to generate a Symbol")
        val param277 = GlobalMessage(getUUID(), "no-await-in-loop", "00", "禁止将 await 写在循环里，因为这样就无法同时发送多个异步请求了", null, "It is forbidden to write await in a loop, because it is impossible to send multiple asynchronous requests at the same time")
        val param278 = GlobalMessage(getUUID(), "no-empty", "00", "禁止出现空代码块", null, "It is forbidden to have empty code blocks")
        val param279 = GlobalMessage(getUUID(), "no-useless-call", "00", "禁止出现没必要的 call 或 apply", null, "It is forbidden to have unnecessary call or apply")
        val param280 = GlobalMessage(getUUID(), "no-undef-init", "00", "禁止将 undefined 赋值给变量", null, "It is forbidden to assign undefined to variable")
        val param281 = GlobalMessage(getUUID(), "use-isnan", "00", "必须使用 isNaN(foo) 而不是 foo === NaN", null, "isNaN(foo) must be used instead of foo === NaN")
        val param282 = GlobalMessage(getUUID(), "prefer-const", "00", "申明后不再被修改的变量必须使用 const 来申明", null, "Variables that are no longer modified after declaration must be declared using const")
        val param283 = GlobalMessage(getUUID(), "padded-blocks", "00", "代码块首尾必须要空行", null, "There must be blank lines at the beginning and end of code block")
        val param284 = GlobalMessage(getUUID(), "sort-vars", "00", "变量申明必须排好序", null, "Variable declarations must be sorted")
        val param285 = GlobalMessage(getUUID(), "react/jsx-filename-extension", "00", "限制文件后缀", null, "Limit file suffixes")
        val param286 = GlobalMessage(getUUID(), "no-unmodified-loop-condition", "00", "循环内必须对循环条件的变量有修改", null, "There must be modifications to the variables of the loop condition within the loop")
        val param287 = GlobalMessage(getUUID(), "no-multiple-empty-lines", "00", "禁止出现超过三行的连续空行", null, "It is forbidden to have more than three consecutive blank lines")
        val param288 = GlobalMessage(getUUID(), "no-restricted-imports", "00", "禁止 import 指定的模块", null, "The module specified by import is forbidden")
        val param289 = GlobalMessage(getUUID(), "no-process-exit", "00", "禁止使用 process.exit(0)", null, "It is forbidden to use process.exit(0)")
        val param290 = GlobalMessage(getUUID(), "comment-ratio", "00", "注释行数在文件总行数占比不少于10%", null, "The number of comment lines accounts for no less than 10% of the total number of lines in the file")
        val param291 = GlobalMessage(getUUID(), "valid-jsdoc", "00", "注释必须符合 jsdoc 的规范", null, "Comments must conform to the specification of jsdoc")
        val param292 = GlobalMessage(getUUID(), "react/no-did-update-set-state", "00", "禁止在 componentDidUpdate 里面使用 setState", null, "It is forbidden to use setState in componentDidUpdate")
        val param293 = GlobalMessage(getUUID(), "arrow-body-style", "00", "箭头函数能够省略 return 的时候，必须省略，比如必须写成 () => 0，禁止写成 () => { return 0 }", null, "When the arrow function can omit return, it must be omitted. For example, it must be written as () => 0, but it is forbidden to write () => { return 0 }")
        val param294 = GlobalMessage(getUUID(), "require-yield", "00", "generator 函数内必须有 yield", null, "There must be a yield inside the generator function")
        val param295 = GlobalMessage(getUUID(), "space-indent", "00", "采用指定个数空格缩进（默认为4个）且禁止使用tab键", null, "Indent with specified number of spaces (the default is 4) and it is forbidden to use tab")
        val param296 = GlobalMessage(getUUID(), "operator-assignment", "00", "必须使用 x = x + y 而不是 x += y", null, "x = x + y must be used instead of x += y")
        val param297 = GlobalMessage(getUUID(), "no-new-func", "00", "禁止使用 new Function，比如 let x = new Function(\"a\", \"b\", \"return a + b\");", null, "It is forbidden to use new Function, such as let x = new Function(“a”, “b”, “return a + b”);")
        val param298 = GlobalMessage(getUUID(), "no-else-return", "00", "禁止在 else 内使用 return，必须改为提前结束", null, "It is forbidden to use return in else, it must be changed to early termination")
        val param299 = GlobalMessage(getUUID(), "vue/html-end-tags", "00", "html 的结束标签必须符合规定", null, "The closing tag of html must meet the requirements")
        val param300 = GlobalMessage(getUUID(), "no-restricted-syntax", "00", "禁止使用特定的语法", null, "It is forbidden to use specific syntax")
        val param301 = GlobalMessage(getUUID(), "no-new-wrappers", "00", "禁止使用 new 来生成 String, Number 或 Boolean", null, "It is forbidden to use new to generate a String, Number or Boolean")
        val param302 = GlobalMessage(getUUID(), "vue/no-side-effects-in-computed-properties", "00", "禁止在计算属性中对属性修改", null, "It is forbidden to modify the properties in computed properties")
        val param303 = GlobalMessage(getUUID(), "wrap-iife", "00", "立即执行的函数必须符合如下格式 (function () { alert('Hello') })()", null, "Functions that are executed immediately must conform to the format (function () { alert('Hello') })()")
        val param304 = GlobalMessage(getUUID(), "no-warning-comments", "00", "禁止注释中出现 TODO 和 FIXME", null, " It is forbidden to have TODO and FIXME in the comment")
        val param305 = GlobalMessage(getUUID(), "prefer-arrow-callback", "00", "必须使用箭头函数作为回调", null, "Arrow function must be used as callback")
        val param306 = GlobalMessage(getUUID(), "no-caller", "00", "禁止使用 caller 或 callee", null, " It is forbidden to use caller or callee")
        val param307 = GlobalMessage(getUUID(), "id-blacklist", "00", "禁止使用指定的标识符", null, " It is forbidden to use specified identifier")
        val param308 = GlobalMessage(getUUID(), "vue/valid-v-html", "00", "v-html 指令必须合法", null, "The v-html instruction must be legal")
        val param309 = GlobalMessage(getUUID(), "no-useless-computed-key", "00", "禁止出现没必要的计算键名，比如 let a = { ['0']: 0 };", null, "It is forbidden to have unnecessary calculation key names, such as let a = { ['0']: 0 };")
        val param310 = GlobalMessage(getUUID(), "react/boolean-prop-naming", "00", "布尔值类型的 propTypes 的 name 必须为 is 或 has 开头", null, "The name of the propTypes of Boolean type must start with is or has")
        val param311 = GlobalMessage(getUUID(), "no-case-declarations", "00", "switch 的 case 内有变量定义的时候，必须使用大括号将 case 内变成一个代码块", null, "When there is a variable definition in the case of switch, a brace must be used to turn the case into a code block")
        val param312 = GlobalMessage(getUUID(), "no-magic-numbers", "00", "禁止使用 magic numbers", null, " It is forbidden to use magic numbers")
        val param313 = GlobalMessage(getUUID(), "vue/require-component-is", "00", "<component> 必须有 v-bind:is", null, "<component> must have v-bind:is")
        val param314 = GlobalMessage(getUUID(), "no-redeclare", "00", "禁止重复定义变量", null, "It is forbidden to repeatedly define variables")
        val param315 = GlobalMessage(getUUID(), "no-dupe-class-members", "00", "禁止重复定义类", null, "It is forbidden to repeatedly define class")
        val param316 = GlobalMessage(getUUID(), "react/forbid-elements", "00", "禁止使用一些指定的 elements", null, "It is forbidden to use some specified elements.")
        val param317 = GlobalMessage(getUUID(), "no-negated-condition", "00", "否定的表达式可以把逻辑表达的更清楚", null, "Negative expressions can make the logic more clear")
        val param318 = GlobalMessage(getUUID(), "curly", "00", "if 后面必须要有 {，除非是单行 if", null, "There must be { after the if, unless it is a single line if")
        val param319 = GlobalMessage(getUUID(), "eqeqeq", "00", "必须使用 === 或 !==，禁止使用 == 或 !=，与 null 比较时除外", null, " === or !== must be used and it is forbidden to use == or !=, except when compared to null")
        val param320 = GlobalMessage(getUUID(), "no-return-assign", "00", "禁止在 return 语句里赋值", null, "It is forbidden to assign values in the return statement")
        val param321 = GlobalMessage(getUUID(), "react/style-prop-object", "00", "style 属性的取值必须是 object", null, "The value of the style property must be object")
        val param322 = GlobalMessage(getUUID(), "no-eq-null", "00", "禁止使用 foo == null 或 foo != null，必须使用 foo === null 或 foo !== null", null, "It is forbidden to use foo == null or foo != null, but foo === null or foo !== null must be used")
        val param323 = GlobalMessage(getUUID(), "react/jsx-no-comment-textnodes", "00", "禁止在 jsx 中使用像注释的字符串", null, "It is forbidden to use strings like comments in jsx")
        val param324 = GlobalMessage(getUUID(), "no-buffer-constructor", "00", "禁止直接使用 Buffer", null, "It is forbidden to directly use Buffer")
        val param325 = GlobalMessage(getUUID(), "no-this-before-super", "00", "禁止在 super 被调用之前使用 this 或 super", null, "It is forbidden to use this or super before super is called")
        val param326 = GlobalMessage(getUUID(), "camelcase", "00", "变量名必须是 camelcase 风格的", null, "The variable name must be camelcase style")
        val param327 = GlobalMessage(getUUID(), "react/no-multi-comp", "00", "禁止在一个文件创建两个组件", null, "It is forbidden to create two components in one file")
        val param328 = GlobalMessage(getUUID(), "no-restricted-globals", "00", "禁止使用指定的全局变量", null, "It is forbidden to use specified global variables")
        val param329 = GlobalMessage(getUUID(), "no-plusplus", "00", "禁止使用 ++ 或 --", null, "It is forbidden to use ++ or --")
        val param330 = GlobalMessage(getUUID(), "no-extra-bind", "00", "禁止出现没必要的 bind", null, "It is forbidden to have an unnecessary bind")
        val param331 = GlobalMessage(getUUID(), "react/react-in-jsx-scope", "00", "出现 jsx 的地方必须 import React", null, "import React is a must for the place where jsx appears")
        val param332 = GlobalMessage(getUUID(), "space-infix-ops", "00", "操作符左右必须有空格，比如 let sum = 1 + 2;", null, "There must be spaces at the left and right side of operator, such as let sum = 1 + 2;")
        val param333 = GlobalMessage(getUUID(), "vue/no-multi-spaces", "00", "禁止出现连续空格", null, "It is forbidden to have consecutive spaces")
        val param334 = GlobalMessage(getUUID(), "no-process-env", "00", "禁止使用 process.env.NODE_ENV", null, "It is forbidden to use process.env.NODE_ENV")
        val param335 = GlobalMessage(getUUID(), "vue/v-on-style", "00", "限制 v-on 的风格", null, "Limit the style of v-on")
        val param336 = GlobalMessage(getUUID(), "react/no-deprecated", "00", "禁止使用已废弃的 api", null, "It is forbidden to use abandoned apis")
        val param337 = GlobalMessage(getUUID(), "require-await", "00", "async 函数中必须存在 await 语句", null, "The await statement must exist in the async function")
        val param338 = GlobalMessage(getUUID(), "array-element-newline", "00", "配置数组的元素之间的换行格式", null, "Configuring the linefeed format between elements of an array")
        val param339 = GlobalMessage(getUUID(), "react/no-will-update-set-state", "00", "禁止在 componentWillUpdate 中使用 setState", null, "   It is forbidden to use setState in componentWillUpdate")
        val param340 = GlobalMessage(getUUID(), "vue/order-in-components", "00", "组件的属性必须为一定的顺序", null, "The properties of the components must be in a certain order")
        val param341 = GlobalMessage(getUUID(), "id-match", "00", "限制变量名必须匹配指定的正则表达式", null, "The limit variable name must match the specified regular expression")
        val param342 = GlobalMessage(getUUID(), "react/require-optimization", "00", "组件必须有 shouldComponentUpdate", null, "The component must have shouldComponentUpdate")
        val param343 = GlobalMessage(getUUID(), "no-unexpected-multiline", "00", "禁止出现难以理解的多行表达式", null, "It is forbidden to have hard-to-understand multi-line expressions")
        val param344 = GlobalMessage(getUUID(), "no-extra-boolean-cast", "00", "禁止不必要的布尔转换", null, "It is forbidden to have unnecessary Boolean conversions")
        val param345 = GlobalMessage(getUUID(), "func-names", "00", "函数必须有名字", null, "The function must have a name")
        val param346 = GlobalMessage(getUUID(), "react/forbid-prop-types", "00", "禁止使用一些指定的 propTypes", null, "It is forbidden to use some specified propTypes")
        val param347 = GlobalMessage(getUUID(), "vue/valid-v-else", "00", "v-else 指令必须合法", null, "The v-else instruction must be legal")
        val param348 = GlobalMessage(getUUID(), "vue/valid-v-on", "00", "v-on 指令必须合法", null, "The v-on instruction must be legal")
        val param349 = GlobalMessage(getUUID(), "no-catch-shadow", "00", "禁止 catch 的参数名与定义过的变量重复", null, "The parameter name of the catch is forbidden from being repeated with the defined variable")
        val param350 = GlobalMessage(getUUID(), "react/display-name", "00", "组件必须有 displayName 属性", null, "The component must have a displayName property")
        val param351 = GlobalMessage(getUUID(), "no-constant-condition", "00", "测试", null, "Test")
        val param352 = GlobalMessage(getUUID(), "runtime/threadsafe_fn", "00", "1. 检查是否使用了在POSIX标准中非线程安全的函数，如果使用了，给出警告。", null, "1. Check if a non-thread-safe function in the POSIX standard is used, and if so, give a warning. ")
        val param353 = GlobalMessage(getUUID(), "runtime/member_string_references", "00", "1. 检查是否定义了const xxx&amp;这种类型的类数据成员。如果存在，给出警告，建议使用指针代替之。", null, "1. Check if a class data member of type of const xxx&amp; is defined. If it does, give a warning and suggest using a pointer instead. ")
        val param355 = GlobalMessage(getUUID(), "build/header_guard", "00", "1.检查两点: (1)头文件中是否含有#ifndef、#define，(2)#ifndef和#define中的内容是否一样。如果上述两条有一条满足，给出警告。<br> 2. 检查#ifndef的格式是否正确，正确的命名格式是：PATH_FILE_H_。如果格式有出入，给出警告。<br> 3. 检查#endif的格式是否正确，正确的格式是#endif // PATH_FILE_H_。如果不满足这种格式，给出警告。<br> 4. 对上一点3的补充检查：检查#endif后面的注释，如果没有/* */或者//...格式注释，给出警告。", null, "1. Check two points: (1) if the header file contains #ifndef, #define, (2)if the content in #ifndef and #define is the same. If one of the above two points is satisfied, give a warning. <br> 2. Check if the format of #ifndef is correct. The correct naming format is: PATH_FILE_H_. If the format is different, give a warning. <br> 3. Check if the format of #endif is correct. The correct format is #endif // PATH_FILE_H_. If it is not in this format, give a warning. <br> 4. Supplementary check on point 3: Check the comment after #endif, if there is no comment in the format of /* */ or //, give a warning. ")
        val param356 = GlobalMessage(getUUID(), "build/endif_comment", "00", "1.检查#endif后面是否跟有注释，如果没有注释，给出警告。", null, "1. Check if #endif is followed by a comment. If there is no comment, give a warning. ")
        val param357 = GlobalMessage(getUUID(), "runtime/printf_format", "00", "1. 检查在使用printf打印语句时，是否使用了“%qd”。如果使用了，给出警告，建议使用“%lld”。<br> 2. 检查在使用printf打印语句时，是否使用了“%1\$d”这种格式。如果使用了，给出警告。", null, "1. Check if \" % qd\" is used when printing statements with printf. If used, give a warning and suggest using \"%lld\". <br> 2. Check if the format \"%1\$d\" is used when printing statements using printf. If used, give a warning. ")
        val param358 = GlobalMessage(getUUID(), "readability/braces", "00", "1. 检查if ... elseif ... elseif这种结构，如果其中有if或者else if使用了大括号({})，则其他的if或者else if没有使用大括号（{}），则给出警告。<br> 2. 检查if或else体中有多条语句时，是否有大括号{}。如果没有，则给出警告。<br> 3. 检查else是否和与之匹配的if有同样的缩进，如果没有，给出警告；同时建议用户，对于嵌套关系比较模糊的情况，使用{}标示。<br> 4. 检查右大括号}后面是否有“;”，如果有，给出警告。PS：对于namespace、class等正确的情况，不会给出警告。<br> 5. 检查if是否在单独一行，如果不在单独一行，给出警告。", null, "1. Check the if ... elseif ... elseif structure. If there is if or else if that uses the braces ({}), while the other if or else if does not use braces ({}), give a warning. <br> 2. Check if there are multiple statements in the if or else body, if there are braces {}. If not, give a warning. <br> 3. Check if the else has the same indentation as the matching if. If not, give a warning; also suggest the user to use a {} to indicate for cases where the nesting relationship is ambiguous. <br> 4. Check if there is a \";\" after the closing brace}, and if so, give a warning. PS: No warning will be given if namespace, class, etc. are correct <br> 5. Check if if is on a separate line. If not, give a warning. ")
        val param360 = GlobalMessage(getUUID(), "runtime/references", "00", "1. 在函数参数中，查找是否使用了非const型的指针。如果发现了这种类型的指针，则给出警告，并建议使用const型或者指针代替之。", null, "1. In the function 1. 检查在使用printf打印语句时，是否使用了“%qd”。如果使用了，给出警告，建议使用“%lld”。<br> 2. 检查在使用printf打印语句时，是否使用了“%1\$d”这种格式。如果使用了，给出警告。argument, check if a non-const type pointer is used. If a pointer of this type is found, give a warning and suggest to use a const type or a pointer instead. ")
        val param361 = GlobalMessage(getUUID(), "whitespace/operators", "00", "1. 检查“=”号两边是否有空格。如果没有，给出警告。<br> 2. 检查“==|!=|&lt;=|&gt;=||”双目运算符两边是否有空格。如果没有，给出警告。<br> 3. 检查“&lt;|&gt;|&lt;&lt;|&gt;&gt;|!|~|--|++”等单目运算符两边是否有空格。如果没有，给出警告。<br> 4. 检查“&amp;&amp;”两边是否有空格。如果没有，给出警告。", null, "1. Check if there are spaces on both sides of the \" = \" sign. If not, give a warning. <br> 2. Check if the \" == | != |& lt;=|&gt;= || \" binocular operator has spaces on both side. If not, give a warning. <br> 3. Check if there are spaces on both side of the monocular operator such as \"& lt;|&gt;|&lt;&lt;|&gt;&gt;|!|~|--|++\". If not, give a warning. <br> 4. Check if there are spaces on both side of \"&amp;&amp;\". If not, give a warning. ")
        val param362 = GlobalMessage(getUUID(), "readability/union_name", "00", "检查联合体名首字母大写/小写开头，默认为大写", null, "Check the first letter of the combo name for starting with uppercase/lowercase. The default is uppercase.")
        val param363 = GlobalMessage(getUUID(), "runtime/printf", "00", "1. 检查使用printf时，潜在的格式化bugs。如果发现，给出警告。<br> 2. 当检查到snprintf函数中存在数字（size）参数时，建议使用sizeof（变量）代替数字。<br> 3. 检查是否使用了sprintf。如果使用了，给出警告，并建议使用snprintf代替之。<br> 4. 检查是否使用了strcpy或strcat。如果使用了，给出警告，并建议使用snprintf代替之。", null, "1. Check for potential formatting bugs when using printf. If found, give a warning. <br> 2. When it is checked that there is a size parameter in the snprintf function, it is suggested to use sizeof instead of a number. <br> 3. Check if sprintf is used. If used, give a warning and suggest using snprintf instead. <br> 4. Check if strcpy or strcat is used. If used, give a warning and suggest using snprintf instead. ")
        val param364 = GlobalMessage(getUUID(), "build/include_what_you_use", "00", "1.检查是否添加必要的标准模版库，如果没有，给出警告。", null, "1. Check if the necessary standard template library is added. If not, give a warning. ")
        val param365 = GlobalMessage(getUUID(), "build/c++14", "00", "1.检查是否使用了未经批准的C++14头文件，如果使用了，给出警告。", null, "1. Check if an unapproved C++14 header file is used. If it is used, give a warning. ")
        val param366 = GlobalMessage(getUUID(), "runtime/init", "00", "1. 检查是否存在使用变量自身初始化自己的情况。如果存在这种情况，给出警告。", null, "1. Check if there is a case where variable initializes itself. If yes, give a warning. ")
        val param368 = GlobalMessage(getUUID(), "readability/check", "00", "1.检查ASSERT和CHECK断言的使用，建议使用ASSERT_EQ和CHECK_EQ等方式，而不是使用ASSERT_TRUE(condition)这种形式。", null, "1. Check the use of ASSERT and CHECK assertions. It is suggested to use ASSERT_EQ and CHECK_EQ instead of ASSERT_TRUE(condition). ")
        val param369 = GlobalMessage(getUUID(), "build/class", "00", "1.检查类声明是否完整，即类声明的结束部位是否含有“}”。如果没有，给出类声明不完整警告。", null, "1. Check if the class declaration is complete, that is, if the end of the class declaration contains \"}\". If not, give a warning that the class declaration is incomplete. ")
        val param370 = GlobalMessage(getUUID(), "readability/inheritance", "00", "1. 在函数的后面如果有关键字final或者overrider，表示该函数不可以为虚函数，如果检查到final或overrider修饰的函数有virtual修饰，会给出警告。", null, "1. If there is a keyword final or overrider after the function, it means that the function can not be a virtual function. If it is found that the function modified by final or overrider has virtual modification, a warning will be given. ")
        val param373 = GlobalMessage(getUUID(), "runtime/string", "00", "1. 检查是否使用了static或global修饰的string常量。如果检测到了，给出警告，并建议使用C风格字符串代替。", null, "1. Check if a string constant modified by static or global is used. If detected, give a warning and suggest using a C-style string instead. ")
        val param374 = GlobalMessage(getUUID(), "readability/fn_size", "00", "1. 建议编写小巧、功能集中的函数，大于50行开始给出警告，具体警告等级和代码行数关系为：50 =&gt; 0, 100 =&gt; 1, 200 =&gt; 2, 400 =&gt; 3, 800 =&gt; 4, 1600 =&gt; 5；测试代码量可以加倍。<br> 2. 检查到函数定义，但是没有找到函数体的时候，给出警告。", null, "1. It is suggested to write a small, function-focused function. Give a warning if it is more than 50 lines, the relation between specific warning level and the number of lines of code is: 50 =&gt; 0, 100 =&gt; 1, 200 =&gt; 2, 400 =&gt; 3, 800 =&gt; 4, 1600 =&gt; 5; the amount of code to be tested can be doubled. <br> 2. When the function definition is found, but the function body is not found, give a warning. ")
        val param375 = GlobalMessage(getUUID(), "whitespace/comments", "00", "1. 检查代码和注释之间的空格数量，建议最少空2格。如果没有，给出警告。<br> 2. 检查在注释内容和注释符号//之间的空格。如果没有空格，给出警告。", null, "1. Check the number of spaces between the code and the comment. It is suggested to have at least 2 spaces. If not, give a warning. <br> 2. Check for space between the comment content and the comment symbol //. If there are no spaces, give a warning. ")
        val param376 = GlobalMessage(getUUID(), "whitespace/ending_newline", "00", "1. 检查文件结尾是否有空白行，如果没有，提示用户添加一行空白行。", null, "1. Check if there is a blank line at the end of the file. If not, prompt the user to add a blank line. ")
        val param377 = GlobalMessage(getUUID(), "build/deprecated", "00", "1. 检查是否使用了“&gt;?”或者“&lt;?”操作符。如果使用了，给出警告，并建议使用max或者min代替之。", null, "1. Check if the \"&gt;?\" or \"&lt;?\" operator is used. If used, give a warning and suggest using max or min instead. ")
        val param378 = GlobalMessage(getUUID(), "readability/multiline_comment", "00", "1. 多行注释，如果没有搜索到注释结束标识符，给出警告。<br> 2. 检测到了多行注释/* */，lint工具可能会对此给出警告，建议使用//代替之。", null, "1. For multi-line comments, if a comment end identifier is not found, give a warning. <br> 2. A multi-line comment /* */ is detected. The lint tool may give a warning about this. It is suggested to use // instead. ")
        val param379 = GlobalMessage(getUUID(), "build/c++tr1", "00", "1.检查是否使用了未经批准的C++TR1头文件，如果使用了，给出警告。", null, "1. Check if an unapproved C++TR1 header file is used. If it is used, give a warning. ")
        val param380 = GlobalMessage(getUUID(), "runtime/rtti", "00", "1. 小心使用dynamic_cast<>,如果需要在类层次中使用，使用static_cast<>进行向上转型.如果虚函数足够使用，不要使用RTTI（新版本cpplint已经删除）", null, "1. Use the dynamic_cast<> with care. If you need to use it in the class hierarchy, use static_cast<> to make an upcasting. If the virtual functions are sufficient for use, don't use RTTI (the new version of cpplint has been removed)")
        val param381 = GlobalMessage(getUUID(), "build/explicit_make_pair", "00", "1.为了C++11的兼容性，省略make_pair中的模板参数，或者直接使用pair，或者直接构造一个pair。如果检测到make_pair中使用了模板参数，给出警告。", null, "1. For C++11 compatibility, omit the template parameters in make_pair, or use pair directly, or construct a pair directly. If a template parameter is detected in make_pair, give a warning. ")
        val param382 = GlobalMessage(getUUID(), "whitespace/empty_if_body", "00", "1. 检查if语块是否有效，是否有else分支，如果没有，给出警告。", null, "1. Check if the if block is valid and if there is an else branch. If not, give a warning. ")
        val param383 = GlobalMessage(getUUID(), "build/include", "00", "1.检查每一个C++源文件是否都有一个对应的.h头文件，如果没有，给出警告。<br> 2. 检查include的头文件是否加上路径，如果没有，给出警告。<br> 3. 检查是否include了多次同一个头文件，如果是，给出警告。<br> 4. 不要include其他包里面的.cc文件。如果include了，给出警告。", null, "1. Check if each C++ source file has a corresponding .h header file. If not, give a warning. <br> 2. Check if a path is added to the header file of include. If not, give a warning. <br> 3. Check if the same header file is included multiple times, and if so, give a warning. <br> 4. Do not include .cc files in other packages. If include, give a warning. ")
        val param384 = GlobalMessage(getUUID(), "runtime/vlog", "00", "1. VLOG()接受数值等级参数，如果使用符号等级参数，请使用LOG()", null, "1. VLOG() accepts numeric level parameters. If symbol level parameters are to be used, use LOG()")
        val param385 = GlobalMessage(getUUID(), "whitespace/newline", "00", "1. 检查一行上是否有多条语句。如果出现，给出警告。<br> 2. 检查else语句的位置，建议和}在一行上。如果不在一行上，给出警告。<br> 3. 检查是否出现“else{”这种语句。如果出现，给出警告，建议将{放到下一行。<br> 4. 检查{是否和do\\while在同一行上。如果出现，给出警告，建议将{放到下一行。<br> 5. 检查在换行的时候，是否使用了回车\r。如果使用了，给出警告，建议使用\n换行。", null, "1. Check if there are multiple statements on one line. If yes, give a warning. <br> 2. Check the position of else statement, which is suggested to be on the same line of }. If not on the same line, give a warning. <br> 3. Check if the statement \"else { \" appears. If it appears, give a warning and suggest placing { on the next line. <br> 4. Check if { is on the same line as do\\while. If yes, give a warning and suggest placing { on the next line. <br> 5. Check if the carriage return \r is used when changing the line. If used, give a warning and suggest using \n for line break. ")
        val param386 = GlobalMessage(getUUID(), "runtime/indentation_namespace", "00", "1. 检查是否在namespace中存在缩进。如果有，给出警告。", null, "1. Check if there is indentation in the namespace. If yes, give a warning. ")
        val param387 = GlobalMessage(getUUID(), "runtime/virtual", "00", "该规则说明正在补充中…", null, "The rule description is being supplemented...")
        val param388 = GlobalMessage(getUUID(), "runtime/arrays", "00", "1. 检查是否使用变量来初始化数组。如果发现了，给出警告，并建议使用编译时常量来初始化数组。", null, "1. Check if variables are used to initialize the array. If found, give a warning and suggest using a compile-time constant to initialize the array. ")
        val param389 = GlobalMessage(getUUID(), "readability/function", "00", "1. 检查函数参数是否是不含参数名称的参数,如果是,给出警告.(如void test(int)会给出警告)", null, "1. Check if the function parameter is a parameter without a parameter name. If yes, give a warning. (For example, void test(int) will give a warning)")
        val param390 = GlobalMessage(getUUID(), "readability/streams", "00", "1. 建议不要使用流（google  C++编程规范条目），新版本cpplint已经不支持对此条目的检查。", null, "1. It is not suggested to use streaming (google C++ programming specification entry), the new version of cpplint does not support checking this entry. ")
        val param391 = GlobalMessage(getUUID(), "build/namespaces_literals", "00", "检查使用usingstd; 建议使用具体命名空间，如：using std: string", null, "Check using usingstd; it is suggested to use a specific namespace, such as: using std: string")
        val param392 = GlobalMessage(getUUID(), "readability/enum_name", "00", "检查枚举名首字母大写/小写开头，默认为大写", null, "Check if the enum name start with uppercase / lowercase letter; the default is uppercase")
        val param393 = GlobalMessage(getUUID(), "legal/copyright", "00", "1. 检查文件中是否包含“Copyright [year]&lt;Copyright Owner&gt;”，如果不包含，给出警告。", null, "1. Check if \" Copyright [year]& lt;Copyright Owner & gt;\" is included in the file; if not, give a warning. ")
        val param394 = GlobalMessage(getUUID(), "readability/nolint", "00", "1. 检查文件中是否有未知的NOLINT错误分类<br> 2. 检查未知的分类GLOBAL_NOLINT；", null, "1. Check the file for unknown NOLINT error classification<br> 2. Check for unknown classification GLOBAL_NOLINT;")
        val param397 = GlobalMessage(getUUID(), "whitespace/empty_conditional_body", "00", "1. 检查是否存在空条件体（对应于if）。如果存在，给出警告，建议使用{}。", null, "1. Check for empty condition bodies (corresponding to if). If it does, give a warning and it is suggested to use {}. ")
        val param399 = GlobalMessage(getUUID(), "readability/alt_tokens", "00", "1. 检查符号（and、bitor、or、xor、compl、bitand、and_eq、or_eq、xor_eq、not和not_eq）的使用，建议使用（&amp;&amp;、/、//、^、~、&amp;、&amp;=、|=、^=、!、!=）代替以上几类符号。", null, "1. Check the use of symbols (and, bitor, or, xor, compl, bitand, and_eq, or_eq, xor_eq, not, and not_eq). It is suggested to use (&amp;&amp;, /, //, ^, ~, &amp; &amp;=, |=, ^=, !, !=) to replace the above symbols. ")
        val param400 = GlobalMessage(getUUID(), "readability/struct_name", "00", "检查结构体名首字母大写/小写开头，默认为大写", null, "Check if the first letter of the structure name is uppercase/lower case. The default is uppercase")
        val param401 = GlobalMessage(getUUID(), "whitespace/semicolon", "00", "1. 在分号“;”之后需要有空格。如果没有，给出警告。<br> 2. 检查使用分号“;”表示空状态的语句。如果检查到了，给出警告，并提示使用{}代替。<br> 3. 检查每行最后一个分号“;”，看其前面是否有空格。如果有空格，给出警告。", null, "1. A space is required after the semicolon \";\". If there is not, give a warning. <br> 2. Check if there are statements that use the semicolon \";\" to indicate an empty state. If it is found, give a warning and prompt the user to use {} instead. <br> 3. Check the last semicolon \";\" in each line to see if there is a space in front of it. If there is a space, give a warning. ")
        val param403 = GlobalMessage(getUUID(), "runtime/invalid_increment", "00", "1. 检查是否有使用（*++、*--）的情况。如果使用了这种情况，给出警告，提示这种情况会同时改变指针的位置。", null, "1. Check if there is any use of (*++, *--). If yes, give a warning and hint that this will change the position of pointer. ")
        val param404 = GlobalMessage(getUUID(), "whitespace/line_length", "00", "1. 检查每一行代码的长度。对于长度超过120个字符的，给出较严重警告；对于长度超过100个字符的，给出较低级别警告。", null, "1. Check the length of each line of code. For that longer than 120 characters, give a serious warning; for that longer than 100 characters, give a lower level warning. ")
        val param405 = GlobalMessage(getUUID(), "build/c++11", "00", "1.检查对C++11标准提到的右值引用的使用，如果检查到使用右值引用，给出警告（未批准的C++特性）。<br> 2. 检查是否使用了默认的lambda捕获，如果使用了，给出警告（未批准的C++特性）。<br> 3. 检查是否include了未批准的C++特性的头文件，如cfenv、condition_variable等。如果检查到了，给出警告。<br> 4. 检查是否使用了std: : alignment_of或std: : aligned_union，如果使用了，给出警告。", null, "1. Check the use of rvalue reference mentioned in the C++11 standard; if it is detected a rvalue reference is used, give a warning (unapproved C++ features). <br> 2. Check if the default lambda is used for capturing; if it is used, give a warning (unapproved C++ feature). <br> 3. Check if the header files of unapproved C++ features, such as cfenv, condition_variable, etc., are included; if yes, give a warning. <br> 4. Check if std: : alignment_of or std: : aligned_union is used; if it is used, give a warning. ")
        val param406 = GlobalMessage(getUUID(), "readability/nul", "00", "1. 检查文件中是否存在'\\0'字符，即NUL字符，如果存在，给出警告。", null, "1. Check if there is a '\\0' character in the file, i.e. NUL character, and if yes, give a warning.")
        val param407 = GlobalMessage(getUUID(), "readability/class_name", "00", "检查类名首字母大写/小写开头，默认为大写", null, "Check if the first letter of class name is uppercase/lowercase. The default is uppercase")
        val param410 = GlobalMessage(getUUID(), "readability/casting", "00", "1.对基本类型，检查是否使用了过时的类型转换，建议使用static_cast()代替(type)这种转换方式。检查的类型有：int,float,double,bool,char,int32,uint32,int64,uint64.<br> 2. 检查是否在类型转换前面使用了取址操作符。如果使用了，给出警告。如以下语句就会收到警告：&down_cast(obj)->member_;', alt_error_msg)", null, "1. For basic types, check if outdated type conversions are used. It is suggested to use static_cast() instead of the (type) conversion method. The types checked are: int, float, double, bool, char, int32, uint32, int64, uint64.<br> 2. Check if an address-of operator is used before the type conversion. If used, give a warning. The following statement will receive a warning: &down_cast(obj)->member_;', alt_error_msg)")
        val param411 = GlobalMessage(getUUID(), "whitespace/parens", "00", "1. 检查函数名和开始的括号(之间是否有空格，如果有空格，给出警告。<br> 2. 检查函数的结束括号)是否和函数名在同一行上。如果不在同一行，在下一行的话给出警告。<br> 3. 检查函数的结束括号）前面是否有空格，如果有空格，给出警告。<br> 4. 检查if\\for\\while\\switch和开始的括号(之间是否有空格，如果没有空格，给出警告。<br> 5. 检查if\\for\\while\\switch后面的括号()之间的空格是否对称。如果不对称。给出警告（如if( foo )这种情况）。<br> 6. 检查if\\for\\while\\switch后面的括号()内侧的空格情况，建议可以有0个或者1个空格。如果不是0个或者1个，给出警告。", null, "1. Check if there is a space between the function name and the start bracket (; if there is a space, give a warning. <br> 2. Check if the end bracket of the function) is on the same line as the function name. If not on the same line but on the next line, give a warning. <br> 3. Check if there is a space in front of the end bracket ) of the function. If there is a space, give a warning. <br> 4. Check if\\for\\while\\switch and the beginning parenthesis (if there is a space between them, if there is no space, give a warning.<br> 5. Check if the spaces between the brackets () after if\\for\\while\\ are symmetrical. If asymmetry, give a warning (such as if( foo )). <br> 6. Check the spaces inside the brakets () after if\\for\\while\\switch; it is suggested to have 0 or 1 space. If it is not 0 or 1, give a warning.")
        val param412 = GlobalMessage(getUUID(), "build/include_order", "00", "1.检查include文件的顺序：本文件相应头文件，C系统文件，C++系统文件，其他库文件。", null, "1. Check the order of include files: header files corresponding to this file, C system files, C++ system files, and other library files. ")
        val param413 = GlobalMessage(getUUID(), "readability/constructors", "00", "1. 检查宏DISALLOW_COPY_AND_ASSIGN和DISALLOW_IMPLICIT_CONSTRUCTORS的作用域是否正确，正确的作用域为private。<br> 2. 检查宏DISALLOW_COPY_AND_ASSIGN和DISALLOW_IMPLICIT_CONSTRUCTORS的位置是否正确，正确的位置是在类的结束位置。", null, "1. Check if the scopes of the macros DISALLOW_COPY_AND_ASSIGN and DISALLOW_IMPLICIT_CONSTRUCTORS are correct; the correct scope is private. <br> 2. Check if the positions of the macros DISALLOW_COPY_AND_ASSIGN and DISALLOW_IMPLICIT_CONSTRUCTORS are correct; the correct position is at the end of the class. ")
        val param414 = GlobalMessage(getUUID(), "runtime/explicit", "00", "1. 检查只有一个参数的构造函数是否使用了explicit关键字。如果没有使用，给出警告。<br> 2. 检查没有参数的构造函数是否使用了explicit关键字。如果使用了，给出警告。 3. 检查有多个参数的构造函数是否使用了explicit关键字。如果使用了，给出警告。", null, "1. Check if the constructor with only one parameter uses explicit keywords. If not, give a warning. <br> 2. Check if the constructor with no parameters uses explicit keywords. If used, give a warning. 3. Check if the constructor with multiple parameters uses explicit keywords. If used, give a warning. ")
        val param415 = GlobalMessage(getUUID(), "runtime/sizeof", "00", "1. 建议使用sizeof(变量)的形式", null, "1. It is suggested to use the form of sizeof (variable).")
        val param416 = GlobalMessage(getUUID(), "whitespace/end_of_line", "00", "1. 检查每一行的末尾是否有空格。如果有空格，给出警告，建议删除这些空格。", null, "1. Check for spaces at the end of each line. If there are spaces, give a warning and it is suggested to delete these spaces. ")
        val param417 = GlobalMessage(getUUID(), "readability/namespace", "00", "1. 检查namespace的结束位置是否有注释，注释格式为namespace“//namespace xxx”。<br> 2. 检查匿名namespace的结束位置注释是否合法，注释格式应该为“//namespace”或者“//anonymous namespace”", null, "1. Check if there is a comment at the end of namespace. The comment format is namespace “//namespace xxx”. <br> 2. Check if the end position of the anonymous namespace is legal. The comment format should be \"//namespace\" or \"//anonymous namespace\"")
        val param418 = GlobalMessage(getUUID(), "readability/multiline_string", "00", "1. 检测到有字符串分多行显示时，“...”这种连接多行字符串的方式在lint中会得到警告，建议使用C++11的raw strings或者concatenation代替。", null, "1. When it is found that the strings are displayed in multiple lines, the method of using \"...\"  to connect multi-line strings will be warned in lint. It is suggested to use C++11 raw strings or concatenation instead. ")
        val param419 = GlobalMessage(getUUID(), "runtime/memset", "00", "1. 检查memset是否有书写错误。如果检查到类似于memset(buf, sizeof(buf), 0)，则是一个潜在的memset bug，给出警告。", null, "1. Check if memset has a writing error. If it is detected similar to memset(buf, sizeof(buf), 0), it is a potential memset bug, and give a warning. ")
        val param420 = GlobalMessage(getUUID(), "build/forward_decl", "00", "1.在作用域内（如namespace作用域），检查是否使用了类似“class AA: : tt;”这种格式的前向声明。如果有这种前向声明，给出警告。", null, "1. Check if a forward declaration in a format like \"class AA : : tt;\"  is used in the scope (such as the namespace scope). If there is such a forward declaration, give a warning. ")
        val param421 = GlobalMessage(getUUID(), "whitespace/braces", "00", "1. 检查“[”符号前面是否有空白。如果有，给出警告。<br> 2. 检查“{”符号前面是否留有空白。如果没有，给出警告。<br> 3. 检查“}else”这种情况的else前面是否留有空白。如果没有，给出警告。<br> 4. 检查“{”是否接在语句最后，即“{”是否直接跟在语句的后面，而不是单独起一行。如果单独占一行，给出警告。", null, "1. Check if there is a blank in front of the \"[\" symbol. If yes, give a warning. <br> 2. Check if there is a blank in front of the \"{ \" symbol. If no, give a warning. <br> 3. Check if there is a blank in front of the else in the case of \" }else\". If no, give a warning. <br> 4. Check if \"{ \" is at the end of the statement, that is, \"{ \" is directly following the statement, not on a separate line. Give a warning if it is on a separate line. ")
        val param423 = GlobalMessage(getUUID(), "readability/todo", "00", "1. 检查TODO注释的格式是否正确，建议格式为“// TODO(my_username): Stuff.”", null, "1. Check if the format of TODO comment is correct. The suggested format is \"// TODO(my_username): Stuff.\"")
        val param426 = GlobalMessage(getUUID(), "build/include_alpha", "00", "1.检查相同目录下头文件是否按字母序升序引用，如果没有，给出警告。", null, "1. Check if the header files in the same directory are referenced in ascending alphabetical order. If not, give a warning. ")
        val param428 = GlobalMessage(getUUID(), "runtime/operator", "00", "1. 检查是否重载了操作符&amp;。如果重载了，鉴于该操作符的危险性，给出警告。", null, "1. Check if the operator &amp; is reloaded. If yes, give a warning in view of the danger of this operator")
        val param429 = GlobalMessage(getUUID(), "build/namespaces", "00", "1. 检查命名空间定义是否有结束标记，如果没有，给出警告。<br> 2. 检查是否使用了using编译指令，如果使用了，给出警告，提示用户使用using声明指令。<br> 3. 检查在.h文件中是否使用了不具名的命名控件，如果使用了，给出警告。", null, "1. Check if the namespace definition has an end tag. If no, give a warning. <br> 2. Check if the using compiler directive is used. If used, give a warning and prompt the user to use the using declaration directive. <br> 3. Check if the unnamed naming control is used in the .h file. If used, give a warning. ")
        val param431 = GlobalMessage(getUUID(), "runtime/int", "00", "1. 检查port前面是否使用了unsigned short修饰。如果不是，给出警告。<br> 2. 检查是否使用了short、long、long long。如果发现使用了这些，给出警告，并建议使用int16、int64等代替之。", null, "1. Check if the unsigned short modifier is used in front of the port. If not, give a warning. <br> 2. Check if short, long, long long are used. If there are found, give a warning and suggest the user to use int16, int64, etc. instead. ")
        val param432 = GlobalMessage(getUUID(), "build/storage_class", "00", "1. 存储类型的关键字（static、extern、typedef、etc）应该放在其他关键词（如const、volatile、void等）前面。如果没有放到前面，给出警告。", null, "1. The storage type keywords (static, extern, typedef, etc) should be placed before other keywords (such as const, volatile, void, etc.). If not, give a warning. ")
        val param433 = GlobalMessage(getUUID(), "whitespace/todo", "00", "1. 检查TODO注释前后的空格数量，如果没有空格，会给出警告；如果多余1个空格，也给出警告。", null, "1. Check the number of spaces before and after the TODO comment. If there is no space, give a warning; if there is more than 1 space, give a warning. ")
        val param434 = GlobalMessage(getUUID(), "whitespace/pos_braces", "00", "检查大括号“{”不单独占一行，如果单独占一行，则告警。", null, "Check if the brace \"{ \" is on a separate line. If it is on a separate line, give an alarm. ")
        val param435 = GlobalMessage(getUUID(), "whitespace/tab", "00", "1. 检查文中是否使用了Tab。如果使用了，给出警告，建议使用空格代替。", null, "1. Check if the Tab is used in the text. If used, give a warning and use a space instead. ")
        val param436 = GlobalMessage(getUUID(), "whitespace/comma", "00", "1. 在逗号“,”之后需要有空格。如果没有，给出警告。", null, "1. A space is required after the comma \", \". If there is no space, give a warning. ")
        val param437 = GlobalMessage(getUUID(), "whitespace/indent", "00", "1. 检查每一行开始的缩进数量是否合法。如果出现奇数个缩进的情况，给出警告。建议使用2个空格缩进。<br> 2. 结束的括号（如)、}）应该和开始的括号对齐。如果不对其，给出警告。<br> 3. 检查public、private、protected、signals和slots的缩进是否合理。建议缩进一个空格，如果不是，给出警告。", null, "1. Check if the amount of indentations at the beginning of each line is legal. If an odd number of indentations is found, give a warning and suggesting using 2 spaces to indent. <br> 2. The closing parenthesis (such as ), }) should be aligned with the opening parenthesis. If not, give a warning. <br> 3. Check if the indentation of public, private, protected, signals, and slots is reasonable. It is suggested to indent a space. If not, give a warning. ")
        val param438 = GlobalMessage(getUUID(), "readability/utf8", "00", "1. 检查文件中是否包含非法的UTF-8字符，如果存在，给出警告。", null, "1. Check if the file contains illegal UTF-8 characters. If yes, give a warning. ")
        val param439 = GlobalMessage(getUUID(), "whitespace/blank_line", "00", "1. 检查代码块开始处是否有空行。如果在代码块开始的时候有空行，给出警告。<br> 2. 检查代码块结束处是否有空行。如果有空行，给出警告。<br> 3. 检查public\\protected\\private后面是否有空行。如果有空行，给出警告。", null, "1. Check if there is a blank line at the beginning of the code block. If there is a blank line at the beginning of the code block, a warning is given. <br> 2. Check if there is a blank line at the end of the code block. If there is a blank line, give a warning. <br> 3. Check if there is a blank line after public\\protected\\private. If there is a blank line, give a warning.")
        val param440 = GlobalMessage(getUUID(), "whitespace/empty_loop_body", "00", "1. 检查是否存在空循环体(对应for、while)。如果存在，给出警告，建议使用{}。", null, "1. Check if there are empty loop bodies (corresponding to for, while). If yes, give a warning, and it is suggested to use {}. ")
        val param441 = GlobalMessage(getUUID(), "whitespace/forcolon", "00", "1. 检查for循环中冒号前后是否有空格，如果没有，给出警告。", null, "1. Check if there are spaces before and after the colon in the for loop. If not, give a warning. ")
        val param442 = GlobalMessage(getUUID(), "build/printf_format", "00", "1. 检查是否使用了未定义的字符转义序列，如\\%,\\[,\\(和\\{。如果检查到这些转义序列，给出警告。", null, "1. Check if undefined character escape sequences are used, such as \\%,\\[,\\ (and \\{. If these escape sequences are found, give a warning.")
        val param444 = GlobalMessage(getUUID(), "runtime/casting", "00", "1. 检查是否在类型转换前面使用了取址操作符。如果使用了，给出警告，因为这种用法有可能获取临时变量的地址。如以下语句就会收到警告：&amp;down_cast&lt;Obj*&gt;(obj)-&gt;member_;', alt_error_msg)", null, "1. Check if an address-of operator is used before the type conversion. If used, give a warning because it is possible to get the address of temporary variables.  A warning will be received in case of the statement: &amp;down_cast&lt;Obj*&gt;(obj)-&gt;member_;', alt_error_msg)")
        val param445 = GlobalMessage(getUUID(), "unconvert/convert", "00", "不必要的转换", null, "Unnecessary conversion")
        val param446 = GlobalMessage(getUUID(), "structcheck/field", "00", "未使用的struct字段", null, "Unused struct field")
        val param447 = GlobalMessage(getUUID(), "staticcheck/unused", "00", "这个变量%s的值不会被使用", null, "The value of this variable %s will not be used")
        val param448 = GlobalMessage(getUUID(), "gosimple/for", "00", "应该使用for range，而不是 for select{}", null, "for range should be used instead of for select{}")
        val param449 = GlobalMessage(getUUID(), "golint/interfacecomment", "00", "接口需要有注释说明", null, "The interface needs to have a comment")
        val param450 = GlobalMessage(getUUID(), "gas/crypto", "00", "7、使用弱的数字随机生成器（使用math/rand，而不是crypto/rand）；<br>", null, "7. use a weak numeric random generator (using math/rand instead of crypto/rand);<br>")
        val param451 = GlobalMessage(getUUID(), "gosimple/sendrecv", "00", "应该使用简单通道的send/receive，而不是select", null, "   The send/receive of simple channel instead of select should be used")
        val param452 = GlobalMessage(getUUID(), "gas/crypto", "00", "6、crypto/des已被列入import的黑名单中；<br>", null, "6. crypto/des has been included in the import blacklist; <br>")
        val param453 = GlobalMessage(getUUID(), "gas/crypto", "00", "5、RSA Key至少应该有%d位(bits)；<br>", null, "5. RSA Key should have at least %d bits (bits); <br>")
        val param454 = GlobalMessage(getUUID(), "gas/crypto", "00", "4、使用了弱的加密方法；<br>", null, "4. weak encryption method is used; <br>")
        val param455 = GlobalMessage(getUUID(), "gas/crypto", "00", "3、crypto/md5已被列入import的黑名单中；<br>", null, "3. crypto/md5 has been included in the import blacklist; <br>")
        val param456 = GlobalMessage(getUUID(), "gas/crypto", "00", "2、硬编码凭证；<br>", null, "2. hard-coded credentials; <br>")
        val param457 = GlobalMessage(getUUID(), "gas/crypto", "00", "1、crypto/rc4已被列入import的黑名单中；<br>", null, "1. crypto/rc4 has been included in the import blacklist; <br>")
        val param458 = GlobalMessage(getUUID(), "staticcheck/append", "00", "append的结果不会被使用，除非在其他的appends里", null, "The results of append will not be used unless in other appends")
        val param459 = GlobalMessage(getUUID(), "goconst/string", "00", "重复使用的字符串应提取为常量：在 %s:%d:%d:%d 也使用了字符串 \" % s\"，文件为： %s", null, "Reused strings should be extracted as constants: the string \\%s\\ is also used in %s:%d:%d:%d, and the file is: %s")
        val param460 = GlobalMessage(getUUID(), "staticcheck/timeticker", "00", "使用time.Tick在某些场景下会泄漏。可以考虑在endless function中使用，或者使用time.NewTicker。", null, "Using time.Tick will leak in some scenarios. Consider using it in the endless function, or using time.NewTicker. ")
        val param461 = GlobalMessage(getUUID(), "golint/args", "00", "context.Context应该是函数的第一个参数", null, "The context.Context should be the first parameter of the function")
        val param462 = GlobalMessage(getUUID(), "golint/fnsize", "00", "检查函数体行数（逻辑代码行+注释行）是否超过设定值，默认80行", null, "Check if the number of function body lines (logical code line + comment line) exceeds the set value, the default is 80 lines")
        val param463 = GlobalMessage(getUUID(), "maligned/size", "00", "%s：结构大小%d 可以为 %d", null, "%s: The structure size %d can be %d")
        val param464 = GlobalMessage(getUUID(), "unused/unused", "00", "%s没有被使用", null, "%s has not been used")
        val param465 = GlobalMessage(getUUID(), "varcheck/unused", "00", "未使用的全局变量", null, "Unused global variables")
        val param466 = GlobalMessage(getUUID(), "gas/tls", "00", "1、PreferServerCipherSuites设为false；<br>2、MinVersion太低；<br>3、InsecureSkipVerify设为true；<br>4、InsecureSkipVerify可能为true；<br>5、MaxVersion太低；<br>6、MinVersion可能太低；<br>7、PreferServerCipherSuites可能为false；<br>8、MaxVersion可能太低；", null, "1. PreferServerCipherSuites is set to false; <br>2. MinVersion is too low; <br>3. InsecureSkipVerify is set to true; <br>4. InsecureSkipVerify may be true; <br>5. MaxVersion is too low; <br>6. MinVersion may be too low; <br>7. PreferServerCipherSuites may be false; <br>8, MaxVersion may be too low;")
        val param467 = GlobalMessage(getUUID(), "gas/httpoxy", "00", "net/http/cgi已被列入import的黑名单中：Go版本低于1.6.3容易受到Httpoxy的攻击：(CVE-2016-5386)；", null, "net/http/cgi have been included in the import blacklist: Go version below 1.6.3 is vulnerable to Httpoxy attacks: (CVE-2016-5386);")
        val param468 = GlobalMessage(getUUID(), "gosimple/copy", "00", "应该使用copy()而不是循环", null, "copy() instead of loop should be used")
        val param469 = GlobalMessage(getUUID(), "golint/string", "00", "错误字符串不应该大写，或者标点符号、新行结尾", null, "The error string should not be capitalized, or punctuation, the end of a new line")
        val param470 = GlobalMessage(getUUID(), "vet/vet", "00", "检测妨碍编译的错误", null, " Detect errors that prevent compilation")
        val param471 = GlobalMessage(getUUID(), "golint/comment", "00", "1、exported类型%s %s应该有它自己的声明；<br>2、exported类型变量应该有注释，或者改成非exported类型；<br>3、exported类型 %s %s应该有注释%s，或者设置为unexported类型；<br>4、exported类型 %s %s应该有注释，或者设置为unexported类型；<br>5、在exported类型 %v的注释，形式应该是%v…；<br>6、在exported类型%s %s的注释，形式应该是%s…；", null, "1. exported type %s %s should have its own declaration; <br>2. exported type variables should have comments, or be changed to non-exported type; <br>3. exported type %s %s should have a comment %s , or be set to unexported type; <br>4. exported type %s %s should have a comment, or be set to unexported type; <br>5. in the comment of the exported type %v, the form should be %v...;<br >6. In the comment of the exported type %s %s, the form should be %s...;")
        val param472 = GlobalMessage(getUUID(), "golint/funcpara", "00", "函数参数的首字母需要小写", null, "The first letter of the function parameter needs to be lowercase")
        val param473 = GlobalMessage(getUUID(), "gas/file", "00", "在tmp目录创建文件没有使用ioutil.Tempfile", null, "ioutil.Tempfile is not used when creating a file in the tmp directory")
        val param474 = GlobalMessage(getUUID(), "staticcheck/resource", "00", "空的临界区（critical section)", null, "Empty critical section（critical section)")
        val param475 = GlobalMessage(getUUID(), "gas/calls", "00", "审查不安全的调用", null, "Review unsafe calls")
        val param476 = GlobalMessage(getUUID(), "staticcheck/explicit", "00", "只有第1个常量是显式类型", null, "Only the first constant is an explicit type")
        val param477 = GlobalMessage(getUUID(), "golint/funccomment", "00", "函数需要有注释说明", null, "The function needs to have a comment")
        val param478 = GlobalMessage(getUUID(), "golint/type", "00", "1、不应该使用基础类型 %s 作为context.WithValue的key；<br>2、var %s 是类型 %v，不要使用unit-specific后缀 %q；", null, "1. The base type %s should not be used as the key for context.WithValue; <br>2. var %s is type %v, and do not use the unit-specific suffix %q;")
        val param479 = GlobalMessage(getUUID(), "staticcheck/break", "00", "break语句没有效果，是希望跳出外部循环吗？", null, "The break statement has no effect. Is it expected to jump out of the outer loop? ")
        val param480 = GlobalMessage(getUUID(), "golint/equivalent", "00", "应该省略range的第二个值；这个循环等效于`for %s %s range ...`", null, "The second value of range should be omitted; this loop is equivalent to `for %s %s range ...`")
        val param481 = GlobalMessage(getUUID(), "gosimple/usageadvice", "00", "1、应该转换 %s(类型为%s）到%s，而不是使用struct结构；<br>2、应该使用 for {} 而不是 for true {}；<br>3、不必要的nil检查；<br>4、应该使用原生字符串 (`...`)及regexp.%s避免转义两次；<br>5、应该使用无条件的 %s.%s 来替换这个if语句；<br>6、当 %s 为true时，%s不可能为nil；<br>7、应该写 %s 而不是 %s；<br>8、应该在新的一行里合并变量的声明和赋值；<br>9、多余的break语句；<br>10、应该使用fmt.Errorf(...)，而不是errors.New(fmt.Sprintf(...))；<br>11、应该省略nil检查；%s执行len()定义为0；<br>12、应该使用%s = append(%s, %s...) 来替换循环；<br>13、使用slice时应该省略第二个index，s[a:len(s)] 等效于 s[a:]；<br>14、应该使用time.Util，而不是t.Sub(time.Now())；<br>15、应该使用%v.String()方法而不是%v；<br>16、if %s != nil { return %s }; return %s' 可以简化为 'return %s'；<br>17、'_ = <-ch' 可以简化为 '<-ch'；<br>18、应该使用 'return <expr>'，而不是 'if <expr> { return <bool> }; return <bool>'；<br>19、应该使用String()，而不是fmt.Sprintf；<br>20、应该使用%v.Bytes()方法而不是%v；<br>21、应该使用 %s%s.%s(%s)；<br>22、应该使用time.Since，而不是time.Now().Sub；<br>23、多余的return语句；<br>24、应该使用make(%s, %s)；<br>25、应该使用%sbytes.Equal(%s)；<br>26、应该使用make(%s)；<br>27、参数已经是字符串，没有必要使用fmt.Sprintf；<br>28、应该使用copy(%s[:%s], %s[%s:])；<br>29、参数的基础类型是字符串，应该使用简单的转换而不是使用fmt.Sprintf；<br>30、应该省略range的值；这个循环等效与 `for range ...`；", null, "1. convert %s (type is %s) to %s instead of using struct structure; <br>2. for {} should be used instead of for true {};<br>3. Unnecessary nil check; <br> 4. The original string (`...`) and regexp.%s should be used to avoid escaping twice; <br>5. You should use the unconditional %s.%s to replace this if statement; >6. When %s is true, %s cannot be nil; <br>7. %s should be written instead of %s; <br>8. The declaration and assignment of variables should be merged in a new line; <br> 9. Redundant break statement; <br>10. fmt.Errorf (...) should be used instead of errors.New (fmt.Sprintf (...)); <br> 11 nil check should be omitted; %s execution len() is defined as 0;<br>12. %s = append(%s, %s...) should be used to replace the loop; <br>13. The second index should be omitted when using slice, and s[a:len(s)] is equivalent to s[a:];<br>14. time.Util should be used instead of t.Sub(time.Now());<br>15. %v.String() method should be used instead of %v;<br>16. if %s != nil { return %s }; return %s' can be simplified to 'return %s';<br>17. '_ = <-ch' can be simplified to '<-ch';<br> 18. 'return <expr>' should be used instead of 'if <expr> { return <bool> }; return <bool>';<br>19. String() should be used instead of fmt.Sprintf;<br >20.  %v.Bytes () method should be used instead of %v; <br> 21. %s%s.%s (% s) should be used; <br> 22. time.Since should be used instead of time .Now().Sub;<br>23. Redundant return statement; <br>24. make (%s, %s) should be used; <br>25. %sbytes.Equal(%s) should be used; <br>26. make(%s) should be used;<br>27. The parameter is already a string, so there is no need to use fmt.Sprintf;<br>28. copy(%s[:%s], %s[ %s:]) should be used;<br>29. The base type of parameter is a string; a simple conversion should be used instead of using fmt.Sprintf;<br>30. The value of range should be omitted; this loop is equivalent to `for range ...`;")
        val param482 = GlobalMessage(getUUID(), "gas/subprocess", "00", "1、子进程启动时传入了部分路径；<br>2、检查子进程的启动；<br>3、子进程启动使用了变量参数；<br>4、子进程启动时传入了变量；", null, "1. Part of pathes are passed in when the child process is started; <br> 2. Check the start of the child process; <br> 3. The child process starts using the variable parameters; <br> 4. Variables are passed in when the child process is started; ")
        val param483 = GlobalMessage(getUUID(), "staticcheck/condition", "00", "这个条件多次出现在if/else if链中", null, "This condition appears multiple times in the if/else if chain")
        val param484 = GlobalMessage(getUUID(), "gosimple/boolcmp", "00", "应该省略bool常量的比较，可以简化为：%s", null, "The comparison of bool constants should be omitted, which can be simplified to: %s")
        val param485 = GlobalMessage(getUUID(), "staticcheck/deprecated", "00", "%s 已经废弃：%s", null, "%s has been abandoned: %s")
        val param486 = GlobalMessage(getUUID(), "gas/escape", "00", "该方法不会自动转义HTML。验证数据格式良好。", null, "This method does not automatically escape HTML. Verify that the data is in good format. ")
        val param487 = GlobalMessage(getUUID(), "vetshadow/shadow", "00", "%q的声明覆盖了%s位置的声明", null, "The declaration of %q overrides the declaration of the %s position")
        val param488 = GlobalMessage(getUUID(), "staticcheck/loop", "00", "1、在range循环中，defers不会运行，除非通道被关闭；<br>2、循环变量没有变化；<br>3、循环条件一直没有变化，或者存在竞争条件；<br>4、外层循环无条件终止；<br>5、这个循环会spin，导致100%CPU的使用；<br>6、无限循环中的defers永远不会运行；<br>7、在for+select循环中不应该存在空的default case，会导致循环spin；", null, "1. In the range loop, defers will not run unless the channel is closed; <br>2. The loop variables have no change; <br>3. The loop conditions have always no change, or there is a race condition; <br>4. Outer loop terminates unconditionally; <br>5. This loop will spin, resulting in 100% CPU usage; <br>6. defers in infinite loop will never run; <br>7. empty default case should not exist in the for+select loop, because it will cause a loop spin;")
        val param489 = GlobalMessage(getUUID(), "gas/network", "00", "绑定了所有网络接口", null, "All network interfaces have been bound")
        val param490 = GlobalMessage(getUUID(), "unparam/unused", "00", "参数%s没有使用", null, "The parameter %s is not used")
        val param491 = GlobalMessage(getUUID(), "golint/ret", "00", "1、当返回多个值时，错误值应该放到最后；<br>2、exported类型 %s %s 返回unexported类型 %s，这会造成使用上的困扰；<br>3、if语块以return语句结束，那么可以删去else分支并将else内的语句移到if语块外；", null, "1. When multiple values are returned, the error values should be put to the end; <br> 2. exported type %s %s returns unexported type %s, which will cause trouble in use; <br>3. if block ends with return statement. Then, it is possible delete the else branch and move the statement inside else to the outside of the if block;")
        val param492 = GlobalMessage(getUUID(), "staticcheck/boolean", "00", "对于boolean类型的双重否定是没有效果的，是笔误吗？", null, "Double negation to the boolean type has no effect. Is it a clerical error? ")
        val param493 = GlobalMessage(getUUID(), "staticcheck/iowriter", "00", "io.Writer.Write 不能修改所提供的buffer缓冲区，即使是临时性的", null, "io.Writer.Write cannot modify the buffer zone provided, even if it is temporary")
        val param494 = GlobalMessage(getUUID(), "safesql/sql", "00", "潜在不安全的SQL语句", null, "Potentially unsafe SQL statements")
        val param495 = GlobalMessage(getUUID(), "golint/funcret", "00", "函数返回参数首字母需要小写", null, "The first letter of function return parameters needs to be lowercase")
        val param496 = GlobalMessage(getUUID(), "gas/sql", "00", "1、使用了SQL字符拼接；<br>2、使用了SQL字符格式化；", null, "1. SQL strings combining is used; <br> 2. SQL character formatting is used;")
        val param497 = GlobalMessage(getUUID(), "golint/noptr", "00", "不建议map、chan类型使用指针类型", null, "It is not suggested to use pointer type for map and chan types.")
        val param498 = GlobalMessage(getUUID(), "golint/structcomment", "00", "结构体需要有注释说明", null, "The structure needs comments")
        val param499 = GlobalMessage(getUUID(), "golint/package", "00", "1、package注释开头不应该有空格；<br>2、空的import应只能在main或test package里面，或者使用注释说明；<br>3、package需要写注释；<br>4、不应该使用 . imports 形式；<br>5、package注释与声明之间不应该有空行；<br>6、package名称不应该有下划线；<br>7、package注释应该是该种形式 %s；", null, "1. package comments should not have spaces at the beginning; <br>2. Empty import should only be in the main or test package, or have comments; <br>3. package needs comments; <br>4. . imports form should not be used; <br>5. There should be no blank lines between package comments and declarations; <br>6. package names should not be underlined; <br>7. package comments should be in the form of %s;")
        val param500 = GlobalMessage(getUUID(), "deadcode/unused", "00", "%s 未使用的函数", null, "%s unused function")
        val param501 = GlobalMessage(getUUID(), "staticcheck/return", "00", "%s 是一个纯函数（pure function），但是它的返回值被忽略了", null, "%s is a pure function, but its return value has been ignored")
        val param502 = GlobalMessage(getUUID(), "goimports/notimport", "00", "文件没有被goimport", null, "The file is not goimported")
        val param503 = GlobalMessage(getUUID(), "staticcheck/routine", "00", "启动goroutine前应该先调用%s，以避免竞争", null, "You should call %s before starting goroutine to avoid competition.")
        val param504 = GlobalMessage(getUUID(), "staticcheck/overrun", "00", "index索引超出界限（bounds）", null, "The index is out of bounds (bounds)")
        val param505 = GlobalMessage(getUUID(), "errcheck/retvalue", "00", "没有检查返回值", null, "return values are not checked")
        val param506 = GlobalMessage(getUUID(), "gas/permission", "00", "1、文件至少需要%#o的权限；<br>2、目录至少需要%#o的权限；", null, "1. The file requires at least %#o permissions; <br>2. The directory requires at least %#o permissions;")
        val param507 = GlobalMessage(getUUID(), "staticcheck/simple", "00", "1、文件模式 %s 计算结果是 %#o；是否应该是 0%s；<br>2、&*x 可以简化为 x。这个用法不会复制x。；", null, "1. The file mode %s computed result is %#o; if it should be 0%s; <br>2. &*x can be simplified to x. This usage does not copy x. ;")
        val param508 = GlobalMessage(getUUID(), "staticcheck/infinite", "00", "调用了无限递归", null, "Infinite recursion is called")
        val param509 = GlobalMessage(getUUID(), "interfacer/interface", "00", "%s 可以为 %s", null, "%s can be %s")
        val param510 = GlobalMessage(getUUID(), "staticcheck/defer", "00", "1、刚好在lock之后defer %s，你是想defer %s吗？；<br>2、defer %s 前，应该先检查返回的错误码；", null, "1. defer %s just after the lock. Do you want to defer %s? ;<br>2. before defer %s, the error code returned should be checked;")
        val param511 = GlobalMessage(getUUID(), "golint/replace", "00", "应该替换%s以%s%s", null, "%s should be replaced with %s%s")
        val param512 = GlobalMessage(getUUID(), "staticcheck/signal", "00", "1、%s 信号不能被捕获；<br>2、%s 不能被捕获（是syscall.SIGTERM吗？）；", null, "1. The %s signal cannot be captured; <br>2. %s cannot be captured (is syscall.SIGTERM?);")
        val param513 = GlobalMessage(getUUID(), "staticcheck/sleep", "00", "睡眠 %d（ns）很可能是一个bug。如果不是的话请明确下:%s", null, "Sleep %d(ns) is probably a bug. If not, please make clear: %s")
        val param514 = GlobalMessage(getUUID(), "golint/convar", "00", "常量需要是全部大写", null, "Constants need to be all uppercase")
        val param515 = GlobalMessage(getUUID(), "ineffassign/assign", "00", "无效赋值：%s", null, "Invalid assignment: %s")
        val param516 = GlobalMessage(getUUID(), "staticcheck/args", "00", "1、参数%s在使用前被覆写了；<br>2、该函数类似print风格的函数，有第一个动态参数，但没有其他更多的参数。应该使用print风格的函数。；<br>3、io.Seeker的第一个参数是偏移值offset，但是使用了io.Seek*常量；", null, "1. The parameter %s has been overwritten before use; <br> 2. The function is similar to the print style function, having the first dynamic parameter, but no other parameters. The print-style function should be used;<br>3. The first parameter of io.Seeker is an offset value, but using the io.Seek* constant;")
        val param517 = GlobalMessage(getUUID(), "gas/error", "00", "没有处理错误", null, "Errors are not handled")
        val param518 = GlobalMessage(getUUID(), "golint/print", "00", "%s(fmt.Sprintf(...)) 不符合要求，应该使用 %s.Errorf(...)替换之", null, " %s(fmt.Sprintf(...)) does not meet the requirements and should be replaced with %s.Errorf(...).")
        val param519 = GlobalMessage(getUUID(), "golint/naming", "00", "1、%s 名称会被其他packages以 %s.%s引用，可以考虑这样调用 %s；<br>2、error变量%s应该以%sFoo形式命名；<br>3、Go名称不应该使用全部大写，请使用驼峰格式；<br>4、Go名称不应该使用k开头；%s %s应该是%s；<br>5、%s %s 应该是 %s；<br>6、receiver名称不应该有下划线，如果是不使用的，可以省略；<br>7、receiver名称应该是其身份的反射；不要使用this或者self；<br>8、receiver名称%s应该与之前的%s %s保持一致；<br>9、Go名称不应该使用下划线；%s %s应该是%s；", null, "1. %s name will be referenced by other packages in %s.%s. Calling %s by this method can be considered; <br>2. error variable %s should be named in %sFoo; <br>3. Go name should not be in full uppercase. Please use the hump format; <br>4. Go name should not start with k; %s %s should be %s; <br>5. %s %s should be %s;<br>6. The receiver name should not be underlined. If unused, it can be omitted. <br>7. The receiver name should be the reflection of its identity; do not use this or self; <br>8. The receiver name %s should be the same as the previous %s %s; <br>9. Go name should not be underlined; %s %s should be %s;")
        val param520 = GlobalMessage(getUUID(), "gas/modulus", "00", "检查模数是否为0", null, "Check if the modulus is 0")
        val param521 = GlobalMessage(getUUID(), "staticcheck/exit", "00", "TestMain应该调用os.Exit来设置退出码", null, "TestMain should call os.Exit to set the exit code")
        val param522 = GlobalMessage(getUUID(), "golint/decl", "00", "1、应该丢弃 = %s，从变量%s声明中；它的值为0；<br>2、应该省略类型%s，从变量%s声明中；它会从右手边推断出来；", null, "1. = %s should be discarded from the variable %s declaration; its value is 0; <br> 2. The type %s should be omitted from the variable %s declaration; it will be inferred from the right hand side;")
        val param523 = GlobalMessage(getUUID(), "misspell/spell", "00", "%s 应拼写为 %s", null, "%s should be spelled as %s")
        val param524 = GlobalMessage(getUUID(), "staticcheck/efficient", "00", "m[string(key)] 可能比 k := string(key); m[k] 更加高效", null, "m[string(key)] may be more efficient than k := string(key); m[k]")
        val param525 = GlobalMessage(getUUID(), "unparam/value", "00", "1、参数始终接收%v；<br>2、参数%s始终是%s；", null, "1. The parameter always receives %v; <br> 2. The parameter %s is always %s;")
        val param526 = GlobalMessage(getUUID(), "staticcheck/emptybranch", "00", "空的分支", null, "Empty branch")
        val param527 = GlobalMessage(getUUID(), "staticcheck/goroutine", "00", "goroutine调用T.%s，作为测试test必须在相同的goroutine里调用", null, "calling T.%s in goroutine, which must be called in the same goroutine as the test")
        val param528 = GlobalMessage(getUUID(), "staticcheck/compare", "00", "1、比较两个不同长度的字符串是否相等，会永远返回false；<br>2、无符号数不可能 < 0；<br>3、无符号数永远 >= 0；<br>4、x %s 0的结果永远等于x；<br>5、没有数值等于NaN，即使Nan本身；<br>6、x & 0的结果永远等于 0；", null, "1. Compare two character strings of different lengths and see if they are equal. It will always return false; <br> 2. Unsigned number cannot be < 0; <br> 3. Unsigned number is always >= 0; <br> 4. The result of  x %s 0 is always equal to x; <br>5. No value is equal to NaN, even if Nan itself; <br>6. The result of x & 0 is always equal to 0;")
        val param529 = GlobalMessage(getUUID(), "staticcheck/exp", "00", "1、%s操作符两边存在相同的表达式；<br>2、二元表达式永远是 %t，对于所有可能的值（%s %s %s）；", null, "1. The %s operator has the same expressions on both sides; <br>2. The binary expression is always %t, for all possible values​(%s %s %s);")
        val param530 = GlobalMessage(getUUID(), "staticcheck/httpheader", "00", "在http.Header里的keys都是规范化的，然而%q并不符合规范；修改该常量或者使用http.CanonicalHeaderKey", null, "The keys in http.Header are normalized, but %q does not conform to the specification; modify the constant or use http.CanonicalHeaderKey")
        val param531 = GlobalMessage(getUUID(), "gofmt/notformat", "00", "文件没有使用gofmt -s格式化", null, "The file is not formatted with gofmt -s")
        val param532 = GlobalMessage(getUUID(), "staticcheck/assign", "00", "1、%s 自赋值为 %s；<br>2、赋值给nil map；<br>3、不应该赋值给%s；", null, "1. %s self-assigned to %s; <br>2. Assign to nil map; <br>3. It should not be assigned to %s;")
        val param533 = GlobalMessage(getUUID(), "staticcheck/exec", "00", "exec.Command的第一个参数看起来是shell命令，但是缺少了程序名或者路径", null, "The first argument to exec.Command looks like a shell command, but the program name or path is missing")
        val param534 = GlobalMessage(getUUID(), "nakedret/ret", "00", "检查named return语句的函数体是否超过一定数值，默认是5行", null, "Check if the function body of the named return statement exceeds a certain value. The default is 5 lines")

        // 新增的规则
        val param535 = GlobalMessage(getUUID(), "max-len", "00", "限制一个文件最多的行数", null, "")

        // 舍去旧的规则
        //val param46 = GlobalMessage(getUUID(), "max-len-tosa", "00", "单行字符数不超过指定数量（默认为120个）", null, "The number of characters per line does not exceed the specified value (the default is 120).")
        //val param430 = GlobalMessage(getUUID(), "readability/utf8-tosa", "00", "文件编码必须是utf8", null, "The file encoding must be utf8")
        //val param535 = GlobalMessage(getUUID(), "onesdk.FinalMemberNaming", "00", "检查final成员命名是否全部大写，否则给出告警", null, "Check if the final member name is all uppercase. If not, give an alarm")
        //val param536 = GlobalMessage(getUUID(), "onesdk.InterfaceNaming", "00", "检查接口名称是否符合Pascal命名规范，且以I开头，否则给出告警", null, "Check if the interface name conforms to the Pascal naming specification and starts with I. If not, give an alarm")
        //val param537 = GlobalMessage(getUUID(), "onesdk.PackageNaming", "00", "检查package名称是否包含com.tencent.gcloud，没有则给出告警", null, "Check if the package name contains com.tencent.gcloud. If not, give an alarm")
        //val param538 = GlobalMessage(getUUID(), "onesdk.MethodParamNaming", "00", "检查函数参数是否符合Camel命名规范，没有则给出告警", null, "Check if the function parameters meet the Camel naming specification.If not, give an alarm")
        //val param539 = GlobalMessage(getUUID(), "EscapeSequence", "00", "对于具有特殊转义序列的任何字符(\\b, \\t, \\n, \\f, \\r, \", '及\\)，我们使用它的转义序列，而不是相应的八进制(比如\\012)或Unicode(比如\\u000a)转义。", null, "For any character with a special escape sequence (\\b, \\t, \\n, \\f, \\r, \\, 'and \\), we use its escape sequence instead of the corresponding Octal (such as \\\\012) or Unicode (such as \\\\u000a) escaping. ")
        //val param540 = GlobalMessage(getUUID(), "onesdk.EnumNaming", "00", "检查枚举名称是否符合Pascal命名规范，枚举值是否全部大写，没有则给出告警", null, "Check if the enum name conforms to the Pascal naming specification, and if the enumerated values are all uppercase. If not, give an alarm")
        //val param541 = GlobalMessage(getUUID(), "onesdk.HeaderComment", "00", "检查文件头是否包含Version、Module、Author字段注释，没有则给出告警", null, "Check if the file header contains the comments of Version, Module, and Author fields. If not, give an alarm")
        //val param542 = GlobalMessage(getUUID(), "onesdk.MethodNaming", "00", "检查函数名称是否符合Camel命名规范，没有则给出告警", null, "Check if the function name conforms to the Camel naming specification. If not, give an alarm")
        //val param543 = GlobalMessage(getUUID(), "onesdk.ClassMemberNaming", "00", "检查函数成员变量是否符合Camel命名规范，且以m开头，没有则给出告警", null, "Check if the function member variable conforms to the Camel naming specification and starts with m. If not, give an alarm")

        return mapOf(
                "for-direction" to objectMapper.writeValueAsString(param1),
                "id-length" to objectMapper.writeValueAsString(param2),
                "vue/max-attributes-per-line" to objectMapper.writeValueAsString(param3),
                "vue/no-reservered-keys" to objectMapper.writeValueAsString(param4),
                "react/no-array-index-key" to objectMapper.writeValueAsString(param5),
                "semi-style" to objectMapper.writeValueAsString(param6),
                "unicode-bom" to objectMapper.writeValueAsString(param7),
                "vue/valid-v-bind" to objectMapper.writeValueAsString(param8),
                "consistent-return" to objectMapper.writeValueAsString(param9),
                "no-const-assign" to objectMapper.writeValueAsString(param10),
                "computed-property-spacing" to objectMapper.writeValueAsString(param11),
                "consistent-this" to objectMapper.writeValueAsString(param12),
                "vue/no-parsing-error" to objectMapper.writeValueAsString(param13),
                "react/no-render-return-value" to objectMapper.writeValueAsString(param14),
                "no-constant-condition" to objectMapper.writeValueAsString(param15),
                "no-confusing-arrow" to objectMapper.writeValueAsString(param16),
                "react/jsx-max-props-per-line" to objectMapper.writeValueAsString(param17),
                "no-continue" to objectMapper.writeValueAsString(param18),
                "vue/jsx-uses-vars" to objectMapper.writeValueAsString(param19),
                "no-param-reassign" to objectMapper.writeValueAsString(param20),
                "callback-return" to objectMapper.writeValueAsString(param21),
                "default-case" to objectMapper.writeValueAsString(param22),
                "linebreak-style" to objectMapper.writeValueAsString(param23),
                "spaced-comment" to objectMapper.writeValueAsString(param24),
                "array-bracket-newline" to objectMapper.writeValueAsString(param25),
                "license" to objectMapper.writeValueAsString(param26),
                "operator-linebreak" to objectMapper.writeValueAsString(param27),
                "array-bracket-spacing" to objectMapper.writeValueAsString(param28),
                "vue/no-shared-component-data" to objectMapper.writeValueAsString(param29),
                "sort-keys" to objectMapper.writeValueAsString(param30),
                "no-script-url" to objectMapper.writeValueAsString(param31),
                "no-undef" to objectMapper.writeValueAsString(param32),
                "comma-spacing" to objectMapper.writeValueAsString(param33),
                "vue/v-bind-style" to objectMapper.writeValueAsString(param34),
                "vue/require-valid-default-prop" to objectMapper.writeValueAsString(param35),
                "no-useless-constructor" to objectMapper.writeValueAsString(param36),
                "prefer-spread" to objectMapper.writeValueAsString(param37),
                "no-labels" to objectMapper.writeValueAsString(param38),
                "no-undefined" to objectMapper.writeValueAsString(param39),
                "no-unused-vars" to objectMapper.writeValueAsString(param40),
                "vue/no-async-in-computed-properties" to objectMapper.writeValueAsString(param41),
                "vue/require-render-return" to objectMapper.writeValueAsString(param42),
                "vue/name-property-casing" to objectMapper.writeValueAsString(param43),
                "no-new-object" to objectMapper.writeValueAsString(param44),
                "no-trailing-spaces" to objectMapper.writeValueAsString(param45),
                //"max-len-tosa" to objectMapper.writeValueAsString(param46),
                "block-spacing" to objectMapper.writeValueAsString(param47),
                "react/no-typos" to objectMapper.writeValueAsString(param48),
                "prefer-numeric-literals" to objectMapper.writeValueAsString(param49),
                "react/jsx-boolean-value" to objectMapper.writeValueAsString(param50),
                "space-before-blocks" to objectMapper.writeValueAsString(param51),
                "func-name-matching" to objectMapper.writeValueAsString(param52),
                "no-sync" to objectMapper.writeValueAsString(param53),
                "no-self-compare" to objectMapper.writeValueAsString(param54),
                "no-void" to objectMapper.writeValueAsString(param55),
                "react/no-did-mount-set-state" to objectMapper.writeValueAsString(param56),
                "react/no-unused-prop-types" to objectMapper.writeValueAsString(param57),
                "react/jsx-closing-bracket-location" to objectMapper.writeValueAsString(param58),
                "dot-location" to objectMapper.writeValueAsString(param59),
                "react/no-unescaped-entities" to objectMapper.writeValueAsString(param60),
                "react/jsx-uses-react" to objectMapper.writeValueAsString(param61),
                "no-useless-return" to objectMapper.writeValueAsString(param62),
                "max-statements-per-line" to objectMapper.writeValueAsString(param63),
                "react/jsx-closing-tag-location" to objectMapper.writeValueAsString(param64),
                "complexity" to objectMapper.writeValueAsString(param65),
                "vue/valid-v-else-if" to objectMapper.writeValueAsString(param66),
                "no-underscore-dangle" to objectMapper.writeValueAsString(param67),
                "no-with" to objectMapper.writeValueAsString(param68),
                "new-parens" to objectMapper.writeValueAsString(param69),
                "no-implicit-coercion" to objectMapper.writeValueAsString(param70),
                "vue/valid-v-if" to objectMapper.writeValueAsString(param71),
                "no-useless-escape" to objectMapper.writeValueAsString(param72),
                "object-shorthand" to objectMapper.writeValueAsString(param73),
                "no-alert" to objectMapper.writeValueAsString(param74),
                "no-obj-calls" to objectMapper.writeValueAsString(param75),
                "no-proto" to objectMapper.writeValueAsString(param76),
                "no-multi-str" to objectMapper.writeValueAsString(param77),
                "no-control-regex" to objectMapper.writeValueAsString(param78),
                "prefer-rest-params" to objectMapper.writeValueAsString(param79),
                "comma-dangle" to objectMapper.writeValueAsString(param80),
                "no-self-assign" to objectMapper.writeValueAsString(param81),
                "lines-around-comment" to objectMapper.writeValueAsString(param82),
                "comma-style" to objectMapper.writeValueAsString(param83),
                "react/no-danger-with-children" to objectMapper.writeValueAsString(param84),
                "one-var-declaration-per-line" to objectMapper.writeValueAsString(param85),
                "react/jsx-tag-spacing" to objectMapper.writeValueAsString(param86),
                "no-octal" to objectMapper.writeValueAsString(param87),
                "react/no-is-mounted" to objectMapper.writeValueAsString(param88),
                "no-mixed-operators" to objectMapper.writeValueAsString(param89),
                "no-eval" to objectMapper.writeValueAsString(param90),
                "no-mixed-spaces-and-tabs" to objectMapper.writeValueAsString(param91),
                "max-nested-callbacks" to objectMapper.writeValueAsString(param92),
                "newline-per-chained-call" to objectMapper.writeValueAsString(param93),
                "no-compare-neg-zero" to objectMapper.writeValueAsString(param94),
                "react/jsx-sort-props" to objectMapper.writeValueAsString(param95),
                "eol-last" to objectMapper.writeValueAsString(param96),
                "react/forbid-foreign-prop-types" to objectMapper.writeValueAsString(param97),
                "no-label-var" to objectMapper.writeValueAsString(param98),
                "react/prefer-es6-class" to objectMapper.writeValueAsString(param99),
                "vue/valid-v-show" to objectMapper.writeValueAsString(param100),
                "quotes" to objectMapper.writeValueAsString(param101),
                "no-unreachable" to objectMapper.writeValueAsString(param102),
                "wrap-regex" to objectMapper.writeValueAsString(param103),
                "no-invalid-regexp" to objectMapper.writeValueAsString(param104),
                "generator-star-spacing" to objectMapper.writeValueAsString(param105),
                "no-bitwise" to objectMapper.writeValueAsString(param106),
                "react/jsx-indent" to objectMapper.writeValueAsString(param107),
                "vars-on-top" to objectMapper.writeValueAsString(param108),
                "vue/no-textarea-mustache" to objectMapper.writeValueAsString(param109),
                "vue/html-quotes" to objectMapper.writeValueAsString(param110),
                "vue/valid-v-once" to objectMapper.writeValueAsString(param111),
                "react/no-set-state" to objectMapper.writeValueAsString(param112),
                "prefer-template" to objectMapper.writeValueAsString(param113),
                "no-tabs" to objectMapper.writeValueAsString(param114),
                "vue/no-template-key" to objectMapper.writeValueAsString(param115),
                "react/jsx-curly-spacing" to objectMapper.writeValueAsString(param116),
                "react/no-redundant-should-component-update" to objectMapper.writeValueAsString(param117),
                "vue/attribute-hyphenation" to objectMapper.writeValueAsString(param118),
                "no-throw-literal" to objectMapper.writeValueAsString(param119),
                "template-tag-spacing" to objectMapper.writeValueAsString(param120),
                "no-unsafe-finally" to objectMapper.writeValueAsString(param121),
                "no-nested-ternary" to objectMapper.writeValueAsString(param122),
                "space-unary-ops" to objectMapper.writeValueAsString(param123),
                "vue/valid-v-for" to objectMapper.writeValueAsString(param124),
                "indent" to objectMapper.writeValueAsString(param125),
                "react/require-render-return" to objectMapper.writeValueAsString(param126),
                "jsx-quotes" to objectMapper.writeValueAsString(param127),
                "no-shadow" to objectMapper.writeValueAsString(param128),
                "react/no-string-refs" to objectMapper.writeValueAsString(param129),
                "no-dupe-args" to objectMapper.writeValueAsString(param130),
                "max-statements" to objectMapper.writeValueAsString(param131),
                "no-restricted-modules" to objectMapper.writeValueAsString(param132),
                "require-jsdoc" to objectMapper.writeValueAsString(param133),
                "one-var" to objectMapper.writeValueAsString(param134),
                "vue/valid-v-pre" to objectMapper.writeValueAsString(param135),
                "keyword-spacing" to objectMapper.writeValueAsString(param136),
                "handle-callback-err" to objectMapper.writeValueAsString(param137),
                "react/no-unknown-property" to objectMapper.writeValueAsString(param138),
                "no-regex-spaces" to objectMapper.writeValueAsString(param139),
                "radix" to objectMapper.writeValueAsString(param140),
                "accessor-pairs" to objectMapper.writeValueAsString(param141),
                "symbol-description" to objectMapper.writeValueAsString(param142),
                "switch-colon-spacing" to objectMapper.writeValueAsString(param143),
                "no-useless-rename" to objectMapper.writeValueAsString(param144),
                "no-iterator" to objectMapper.writeValueAsString(param145),
                "no-restricted-properties" to objectMapper.writeValueAsString(param146),
                "yield-star-spacing" to objectMapper.writeValueAsString(param147),
                "react/jsx-pascal-case" to objectMapper.writeValueAsString(param148),
                "func-style" to objectMapper.writeValueAsString(param149),
                "no-lonely-if" to objectMapper.writeValueAsString(param150),
                "no-unneeded-ternary" to objectMapper.writeValueAsString(param151),
                "no-ternary" to objectMapper.writeValueAsString(param152),
                "no-extra-label" to objectMapper.writeValueAsString(param153),
                "react/no-danger" to objectMapper.writeValueAsString(param154),
                "no-implied-eval" to objectMapper.writeValueAsString(param155),
                "no-array-constructor" to objectMapper.writeValueAsString(param156),
                "no-return-await" to objectMapper.writeValueAsString(param157),
                "no-unused-labels" to objectMapper.writeValueAsString(param158),
                "no-new-require" to objectMapper.writeValueAsString(param159),
                "vue/return-in-computed-property" to objectMapper.writeValueAsString(param160),
                "no-implicit-globals" to objectMapper.writeValueAsString(param161),
                "no-lone-blocks" to objectMapper.writeValueAsString(param162),
                "no-template-curly-in-string" to objectMapper.writeValueAsString(param163),
                "react/forbid-component-props" to objectMapper.writeValueAsString(param164),
                "template-curly-spacing" to objectMapper.writeValueAsString(param165),
                "constructor-super" to objectMapper.writeValueAsString(param166),
                "block-scoped-var" to objectMapper.writeValueAsString(param167),
                "no-whitespace-before-property" to objectMapper.writeValueAsString(param168),
                "react/sort-prop-types" to objectMapper.writeValueAsString(param169),
                "no-cond-assign" to objectMapper.writeValueAsString(param170),
                "no-prototype-builtins" to objectMapper.writeValueAsString(param171),
                "sort-imports" to objectMapper.writeValueAsString(param172),
                "react/sort-comp" to objectMapper.writeValueAsString(param173),
                "strict" to objectMapper.writeValueAsString(param174),
                "init-declarations" to objectMapper.writeValueAsString(param175),
                "prefer-promise-reject-errors" to objectMapper.writeValueAsString(param176),
                "dot-notation" to objectMapper.writeValueAsString(param177),
                "global-require" to objectMapper.writeValueAsString(param178),
                "no-inline-comments" to objectMapper.writeValueAsString(param179),
                "no-irregular-whitespace" to objectMapper.writeValueAsString(param180),
                "no-useless-concat" to objectMapper.writeValueAsString(param181),
                "no-sequences" to objectMapper.writeValueAsString(param182),
                "no-use-before-define" to objectMapper.writeValueAsString(param183),
                "max-lines" to objectMapper.writeValueAsString(param184),
                "no-loop-func" to objectMapper.writeValueAsString(param185),
                "no-mixed-requires" to objectMapper.writeValueAsString(param186),
                "max-params" to objectMapper.writeValueAsString(param187),
                "no-global-assign" to objectMapper.writeValueAsString(param188),
                "vue/html-self-closing" to objectMapper.writeValueAsString(param189),
                "no-empty-function" to objectMapper.writeValueAsString(param190),
                "react/no-find-dom-node" to objectMapper.writeValueAsString(param191),
                "react/prop-types" to objectMapper.writeValueAsString(param192),
                "semi-spacing" to objectMapper.writeValueAsString(param193),
                "line-comment-position" to objectMapper.writeValueAsString(param194),
                "no-unsafe-negation" to objectMapper.writeValueAsString(param195),
                "no-shadow-restricted-names" to objectMapper.writeValueAsString(param196),
                "quote-props" to objectMapper.writeValueAsString(param197),
                "nonblock-statement-body-position" to objectMapper.writeValueAsString(param198),
                "no-new" to objectMapper.writeValueAsString(param199),
                "react/prefer-stateless-function" to objectMapper.writeValueAsString(param200),
                "object-curly-spacing" to objectMapper.writeValueAsString(param201),
                "react/jsx-wrap-multilines" to objectMapper.writeValueAsString(param202),
                "no-ex-assign" to objectMapper.writeValueAsString(param203),
                "react/jsx-no-undef" to objectMapper.writeValueAsString(param204),
                "react/no-children-prop" to objectMapper.writeValueAsString(param205),
                "no-var" to objectMapper.writeValueAsString(param206),
                "vue/no-duplicate-attributes" to objectMapper.writeValueAsString(param207),
                "vue/valid-template-root" to objectMapper.writeValueAsString(param208),
                "react/no-direct-mutation-state" to objectMapper.writeValueAsString(param209),
                "no-delete-var" to objectMapper.writeValueAsString(param210),
                "react/default-props-match-prop-types" to objectMapper.writeValueAsString(param211),
                "space-in-parens" to objectMapper.writeValueAsString(param212),
                "vue/no-dupe-keys" to objectMapper.writeValueAsString(param213),
                "react/void-dom-elements-no-children" to objectMapper.writeValueAsString(param214),
                "no-fallthrough" to objectMapper.writeValueAsString(param215),
                "react/jsx-no-literals" to objectMapper.writeValueAsString(param216),
                "no-empty-character-class" to objectMapper.writeValueAsString(param217),
                "no-multi-spaces" to objectMapper.writeValueAsString(param218),
                "prefer-destructuring" to objectMapper.writeValueAsString(param219),
                "react/jsx-no-bind" to objectMapper.writeValueAsString(param220),
                "vue/valid-v-text" to objectMapper.writeValueAsString(param221),
                "react/jsx-indent-props" to objectMapper.writeValueAsString(param222),
                "class-methods-use-this" to objectMapper.writeValueAsString(param223),
                "capitalized-comments" to objectMapper.writeValueAsString(param224),
                "no-class-assign" to objectMapper.writeValueAsString(param225),
                "react/jsx-key" to objectMapper.writeValueAsString(param226),
                "vue/valid-v-cloak" to objectMapper.writeValueAsString(param227),
                "multiline-ternary" to objectMapper.writeValueAsString(param228),
                "guard-for-in" to objectMapper.writeValueAsString(param229),
                "vue/valid-v-model" to objectMapper.writeValueAsString(param230),
                "valid-typeof" to objectMapper.writeValueAsString(param231),
                "vue/require-prop-types" to objectMapper.writeValueAsString(param232),
                "new-cap" to objectMapper.writeValueAsString(param233),
                "react/jsx-equals-spacing" to objectMapper.writeValueAsString(param234),
                "react/jsx-first-prop-new-line" to objectMapper.writeValueAsString(param235),
                "no-multi-assign" to objectMapper.writeValueAsString(param236),
                "no-extend-native" to objectMapper.writeValueAsString(param237),
                "no-invalid-this" to objectMapper.writeValueAsString(param238),
                "no-func-assign" to objectMapper.writeValueAsString(param239),
                "no-octal-escape" to objectMapper.writeValueAsString(param240),
                "no-duplicate-case" to objectMapper.writeValueAsString(param241),
                "no-inner-declarations" to objectMapper.writeValueAsString(param242),
                "no-unused-expressions" to objectMapper.writeValueAsString(param243),
                "vue/no-confusing-v-for-v-if" to objectMapper.writeValueAsString(param244),
                "yoda" to objectMapper.writeValueAsString(param245),
                "semi" to objectMapper.writeValueAsString(param246),
                "no-extra-semi" to objectMapper.writeValueAsString(param247),
                "react/jsx-handler-names" to objectMapper.writeValueAsString(param248),
                "rest-spread-spacing" to objectMapper.writeValueAsString(param249),
                "react/no-unused-state" to objectMapper.writeValueAsString(param250),
                "no-dupe-keys" to objectMapper.writeValueAsString(param251),
                "no-floating-decimal" to objectMapper.writeValueAsString(param252),
                "no-duplicate-imports" to objectMapper.writeValueAsString(param253),
                "no-path-concat" to objectMapper.writeValueAsString(param254),
                "key-spacing" to objectMapper.writeValueAsString(param255),
                "react/self-closing-comp" to objectMapper.writeValueAsString(param256),
                "padding-line-between-statements" to objectMapper.writeValueAsString(param257),
                "no-debugger" to objectMapper.writeValueAsString(param258),
                "vue/require-v-for-key" to objectMapper.writeValueAsString(param259),
                "array-callback-return" to objectMapper.writeValueAsString(param260),
                "max-depth" to objectMapper.writeValueAsString(param261),
                "arrow-spacing" to objectMapper.writeValueAsString(param262),
                "react/require-default-props" to objectMapper.writeValueAsString(param263),
                "func-call-spacing" to objectMapper.writeValueAsString(param264),
                "brace-style" to objectMapper.writeValueAsString(param265),
                "no-empty-pattern" to objectMapper.writeValueAsString(param266),
                "space-before-function-paren" to objectMapper.writeValueAsString(param267),
                "object-property-newline" to objectMapper.writeValueAsString(param268),
                "arrow-parens" to objectMapper.writeValueAsString(param269),
                "no-sparse-arrays" to objectMapper.writeValueAsString(param270),
                "func-id-match" to objectMapper.writeValueAsString(param271),
                "getter-return" to objectMapper.writeValueAsString(param272),
                "react/jsx-no-duplicate-props" to objectMapper.writeValueAsString(param273),
                "no-div-regex" to objectMapper.writeValueAsString(param274),
                "object-curly-newline" to objectMapper.writeValueAsString(param275),
                "no-new-symbol" to objectMapper.writeValueAsString(param276),
                "no-await-in-loop" to objectMapper.writeValueAsString(param277),
                "no-empty" to objectMapper.writeValueAsString(param278),
                "no-useless-call" to objectMapper.writeValueAsString(param279),
                "no-undef-init" to objectMapper.writeValueAsString(param280),
                "use-isnan" to objectMapper.writeValueAsString(param281),
                "prefer-const" to objectMapper.writeValueAsString(param282),
                "padded-blocks" to objectMapper.writeValueAsString(param283),
                "sort-vars" to objectMapper.writeValueAsString(param284),
                "react/jsx-filename-extension" to objectMapper.writeValueAsString(param285),
                "no-unmodified-loop-condition" to objectMapper.writeValueAsString(param286),
                "no-multiple-empty-lines" to objectMapper.writeValueAsString(param287),
                "no-restricted-imports" to objectMapper.writeValueAsString(param288),
                "no-process-exit" to objectMapper.writeValueAsString(param289),
                "comment-ratio" to objectMapper.writeValueAsString(param290),
                "valid-jsdoc" to objectMapper.writeValueAsString(param291),
                "react/no-did-update-set-state" to objectMapper.writeValueAsString(param292),
                "arrow-body-style" to objectMapper.writeValueAsString(param293),
                "require-yield" to objectMapper.writeValueAsString(param294),
                "space-indent" to objectMapper.writeValueAsString(param295),
                "operator-assignment" to objectMapper.writeValueAsString(param296),
                "no-new-func" to objectMapper.writeValueAsString(param297),
                "no-else-return" to objectMapper.writeValueAsString(param298),
                "vue/html-end-tags" to objectMapper.writeValueAsString(param299),
                "no-restricted-syntax" to objectMapper.writeValueAsString(param300),
                "no-new-wrappers" to objectMapper.writeValueAsString(param301),
                "vue/no-side-effects-in-computed-properties" to objectMapper.writeValueAsString(param302),
                "wrap-iife" to objectMapper.writeValueAsString(param303),
                "no-warning-comments" to objectMapper.writeValueAsString(param304),
                "prefer-arrow-callback" to objectMapper.writeValueAsString(param305),
                "no-caller" to objectMapper.writeValueAsString(param306),
                "id-blacklist" to objectMapper.writeValueAsString(param307),
                "vue/valid-v-html" to objectMapper.writeValueAsString(param308),
                "no-useless-computed-key" to objectMapper.writeValueAsString(param309),
                "react/boolean-prop-naming" to objectMapper.writeValueAsString(param310),
                "no-case-declarations" to objectMapper.writeValueAsString(param311),
                "no-magic-numbers" to objectMapper.writeValueAsString(param312),
                "vue/require-component-is" to objectMapper.writeValueAsString(param313),
                "no-redeclare" to objectMapper.writeValueAsString(param314),
                "no-dupe-class-members" to objectMapper.writeValueAsString(param315),
                "react/forbid-elements" to objectMapper.writeValueAsString(param316),
                "no-negated-condition" to objectMapper.writeValueAsString(param317),
                "curly" to objectMapper.writeValueAsString(param318),
                "eqeqeq" to objectMapper.writeValueAsString(param319),
                "no-return-assign" to objectMapper.writeValueAsString(param320),
                "react/style-prop-object" to objectMapper.writeValueAsString(param321),
                "no-eq-null" to objectMapper.writeValueAsString(param322),
                "react/jsx-no-comment-textnodes" to objectMapper.writeValueAsString(param323),
                "no-buffer-constructor" to objectMapper.writeValueAsString(param324),
                "no-this-before-super" to objectMapper.writeValueAsString(param325),
                "camelcase" to objectMapper.writeValueAsString(param326),
                "react/no-multi-comp" to objectMapper.writeValueAsString(param327),
                "no-restricted-globals" to objectMapper.writeValueAsString(param328),
                "no-plusplus" to objectMapper.writeValueAsString(param329),
                "no-extra-bind" to objectMapper.writeValueAsString(param330),
                "react/react-in-jsx-scope" to objectMapper.writeValueAsString(param331),
                "space-infix-ops" to objectMapper.writeValueAsString(param332),
                "vue/no-multi-spaces" to objectMapper.writeValueAsString(param333),
                "no-process-env" to objectMapper.writeValueAsString(param334),
                "vue/v-on-style" to objectMapper.writeValueAsString(param335),
                "react/no-deprecated" to objectMapper.writeValueAsString(param336),
                "require-await" to objectMapper.writeValueAsString(param337),
                "array-element-newline" to objectMapper.writeValueAsString(param338),
                "react/no-will-update-set-state" to objectMapper.writeValueAsString(param339),
                "vue/order-in-components" to objectMapper.writeValueAsString(param340),
                "id-match" to objectMapper.writeValueAsString(param341),
                "react/require-optimization" to objectMapper.writeValueAsString(param342),
                "no-unexpected-multiline" to objectMapper.writeValueAsString(param343),
                "no-extra-boolean-cast" to objectMapper.writeValueAsString(param344),
                "func-names" to objectMapper.writeValueAsString(param345),
                "react/forbid-prop-types" to objectMapper.writeValueAsString(param346),
                "vue/valid-v-else" to objectMapper.writeValueAsString(param347),
                "vue/valid-v-on" to objectMapper.writeValueAsString(param348),
                "no-catch-shadow" to objectMapper.writeValueAsString(param349),
                "react/display-name" to objectMapper.writeValueAsString(param350),
                "no-constant-condition" to objectMapper.writeValueAsString(param351),
                "runtime/threadsafe_fn" to objectMapper.writeValueAsString(param352),
                "runtime/member_string_references" to objectMapper.writeValueAsString(param353),
                "build/header_guard" to objectMapper.writeValueAsString(param355),
                "build/endif_comment" to objectMapper.writeValueAsString(param356),
                "runtime/printf_format" to objectMapper.writeValueAsString(param357),
                "readability/braces" to objectMapper.writeValueAsString(param358),
                "runtime/references" to objectMapper.writeValueAsString(param360),
                "whitespace/operators" to objectMapper.writeValueAsString(param361),
                "readability/union_name" to objectMapper.writeValueAsString(param362),
                "runtime/printf" to objectMapper.writeValueAsString(param363),
                "build/include_what_you_use" to objectMapper.writeValueAsString(param364),
                "build/c++14" to objectMapper.writeValueAsString(param365),
                "runtime/init" to objectMapper.writeValueAsString(param366),
                "readability/check" to objectMapper.writeValueAsString(param368),
                "build/class" to objectMapper.writeValueAsString(param369),
                "readability/inheritance" to objectMapper.writeValueAsString(param370),
                "runtime/string" to objectMapper.writeValueAsString(param373),
                "readability/fn_size" to objectMapper.writeValueAsString(param374),
                "whitespace/comments" to objectMapper.writeValueAsString(param375),
                "whitespace/ending_newline" to objectMapper.writeValueAsString(param376),
                "build/deprecated" to objectMapper.writeValueAsString(param377),
                "readability/multiline_comment" to objectMapper.writeValueAsString(param378),
                "build/c++tr1" to objectMapper.writeValueAsString(param379),
                "runtime/rtti" to objectMapper.writeValueAsString(param380),
                "build/explicit_make_pair" to objectMapper.writeValueAsString(param381),
                "whitespace/empty_if_body" to objectMapper.writeValueAsString(param382),
                "build/include" to objectMapper.writeValueAsString(param383),
                "runtime/vlog" to objectMapper.writeValueAsString(param384),
                "whitespace/newline" to objectMapper.writeValueAsString(param385),
                "runtime/indentation_namespace" to objectMapper.writeValueAsString(param386),
                "runtime/virtual" to objectMapper.writeValueAsString(param387),
                "runtime/arrays" to objectMapper.writeValueAsString(param388),
                "readability/function" to objectMapper.writeValueAsString(param389),
                "readability/streams" to objectMapper.writeValueAsString(param390),
                "build/namespaces_literals" to objectMapper.writeValueAsString(param391),
                "readability/enum_name" to objectMapper.writeValueAsString(param392),
                "legal/copyright" to objectMapper.writeValueAsString(param393),
                "readability/nolint" to objectMapper.writeValueAsString(param394),
                "whitespace/empty_conditional_body" to objectMapper.writeValueAsString(param397),
                "readability/alt_tokens" to objectMapper.writeValueAsString(param399),
                "readability/struct_name" to objectMapper.writeValueAsString(param400),
                "whitespace/semicolon" to objectMapper.writeValueAsString(param401),
                "runtime/invalid_increment" to objectMapper.writeValueAsString(param403),
                "whitespace/line_length" to objectMapper.writeValueAsString(param404),
                "build/c++11" to objectMapper.writeValueAsString(param405),
                "readability/nul" to objectMapper.writeValueAsString(param406),
                "readability/class_name" to objectMapper.writeValueAsString(param407),
                "readability/casting" to objectMapper.writeValueAsString(param410),
                "whitespace/parens" to objectMapper.writeValueAsString(param411),
                "build/include_order" to objectMapper.writeValueAsString(param412),
                "readability/constructors" to objectMapper.writeValueAsString(param413),
                "runtime/explicit" to objectMapper.writeValueAsString(param414),
                "runtime/sizeof" to objectMapper.writeValueAsString(param415),
                "whitespace/end_of_line" to objectMapper.writeValueAsString(param416),
                "readability/namespace" to objectMapper.writeValueAsString(param417),
                "readability/multiline_string" to objectMapper.writeValueAsString(param418),
                "runtime/memset" to objectMapper.writeValueAsString(param419),
                "build/forward_decl" to objectMapper.writeValueAsString(param420),
                "whitespace/braces" to objectMapper.writeValueAsString(param421),
                "readability/todo" to objectMapper.writeValueAsString(param423),
                "build/include_alpha" to objectMapper.writeValueAsString(param426),
                "runtime/operator" to objectMapper.writeValueAsString(param428),
                "build/namespaces" to objectMapper.writeValueAsString(param429),
                //"readability/utf8-tosa" to objectMapper.writeValueAsString(param430),
                "runtime/int" to objectMapper.writeValueAsString(param431),
                "build/storage_class" to objectMapper.writeValueAsString(param432),
                "whitespace/todo" to objectMapper.writeValueAsString(param433),
                "whitespace/pos_braces" to objectMapper.writeValueAsString(param434),
                "whitespace/tab" to objectMapper.writeValueAsString(param435),
                "whitespace/comma" to objectMapper.writeValueAsString(param436),
                "whitespace/indent" to objectMapper.writeValueAsString(param437),
                "readability/utf8" to objectMapper.writeValueAsString(param438),
                "whitespace/blank_line" to objectMapper.writeValueAsString(param439),
                "whitespace/empty_loop_body" to objectMapper.writeValueAsString(param440),
                "whitespace/forcolon" to objectMapper.writeValueAsString(param441),
                "build/printf_format" to objectMapper.writeValueAsString(param442),
                "runtime/casting" to objectMapper.writeValueAsString(param444),
                "unconvert/convert" to objectMapper.writeValueAsString(param445),
                "structcheck/field" to objectMapper.writeValueAsString(param446),
                "staticcheck/unused" to objectMapper.writeValueAsString(param447),
                "gosimple/for" to objectMapper.writeValueAsString(param448),
                "golint/interfacecomment" to objectMapper.writeValueAsString(param449),
                "gas/crypto" to objectMapper.writeValueAsString(param450),
                "gosimple/sendrecv" to objectMapper.writeValueAsString(param451),
                "gas/crypto" to objectMapper.writeValueAsString(param452),
                "gas/crypto" to objectMapper.writeValueAsString(param453),
                "gas/crypto" to objectMapper.writeValueAsString(param454),
                "gas/crypto" to objectMapper.writeValueAsString(param455),
                "gas/crypto" to objectMapper.writeValueAsString(param456),
                "gas/crypto" to objectMapper.writeValueAsString(param457),
                "staticcheck/append" to objectMapper.writeValueAsString(param458),
                "goconst/string" to objectMapper.writeValueAsString(param459),
                "staticcheck/timeticker" to objectMapper.writeValueAsString(param460),
                "golint/args" to objectMapper.writeValueAsString(param461),
                "golint/fnsize" to objectMapper.writeValueAsString(param462),
                "maligned/size" to objectMapper.writeValueAsString(param463),
                "unused/unused" to objectMapper.writeValueAsString(param464),
                "varcheck/unused" to objectMapper.writeValueAsString(param465),
                "gas/tls" to objectMapper.writeValueAsString(param466),
                "gas/httpoxy" to objectMapper.writeValueAsString(param467),
                "gosimple/copy" to objectMapper.writeValueAsString(param468),
                "golint/string" to objectMapper.writeValueAsString(param469),
                "vet/vet" to objectMapper.writeValueAsString(param470),
                "golint/comment" to objectMapper.writeValueAsString(param471),
                "golint/funcpara" to objectMapper.writeValueAsString(param472),
                "gas/file" to objectMapper.writeValueAsString(param473),
                "staticcheck/resource" to objectMapper.writeValueAsString(param474),
                "gas/calls" to objectMapper.writeValueAsString(param475),
                "staticcheck/explicit" to objectMapper.writeValueAsString(param476),
                "golint/funccomment" to objectMapper.writeValueAsString(param477),
                "golint/type" to objectMapper.writeValueAsString(param478),
                "staticcheck/break" to objectMapper.writeValueAsString(param479),
                "golint/equivalent" to objectMapper.writeValueAsString(param480),
                "gosimple/usageadvice" to objectMapper.writeValueAsString(param481),
                "gas/subprocess" to objectMapper.writeValueAsString(param482),
                "staticcheck/condition" to objectMapper.writeValueAsString(param483),
                "gosimple/boolcmp" to objectMapper.writeValueAsString(param484),
                "staticcheck/deprecated" to objectMapper.writeValueAsString(param485),
                "gas/escape" to objectMapper.writeValueAsString(param486),
                "vetshadow/shadow" to objectMapper.writeValueAsString(param487),
                "staticcheck/loop" to objectMapper.writeValueAsString(param488),
                "gas/network" to objectMapper.writeValueAsString(param489),
                "unparam/unused" to objectMapper.writeValueAsString(param490),
                "golint/ret" to objectMapper.writeValueAsString(param491),
                "staticcheck/boolean" to objectMapper.writeValueAsString(param492),
                "staticcheck/iowriter" to objectMapper.writeValueAsString(param493),
                "safesql/sql" to objectMapper.writeValueAsString(param494),
                "golint/funcret" to objectMapper.writeValueAsString(param495),
                "gas/sql" to objectMapper.writeValueAsString(param496),
                "golint/noptr" to objectMapper.writeValueAsString(param497),
                "golint/structcomment" to objectMapper.writeValueAsString(param498),
                "golint/package" to objectMapper.writeValueAsString(param499),
                "deadcode/unused" to objectMapper.writeValueAsString(param500),
                "staticcheck/return" to objectMapper.writeValueAsString(param501),
                "goimports/notimport" to objectMapper.writeValueAsString(param502),
                "staticcheck/routine" to objectMapper.writeValueAsString(param503),
                "staticcheck/overrun" to objectMapper.writeValueAsString(param504),
                "errcheck/retvalue" to objectMapper.writeValueAsString(param505),
                "gas/permission" to objectMapper.writeValueAsString(param506),
                "staticcheck/simple" to objectMapper.writeValueAsString(param507),
                "staticcheck/infinite" to objectMapper.writeValueAsString(param508),
                "interfacer/interface" to objectMapper.writeValueAsString(param509),
                "staticcheck/defer" to objectMapper.writeValueAsString(param510),
                "golint/replace" to objectMapper.writeValueAsString(param511),
                "staticcheck/signal" to objectMapper.writeValueAsString(param512),
                "staticcheck/sleep" to objectMapper.writeValueAsString(param513),
                "golint/convar" to objectMapper.writeValueAsString(param514),
                "ineffassign/assign" to objectMapper.writeValueAsString(param515),
                "staticcheck/args" to objectMapper.writeValueAsString(param516),
                "gas/error" to objectMapper.writeValueAsString(param517),
                "golint/print" to objectMapper.writeValueAsString(param518),
                "golint/naming" to objectMapper.writeValueAsString(param519),
                "gas/modulus" to objectMapper.writeValueAsString(param520),
                "staticcheck/exit" to objectMapper.writeValueAsString(param521),
                "golint/decl" to objectMapper.writeValueAsString(param522),
                "misspell/spell" to objectMapper.writeValueAsString(param523),
                "staticcheck/efficient" to objectMapper.writeValueAsString(param524),
                "unparam/value" to objectMapper.writeValueAsString(param525),
                "staticcheck/emptybranch" to objectMapper.writeValueAsString(param526),
                "staticcheck/goroutine" to objectMapper.writeValueAsString(param527),
                "staticcheck/compare" to objectMapper.writeValueAsString(param528),
                "staticcheck/exp" to objectMapper.writeValueAsString(param529),
                "staticcheck/httpheader" to objectMapper.writeValueAsString(param530),
                "gofmt/notformat" to objectMapper.writeValueAsString(param531),
                "staticcheck/assign" to objectMapper.writeValueAsString(param532),
                "staticcheck/exec" to objectMapper.writeValueAsString(param533),
                "nakedret/ret" to objectMapper.writeValueAsString(param534),
                "max-len" to objectMapper.writeValueAsString(param535)
        )

    }


}
