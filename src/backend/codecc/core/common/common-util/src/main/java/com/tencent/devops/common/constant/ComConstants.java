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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to
 * use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
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

package com.tencent.devops.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 公共常量类
 *
 * @version V1.0
 * @date 2019/5/1
 */
public interface ComConstants {
    String SYSTEM_USER = "system";

    /**
     * 按处理人统计的当前遗留待修复告警的图表合计节点的名称
     */
    String TOTAL_CHART_NODE = "Total";

    /**
     * 是否中断后再执行的标志位，如果任务已经在执行，是否中断后再执行，0（不中断），1（中断），为空默认不中断
     */
    Integer ABORT_ANALYSIS_TASK_FLAG = 1;

    /**
     * 默认过滤路径类型
     */
    String PATH_TYPE_DEFAULT = "DEFAULT";

    /**
     * CODE_YML过滤路径类型
     */
    String PATH_TYPE_CODE_YML = "CODE_YML";

    /**
     * BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
     */
    String BIZ_SERVICE_POSTFIX = "BizService";

    /**
     * 通用Processor类名（CommonBatchBizTypeProcessorImpl）的前缀名,比如：COVERITYBatchMarkDefectProcessorImpl
     */
    String BATCH_PROCESSOR_INFIX = "Batch";

    /**
     * 通用BizService类名（CommonBizTypeBizServiceImpl）的前缀名
     */
    String COMMON_BIZ_SERVICE_PREFIX = "Common";
    /**
     * 项目已接入工具的名称之间的分隔符
     */
    String TOOL_NAMES_SEPARATOR = ",";

    /**
     * 分号分隔符
     */
    String SEPARATOR_SEMICOLON = ":";

    /**
     * GOML工具特殊参数rel_path
     */
    String PARAMJSON_KEY_REL_PATH = "rel_path";
    /**
     * GOML工具特殊参数go_path
     */
    String PARAMJSON_KEY_GO_PATH = "go_path";

    /**
     * 合计
     */
    String SUM = "合计";

    /**
     * 严重程度类别：严重（1），一般（2），提示（4）
     */
    int SERIOUS = 1;
    int NORMAL = 2;
    int PROMPT = 4;

    /**
     * 天数
     */
    int DAY_FOURTEEN = 14;
    int DAY_THIRTY = 30;
    int DAY_THIRTYONE = 31;

    Map<Integer, String> severityMap = new HashMap<Integer, String>() {
        {
            put(SERIOUS, "严重");
            put(NORMAL, "一般");
            put(PROMPT, "提示");
        }
    };

    /**
     * 数据库中表示缺陷严重程度为提示的值为3
     */
    int PROMPT_IN_DB = 3;

    /**
     * 常用整数
     */
    long COMMON_NUM_10000L = 10000L;
    long COMMON_NUM_1000L = 1000L;
    long COMMON_NUM_1L = 1L;
    long COMMON_NUM_0L = 0L;
    float COMMON_NUM_0F = 0F;
    double COMMON_NUM_0D = 0.0D;
    /**
     * ci新建任务前缀
     */
    String PIPELINE_ENNAME_PREFIX = "DEVOPS";


    String GONGFENG_ENNAME_PREFIX = "CODE";

    /**
     * CodeCC服务新建任务前缀
     */
    String CODECC_ENNAME_PREFIX = "CODECC";

    /**
     * 旧的CodeCC服务新建任务前缀
     */
    String OLD_CODECC_ENNAME_PREFIX = "LD_";

    /**
     * codecc平台转bs平台元数据类型
     */
    String METADATA_TYPE = "LANG";
    /**
     * 其他項目語言
     */
    String OTHERS_PROJECT_LANGUAGE = "OTHERS";
    /**
     * 字符串定界符
     */
    String STRING_DELIMITER = "|";
    /**
     * 字符串分隔符
     */
    String STRING_SPLIT = ",";
    /**
     * 字符串标识符
     */
    String STRING_TIPS = "-";
    /**
     * 字符串前后缀
     */
    String STRING_PREFIX_OR_SUFFIX = "";
    /**
     * 字符串前后缀
     */
    String STRING_NULL_ARRAY = "[]";
    /**
     * lint工具类前缀
     */
    String TOOL_LINT_PREFIX = "LINT";

    String DEFECT_STATUS_CLOSED = "closed";

    /**
     * 日期常量
     */
    String DATE_TODAY = "today";
    String DATE_YESTERDAY = "yesterday";
    String DATE_MONDAY = "monday";
    String DATE_LAST_MONDAY = "lastMonday";

    /**
     * 风险系数的配置的key前缀,
     * 比如：圈复杂度的风险系数，RISK_FACTOR_CONFIG:CCN hash
     */
    String PREFIX_RISK_FACTOR_CONFIG = "CONFIG_RISK_FACTOR";

    /**
     * 工具的顺序表
     */
    String KEY_TOOL_ORDER = "TOOL_ORDER";

    /**
     * 语言的顺序表
     */
    String KEY_LANG_ORDER = "LANG_ORDER";

    /**
     * 默认过滤路径
     */
    String KEY_DEFAULT_FILTER_PATH = "DEFAULT_FILTER_PATH";

    /**
     * 工具语言字段
     */
    String KEY_CODE_LANG = "LANG";


    /**
     * 过滤配置
     */
    String KEY_FILTER_CONFIG = "FILTER_CONFIG";

    /**
     * appCode映射
     */
    String KEY_APP_CODE_MAPPING = "APP_CODE_MAPPING";


    /**
     * 开源扫描版本配置
     */
    String KEY_OPENSOURCE_VERSION = "OPENSOURCE_VERSION";

    /**
     * 开源扫描时间周期
     */
    String KEY_OPENSOURCE_PERIOD = "OPENSOURCE_PERIOD";


    /**
     * 开源扫描路由配置
     */
    String KEY_OPENSOURCE_ROUTE = "OPENSOURCE_ROUTE";

    /**
     * 开源扫描下发频率配置
     */
    String KEY_OPENSOURCE_FREQUENCY = "OPENSOURCE_FREQUENCY";

    /**
     * 灰度测试的bg
     */
    String KEY_TOOL_NAMES_GRAY_TEST_BG = "TOOL_NAMES_GRAY_TEST_BG";

    /**
     * 屏蔽用户成员名单
     */
    String KEY_EXCLUDE_USER_LIST = "EXCLUDE_USER_MEMBER";

    /**
     * 管理员名单
     */
    String KEY_ADMIN_MEMBER = "ADMIN_MEMBER";

    /**
     * 开源治理下发校验配置
     */
    String GONGFENG_CHECK_CONFIG = "GONGFENG_CHECK_CONFIG";

    /**
     * 工具对应的规范规则集ID的配置
     */
    String STANDARD_CHECKER_SET_ID = "STANDARD_CHECKER_SET_ID";

    /**
     * 分号
     */
    String SEMICOLON = ";";
    /**
     * ------------------------操作历史记录操作类型------------------
     */
    String REGISTER_TOOL = "register_tool";
    String MODIFY_INFO = "modify_info";
    String ENABLE_ACTION = "enable_action";
    String DISABLE_ACTION = "diable_action";
    String OPEN_CHECKER = "open_checker";
    String CLOSE_CHECKER = "close_checker";
    String TRIGGER_ANALYSIS = "trigger_analysis";
    String AUTHOR_TRANSFER = "author_transfer";
    /**
     * ------------------------操作历史记录操作功能id------------------
     */
    //注册工具
    String FUNC_REGISTER_TOOL = "register_tool";
    //修改任务信息
    String FUNC_TASK_INFO = "task_info";
    //切换任务状态
    String FUNC_TASK_SWITCH = "task_switch";
    //切换工具状态
    String FUNC_TOOL_SWITCH = "tool_switch";
    //任务代码库更新
    String FUNC_CODE_REPOSITORY = "task_code";
    //规则配置
    String FUNC_CHECKER_CONFIG = "checker_config";
    //触发立即分析
    String FUNC_TRIGGER_ANALYSIS = "trigger_analysis";
    //定时扫描修改
    String FUNC_SCAN_SCHEDULE = "scan_schedule";
    //过滤路径
    String FUNC_FILTER_PATH = "filter_path";
    //告警管理
    String FUNC_DEFECT_MANAGE = "defect_manage";
    /**
     * ----------------------------end----------------------------
     */

    /*-------------------------------Accept-Language-----------------------*/
    String ZH_CN = "ZH-CN";
    /**
     * Node规则包
     */
    String NODE = "NODE";
    /**
     * 风格规则包
     */
    String STYLISTIC = "STYLISTIC";
    /**
     * 严格模式包
     */
    String STRICT_MODE = "STRICT_MODE";
    /**
     * 逻辑规则包
     */
    String LOGICA = "LOGICAL";
    /**
     * 默认规则包
     */
    String DEFAULT = "DEFAULT";
    /**
     * 腾讯开源包
     */
    String TOSA = "TOSA";
    /**
     * 变量规则包
     */
    String VARIABLE = "VARIABLE";
    /**
     * ES6规则包
     */
    String ES6 = "ES6";
    /**
     * 最佳实践包
     */
    String BEST_PRACTICES = "BEST_PRACTICES";
    /**
     * 头文件规则包
     */
    String HEADER_FILE = "HEADER_FILE";
    /**
     * 系统API包
     */
    String SYS_API = "SYS_API";
    /**
     * OneSDK规则包
     */
    String ONESDK = "ONESDK";
    /**
     * 安全规则包
     */
    String SECURITY = "SECURITY";
    /**
     * 命名规范包
     */
    String NAMING = "NAMING";
    /**
     * 注释规则包
     */
    String COMMENT = "COMMENT";
    /**
     * 格式规范包
     */
    String FORMAT = "FORMAT";
    /**
     * ESLINT参数名 - eslint_rc
     */
    String PARAM_ESLINT_RC = "eslint_rc";
    /**
     * GOML参数名 - go_path
     */
    String PARAM_GOML_GO_PATH = "go_path";
    /**
     * ----------------------------end----------------------------
     */
    /**
     * GOML参数名 - rel_path
     */
    String PARAM_GOML_REL_PATH = "rel_path";
    /**
     * PYLINT参数名 - py_version
     */
    String PARAM_PYLINT_PY_VERSION = "py_version";
    /**
     * SPOTBUGS参数名 - script_type
     */
    String PARAM_SPOTBUGS_SCRIPT_TYPE = "script_type";
    /**
     * SPOTBUGS参数名 - script_content
     */
    String PARAM_SPOTBUGS_SCRIPT_CONTENT = "script_content";
    /**
     * PHPCS参数名 - script_type
     */
    String PARAM_PHPCS_XX = "script_type";
    /**
     * 圈复杂度阈值
     */
    String KEY_CCN_THRESHOLD = "ccn_threshold";
    /**
     * 默认圈复杂度阈值
     */
    int DEFAULT_CCN_THRESHOLD = 20;
    /**
     * PHPCS规范
     */
    String KEY_PHPCS_STANDARD = "phpcs_standard";
    /**
     * 下划线
     */
    String KEY_UNDERLINE = "_";
    String BLUEKING_LANGUAGE = "blueking_language";
    String KEY_DOCKERNIZED_TOOLS = "DOCKERNIZED_TOOLS";

    /**
     * 一键开启规则集key
     */
    String ONCE_CHECKER_SET_KEY = "ONCE_CHECKER_SET_KEY";


    String GRAY_PROJECT_PREFIX = "GRAY_TASK_POOL_";

    /**
     * 灰度任务池配置类型
     */
    String GRAY_TASK_POOL_CONFIG = "GRAY_TASK_POOL_CONFIG";

    /**
     * 每日分析代码行统计
     */
    String TOTAL_BLANK = "totalBlank";
    String TOTAL_COMMENT = "totalComment";
    String TOTAL_CODE = "totalCode";

    /**
     * 业务类型
     */
    enum BusinessType {
        /**
         * 注册接入新工具
         */
        REGISTER_TOOL("RegisterTool"),

        /**
         * 告警查询
         */
        QUERY_WARNING("QueryWarning"),

        /**
         * 数据报表
         */
        DATA_REPORT("DataReport"),

        /**
         * 分析记录上报
         */
        ANALYZE_TASK("AnalyzeTask"),

        /**
         * 获取分析记录
         */
        GET_TASKLOG("GetTaskLog"),

        /**
         * 路径屏蔽
         */
        FILTER_PATH("FilterPath"),

        /**
         * 工具侧上报告警数据
         */
        UPLOAD_DEFECT("UploadDefect"),

        /**
         * 忽略告警
         */
        IGNORE_DEFECT("IgnoreDefect"),

        /**
         * 恢复忽略告警
         */
        REVERT_IGNORE("RevertIgnore"),

        /**
         * 告警处理人分配
         */
        ASSIGN_DEFECT("AssignDefect"),

        /**
         * 告警处理人转换
         */
        AUTHOR_TRANS("AuthorTrans"),

        /**
         * 告警标志修改
         */
        MARK_DEFECT("MarkDefect"),

        /**
         * 查询规则包
         */
        QUERY_PKG("QueryCheckerPkg"),

        /**
         * 配置规则包
         */
        CONFIG_PKG("ConfigCheckerPkg"),

        /**
         * 查询分析统计结果
         */
        QUERY_STATISTIC("QueryStatistic"),

        /**
         * 树形服务
         */
        TREE_SERVICE("Tree"),

        /**
         * 分析结束生成报告
         */
        CHECK_REPORT("CheckerReport"),

        /**
         * 告警生成
         */
        DEFECT_OPERATE("DefectOperate"),

        /**
         * OP告警数据
         */
        DEFECT_DATA("DefectData");

        private String value;

        BusinessType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    /**
     * 工具类型
     */
    enum Tool {
        COVERITY,
        KLOCWORK,
        PINPOINT,
        SENSITIVE,
        HORUSPY,
        WOODPECKER_SENSITIVE,
        RIPS,
        CPPLINT,
        CHECKSTYLE,
        ESLINT,
        STYLECOP,
        GOML,
        DETEKT,
        PHPCS,
        PYLINT,
        OCCHECK,
        CCN,
        DUPC,
        TSCLUA,
        SPOTBUGS,
        CLOC,
        FLAKE8,
        CLOJURE,
        IP_CHECK,
        GITHUBSTATISTIC,
        SCC;
    }

    /**
     * 工具当前的状态/当前步骤的状态
     */
    enum StepStatus {
        SUCC(0),
        FAIL(1),

        /**
         * 只有步骤为AUTH(-2)：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         */
        NO_CHANGE(2),

        /**
         * 只有步骤为AUTH(-2)：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         */
        AUTH_ING(3);

        private int stepStatus;

        StepStatus(int stepStatus) {
            this.stepStatus = stepStatus;
        }

        public int value() {
            return this.stepStatus;
        }
    }

    /**
     * 上报分析步骤的状态标记,包括成功、失败、进行中、中断
     */
    enum StepFlag {
        SUCC(1),
        FAIL(2),
        PROCESSING(3),
        ABORT(4);

        private int stepFlag;

        StepFlag(int stepStatus) {
            this.stepFlag = stepStatus;
        }

        public int value() {
            return this.stepFlag;
        }
    }

    /**
     * 项目接入多工具步骤
     */
    enum Step4MutliTool {
        /**
         * 步骤：代码托管帐号鉴权，stepStatus = 0表示正常，1表示帐号密码过期，2表示代码没更新,3表示正在鉴权
         *
         * @date 2017/9/13
         * @version V2.4
         */
        AUTH(-2),

        /**
         * 申请中，该状态已经废弃
         */
        APPLYING(-1),

        /**
         * 接入完成
         */
        READY(0),

        /**
         * 排队状态
         */
        QUEUE(1),

        /**
         * 代码下载
         */
        DOWNLOAD(2),

        /**
         * 代码扫描
         */
        SCAN(3),

        /**
         * 代码缺陷提交
         */
        COMMIT(4),

        /**
         * 分析完成
         */
        COMPLETE(5);

        private int value;

        Step4MutliTool(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 项目接入Coverity工具步骤
     */
    enum Step4Cov {
        /**
         * 申请中，该状态已经废弃
         */
        APPLYING(-1),

        /**
         * 接入完成
         */
        READY(0),

        /**
         * 上传
         */
        UPLOAD(1),

        /**
         * 排队状态
         */
        QUEUE(2),

        /**
         * 分析中
         */
        ANALYZE(3),

        /**
         * 工具缺陷提交自带platform
         */
        COMMIT(4),

        /**
         * 将告警从platform同步到codecc
         */
        DEFECT_SYNS(5),

        /**
         * 分析完成
         */
        COMPLETE(6);

        private int value;

        Step4Cov(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 文件类型
     */
    enum FileType {
        NEW(1),
        HISTORY(2),
        FIXED(4),
        IGNORE(8);

        private int value;

        FileType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public String stringValue() {
            return String.valueOf(value);
        }
    }

    /**
     * 规则包分类，默认规则包-0；安全规则包-1；内存规则包-2；编译警告包-3；
     * 系统API包-4；性能问题包-5；表达式问题包-6；可疑问题包-7；定制规则包-8；ONESDK规范规则包-9
     * 腾讯开源包-10；
     */
    enum CheckerPkgKind {
        DEFAULT("0"),
        SECURITY("1"),
        MEMORY("2"),
        PERFORMANCE("3"),
        COMPILE("4"),
        SYS_API("5"),
        EXPRESSION("6"),
        POSSIBLE("7"),
        CUSTOM("8"),
        LOGICAL("9"),
        STYLISTIC("10"),
        BEST_PRACTICES("11"),
        HEADER_FILE("12"),
        ES6("13"),
        NODE("14"),
        VARIABLE("15"),
        STRICT_MODE("16"),
        FORMAT("17"),
        NAMING("18"),
        COMMENT("19"),
        KING_KONG("20"),
        TOSA("21");

        private String value;

        CheckerPkgKind(String value) {
            this.value = value;
        }

        public static String getValueByName(String name) {
            if (values() != null) {
                for (CheckerPkgKind checkerPkgKind : values()) {
                    if (checkerPkgKind.name().equalsIgnoreCase(name)) {
                        return checkerPkgKind.value;
                    }
                }
            }
            return null;
        }

        public String value() {
            return value;
        }
    }

    /**
     * 任务语言
     */
    enum CodeLang {
        C_SHARP(1L, "C#", "C#"),
        C_CPP(2L, "C/C++", "C/C++"),
        JAVA(4L, "JAVA", "JAVA"),
        PHP(8L, "PHP", "PHP"),
        OC(16L, "Objective-C/C++", "OC/OC++"),
        PYTHON(32L, "Python", "Python"),
        JS(64L, "JavaScript", "JS"),
        RUBY(128L, "Ruby", "Ruby"),
        LUA(256L, "LUA", "LUA"),
        GOLANG(512L, "Golang", "Golang"),
        SWIFT(1024L, "SWIFT", "Swift"),
        TYPESCRIPT(2048L, "TypeScript", "TS"),
        KOTLIN(4096L, "Kotlin", "Kotlin"),
        OTHERS(1073741824L, "OTHERS", "其他");

        private Long langValue;

        private String langName;

        private String displayName;

        CodeLang(Long langValue, String langName, String displayName) {
            this.langValue = langValue;
            this.langName = langName;
            this.displayName = displayName;
        }

        public static String getCodeLang(Long value) {
            if (values() != null) {
                for (CodeLang lang : values()) {
                    if (lang.langValue.equals(value)) {
                        return lang.displayName();
                    }
                }
            }
            return null;
        }

        public Long langValue() {
            return langValue;
        }

        public String langName() {
            return langName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /**
     * 风险系数：极高-SH, 高-H，中-M，低-L
     */
    enum RiskFactor {
        SH(1),
        H(2),
        M(4),
        L(8);

        private int value;

        RiskFactor(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * 区分蓝盾codecc任务创建来源
     */
    enum BsTaskCreateFrom {
        /**
         * codecc服务创建的codecc任务
         */
        BS_CODECC("bs_codecc"),

        /**
         * 蓝盾流水线创建的codecc任务
         */
        BS_PIPELINE("bs_pipeline"),

        /**
         * 工蜂代码扫描任务
         */
        GONGFENG_SCAN("gongfeng_scan"),

        /**
         * API 触发创建任务
         */
        API_TRIGGER("api_trigger"),

        /**
         * 定时扫描任务
         */
        TIMING_SCAN("timing_scan");

        private String value;

        BsTaskCreateFrom(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

        @NotNull
        public static Set<String> getByStatType(Set<String> type) {
            Set<String> createFrom;
            if (type != null) {
                createFrom = new HashSet<>();
                if (type.contains(DefectStatType.GONGFENG_SCAN.value)) {
                    // 开源
                    createFrom.add(GONGFENG_SCAN.value());
                }
                if (type.contains(DefectStatType.USER.value)) {
                    // 非开源
                    createFrom.add(BS_CODECC.value());
                    createFrom.add(BS_PIPELINE.value());
                }
            } else {
                createFrom = new HashSet<>(Arrays.asList(BS_CODECC.value(), BS_PIPELINE.value(), GONGFENG_SCAN.value()));
            }
            return createFrom;
        }
    }

    enum EslintFrameworkType {
        standard, vue, react
    }

    /**
     * 工具跟进状态
     */
    enum FOLLOW_STATUS {

        NOT_FOLLOW_UP_0(0), //未跟进
        NOT_FOLLOW_UP_1(1), //未跟进
        EXPERIENCE(2),        //体验
        ACCESSING(3),        //接入中
        ACCESSED(4),        //已接入
        HANG_UP(5),            //挂起
        WITHDRAW(6);        //下架

        private int value;

        FOLLOW_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        @NotNull
        public static List<Integer> getEffectiveStatus() {
            return Arrays.asList(NOT_FOLLOW_UP_0.value, NOT_FOLLOW_UP_1.value, ACCESSED.value, EXPERIENCE.value);
        }
    }

    /**
     * PHPCS规范编码
     */
    enum PHPCSStandardCode {
        PEAR(1),
        Generic(2),
        MySource(4),
        PSR2(8),
        PSR1(16),
        Zend(32),
        PSR12(64),
        Squiz(128);

        private int code;

        PHPCSStandardCode(int code) {
            this.code = code;
        }

        public int code() {
            return this.code;
        }
    }

    /**
     * 工具处理模式
     */
    enum ToolPattern {
        LINT,
        COVERITY,
        KLOCWORK,
        PINPOINT,
        CCN,
        DUPC,
        CLOC,
        STAT,
        TSCLUA;
    }

    /**
     * 流水线工具配置操作类型
     */
    enum PipelineToolUpdateType {
        ADD,
        REPLACE,
        REMOVE,
        GET
    }


    /*------------------------------- 工具参数提示国际化 -----------------------*/
    enum CommonJudge {
        COMMON_Y("Y"),
        COMMON_N("N");
        String value;

        CommonJudge(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }

    }

    /**
     * 任务文件状态
     */
    enum TaskFileStatus {
        NEW(1),
        PATH_MASK(8);

        private int value;

        TaskFileStatus(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * 缺陷类型
     */
    enum DefectType {
        NEW(1),
        HISTORY(2);

        private int value;

        DefectType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public String stringValue() {
            return String.valueOf(value);
        }
    }

    /**
     * 缺陷状态
     */
    enum DefectStatus {
        NEW(1),
        FIXED(2),
        IGNORE(4),
        PATH_MASK(8),
        CHECKER_MASK(16);

        private int value;

        DefectStatus(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    List<String> MASK_STATUS = Arrays.asList("8", "16", "10", "12", "14", "18", "20", "22", "24", "26", "28", "30");

    /**
     * 聚类类型
     */
    enum ClusterType {
        file,
        defect
    }

    /**
     * rdm项目coverity分析状态
     */
    enum RDMCoverityStatus {
        success, failed
    }

    /**
     * 代码托管类型，包括SVN、GIT等
     */
    enum CodeHostingType {
        SVN,
        GIT,
        HTTP_DOWNLOAD,
        GITHUB;
    }

    /**
     * 通用状态，0-启用，1-停用
     */
    enum Status {
        ENABLE(0),
        DISABLE(1);

        private int value;

        Status(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    /**
     * REPAIR: 待修复告警趋势，NEW: 每日新增告警，CLOSE: 每日关闭/修复告警
     */
    enum ChartType {
        REPAIR,
        NEW,
        CLOSE,
    }

    /**
     * 扫描方式
     */
    enum ScanType {
        /**
         * 全量
         */
        FULL(0),

        /**
         * 增量
         */
        INCREMENTAL(1),

        /**
         * diff模式
         */
        DIFF_MODE(2),

        /**
         * 快速增量，以下配置没有变更时，不需要执行扫描，直接生成结果
         * 1.规则
         * 2.规则集
         * 3.路径黑名单
         * 4.路径白名单
         * 5.代码(包括代码库)
         * 6.工具不是重新启用
         * 7.工具镜像
         */
        FAST_INCREMENTAL(3),

        /**
         * 局部增量（其他配置不变，只有规则变化时，coverity/klocwork等编译工具不需要重新执行构建，只需要执行analyze和commit）
         */
        PARTIAL_INCREMENTAL(4);

        public int code;

        ScanType(int code) {
            this.code = code;
        }
    }

    /**
     * 忽略告警原因类型
     */
    enum IgnoreReasonType {
        // 默认为0不是已忽略告警
        DEFAULT(0),
        // 工具误报
        ERROR_DETECT(1),
        // 设计如此
        SPECIAL_PURPOSE(2),
        // 其它
        OTHER(4);

        private int ignoreReasonType;

        private IgnoreReasonType(int ignoreReasonType) {
            this.ignoreReasonType = ignoreReasonType;
        }

        public int value() {
            return this.ignoreReasonType;
        }
    }

    /**
     * 机器人通知范围
     */
    enum BotNotifyRange {
        /**
         * 新增告警
         */
        NEW(1),

        /**
         * 遗留告警(新+旧)
         */
        EXIST(2);

        public int code;

        BotNotifyRange(int code) {
            this.code = code;
        }
    }

    /**
     * 统计项
     */
    enum StaticticItem {
        NEW,
        EXIST,
        CLOSE,
        FIXED,
        EXCLUDE,
        NEW_PROMPT,
        NEW_NORMAL,
        NEW_SERIOUS,
        EXIST_PROMPT,
        EXIST_NORMAL,
        EXIST_SERIOUS;
    }

    /**
     * 报告类型：定时报告T，即时报告I, 开源检查报告O
     */
    enum ReportType {
        T,
        I,
        O,
        A;
    }

    enum InstantReportStatus {
        ENABLED("1"),
        DISABLED("2");

        private String code;

        InstantReportStatus(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }


    enum EmailReceiverType {
        TASK_MEMBER("0"),
        TASK_OWNER("1"),
        CUSTOMIZED("2"),
        NOT_SEND("3"),
        /**
         * 遗留处理人
         */
        ONLY_AUTHOR("4");

        private String code;

        EmailReceiverType(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    /**
     * 告警上报状态
     */
    enum DefectReportStatus {
        PROCESSING,
        SUCCESS,
        FAIL
    }

    enum MarkStatus {
        NOT_MARKED(0),
        MARKED(1),
        NOT_FIXED(2);

        private int value;

        MarkStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * codecc分发路由规则（用于配置在codeccDispatchType中）
     */
    enum CodeCCDispatchRoute {
        //独立构建机集群
        INDEPENDENT(-1L),
        //开源扫描集群
        OPENSOURCE(-2L),
        //devcloud集群
        DEVCLOUD(-101L);

        private Long flag;

        CodeCCDispatchRoute(Long flag) {
            this.flag = flag;
        }

        public Long flag() {
            return this.flag;
        }
    }

    /**
     * 开源扫描规则集类型
     */
    enum OpenSourceCheckerSetType {
        //全量规则集
        FULL,
        //简化规则集
        SIMPLIFIED,
        //两者规则集都配置
        BOTH,
        //oteam专有规则集
        OTEAM,
        //oteam且配置了ci的yml文件专有规则集
        OTEAM_CI
    }

    /**
     * CLOC 告警查询类型
     */
    enum CLOCOrder {
        // 根据文件查询
        FILE,
        // 根据语言查询
        LANGUAGE
    }

    /**
     * 开源失效原因
     */
    enum OpenSourceDisableReason {
        //删除或变为私有
        DELETEORPRIVATE(1),
        //归档
        ARCHIVED(2),
        //不可clone
        NOCLONE(3),
        //无commit记录
        NOCOMMIT(4),
        //owner的问题
        OWNERPROBLEM(5),
        //没有工蜂统计信息
        NOGONGFENGSTAT(6),
        //冗余项目
        REDUNDANTTASK(7),
        //创建或下发过程异常
        CREATETRIGGERERROR(8);
        private Integer code;

        OpenSourceDisableReason(Integer code) {
            this.code = code;
        }

        public Integer getCode() {
            return this.code;
        }
    }

    /**
     * 告警统计类型
     */
    enum StatisticType {
        // 按状态统计
        STATUS,
        // 按严重程度统计
        SEVERITY,
        // 按新旧告警统计
        DEFECT_TYPE
    }


    /**
     * 工具类型
     */
    enum AtomCode {
        CODECC_V2("CodeccCheckAtom"),
        CODECC_V3("CodeccCheckAtomDebug");

        private String code;

        AtomCode(String code) {
            this.code = code;
        }

        public String code() {
            return this.code;
        }
    }

    enum EmailNotifyTemplate {
        BK_PLUGIN_FAILED_TEMPLATE("BK_PLUGIN_FAILED_TEMPLATE");

        private String templateCode;

        EmailNotifyTemplate(String templateCode) {
            this.templateCode = templateCode;
        }

        public String value() {
            return this.templateCode;
        }
    }

    enum WeChatNotifyTemplate {
        BK_PLUGIN_FAILED_TEMPLATE("BK_PLUGIN_FAILED_TEMPLATE");

        private String templateCode;

        WeChatNotifyTemplate(String templateCode) {
            this.templateCode = templateCode;
        }

        public String value() {
            return this.templateCode;
        }
    }


    enum ScanStatus {
        //正在扫描中
        PROCESSING(3),
        //成功
        SUCCESS(0),
        //失败
        FAIL(1);
        private Integer code;

        ScanStatus(Integer code) {
            this.code = code;
        }

        public Integer getCode() {
            return this.code;
        }

        public static String convertScanStatus(Integer code) {
            String status;
            if (PROCESSING.code.equals(code)) {
                status = "分析中";
            } else if (FAIL.code.equals(code)) {
                status = "分析失败";
            } else if (SUCCESS.code.equals(code)) {
                status = "分析成功";
            } else {
                status = "未知状态" + code;
            }
            return status;
        }
    }

    enum TOSAStandardTools {

    }

    enum ToolType {
        STANDARD,
        SECURITY,
        DUPC,
        CCN,
        DEFECT,
        CLOC,
        STAT
    }

    enum BaseConfig {
        // 不支持增量的工具列表
        INCREMENTAL_EXCEPT_TOOLS,

        // 支持coverity增量的灰度任务白名单列表
        INCREMENTAL_TASK_WHITE_LIST,

        // 支持快速增量的灰度任务白名单列表
        FAST_INCREMENTAL_TASK_WHITE_LIST,

        // 支持快速增量的开源灰度任务白名单列表
        FAST_INCREMENTAL_OPENSOURCE_TASK_WHITE_LIST,

        //安全工具
        SECURITY_TOOLS,

        // 规范工具
        STANDARD_TOOLS
    }

    enum ProjectId {
        // Oteam项目都属于这个项目
        CUSTOMPROJ_TEG_CUSTOMIZED,

        // EPC系统的项目
        CUSTOMPROJ_PCG_RD
    }

    String GONGFENG_PROJECT_ID_PREFIX = "CODE_";

    enum DefectStatType {
        /**
         * 所有任务范围
         */
        ALL("all"),
        /**
         * 非开源扫描（服务、流水线）
         */
        USER("user"),
        /**
         * 开源扫描
         */
        GONGFENG_SCAN("gongfeng_scan");

        private String value;

        DefectStatType(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    enum ScanStatType {
        /**
         * 超快增量
         */
        IS_FAST_INCREMENT("IS_FAST_INCRE"),
        /**
         * 非超快增量
         */
        NOT_FAST_INCREMENT("NOT_FAST_INCRE");

        private String value;

        ScanStatType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum CheckerSetType {
        NORMAL("normal"),

        OPEN_SCAN("openScan"),

        EPC_SCAN("epcScan");

        private String value;

        CheckerSetType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @JsonCreator
        public static CheckerSetType forValue(String value) {
            CheckerSetType[] checkerSetTypes = CheckerSetType.values();
            for (CheckerSetType checkerSetType : checkerSetTypes) {
                if (checkerSetType.value.equalsIgnoreCase(value)) {
                    return checkerSetType;
                }
            }
            return NORMAL;
        }
    }

    /**
     * 工具集成进展状态：T-测试，G-灰度，P-发布
     */
    enum ToolIntegratedStatus {
        T(-1),
        G(-2),
        P(0);

        private int value;

        ToolIntegratedStatus(int value) {
            this.value = value;
        }

        public static ToolIntegratedStatus getInstance(int value) {
            ToolIntegratedStatus[] values = ToolIntegratedStatus.values();
            for (ToolIntegratedStatus toolIntegratedStatus : values) {
                if (toolIntegratedStatus.value == value) {
                    return toolIntegratedStatus;
                }
            }
            return P;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 文件生成过程状态标识
     */
    enum FileStatus {
        /**
         * 未开始
         */
        NOT_STARTED("-1"),

        /**
         * 已导出完成
         */
        FINISH("0"),

        /**
         * 正在生成中
         */
        DOING("1");

        private String  code;

        FileStatus(String  code) {
            this.code = code;
        }

        public String  getCode() {
            return this.code;
        }
    }

}
