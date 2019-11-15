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

package com.tencent.devops.common.constant;

/**
 * 公共常量类
 *
 * @version V1.0
 * @date 2019/5/1
 */
public interface ComConstants
{
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
     * BizService的bean名（PatternBizTypeBizService）的后缀名,比如：COVERITYBatchMarkDefectBizService
     */
    String BIZ_SERVICE_POSTFIX = "BizService";
    /**
     * 通用BizService类名（CommonBizTypeBizServiceImpl）的前缀名
     */
    String COMMON_BIZ_SERVICE_PREFIX = "Common";
    /**
     * 项目已接入工具的名称之间的分隔符
     */
    String TOOL_NAMES_SEPARATOR = ",";
    /**
     * GOML工具特殊参数rel_path
     */
    String PARAMJSON_KEY_REL_PATH = "rel_path";
    /**
     * GOML工具特殊参数go_path
     */
    String PARAMJSON_KEY_GO_PATH = "go_path";

    /**
     * 严重程度类别：严重（1），一般（2），提示（4）
     */
    int SERIOUS = 1;
    int NORMAL = 2;
    int PROMPT = 4;
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
    String ENNAME_PREFIX = "DEVOPS";
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
    /**
     * -----------------圈复杂度状态-------------------------
     **/
    String DEFECT_STATUS_ADD = "add";
    String DEFECT_STATUS_UPDATE = "mongotemplate";
    String DEFECT_STATUS_REOPEN = "reopen";
    String DEFECT_STATUS_NEW = "new";
    String DEFECT_STATUS_CLOSED = "closed";
    String DEFECT_STATUS_EXCLUDED = "excluded";
    String DEFECT_STATUS_IGNORE = "ignore";
    String DEFECT_STATUS_UNCLOSED_ADD = "unclosedAdd";
    String DEFECT_STATUS_UNFILTERED_CLOSE = "unfilteredClose";
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
     * 默认过滤路径
     */
    String KEY_DEFAULT_FILTER_PATH = "DEFAULT_FILTER_PATH";


    /**
     * 工具语言字段
     */
    String KEY_CODE_LANG = "LANG";


    /**
     * 业务类型
     */
    enum BusinessType
    {
        /**
         * 注册接入新工具
         */
        REGISTER_PROJECT("RegisterProject"),

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
         * 过滤屏蔽（包括过滤路径屏蔽和规则屏蔽）
         */
        DEFECT_FILTER("DefectFilter"),

        /**
         * 工具侧上报告警数据
         */
        UPLOAD_DEFECT("UploadDefect"),

        /**
         * 忽略告警
         */
        IGNORE_DEFECT("IgnoreDefect"),

        /**
         * 告警处理人分配
         */
        ASSIGN_DEFECT("AssignDefect"),

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
        CHECK_REPORT("CheckerReport");

        private String value;

        BusinessType(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return this.value;
        }
    }

    /**
     * 工具类型
     */
    enum Tool
    {
        COVERITY,
        CPPLINT,
        PYLINT,
        TSCLUA,
        CCN,
        DUPC,
        ESLINT,
        GOML,
        KLOCWORK,
        STYLECOP,
        CHECKSTYLE,
        PHPCS,
        SENSITIVE,
        DETEKT,
        SPOTBUGS
    }

    /**
     * 定制规则根据工具显示顺序
     */
    public enum ToolOrder
    {
        COVERITY,
        KLOCWORK,
        CPPLINT,
        CHECKSTYLE,
        ESLINT,
        STYLECOP,
        PYLINT,
        TSCLUA,
        GOML,
        CCN,
        DUPC;


        //根据名字获取工具排序
        public static int getOrdinalByName(String name)
        {
            if (values() != null)
            {
                for (ToolOrder toolOrder : values())
                {
                    if (toolOrder.name().equalsIgnoreCase(name))
                    {
                        return toolOrder.ordinal();
                    }
                }
            }

            return -1;
        }

    }


    /**
     * 工具当前的状态/当前步骤的状态
     */
    enum StepStatus
    {
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

        StepStatus(int stepStatus)
        {
            this.stepStatus = stepStatus;
        }

        public int value()
        {
            return this.stepStatus;
        }
    }


    /**
     * 项目接入多工具步骤
     */
    enum Step4MutliTool
    {
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
        DEFECT_SUBMI(4),

        /**
         * 分析完成
         */
        COMPLETE(5);

        private int value;

        Step4MutliTool(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return this.value;
        }
    }

    /**
     * 文件类型
     */
    enum FileType
    {
        NEW(1),
        HISTORY(2),
        FIXED(4),
        IGNORE(8);

        private int value;

        FileType(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }

        public String stringValue()
        {
            return String.valueOf(value);
        }
    }


    /**
     * 规则包分类，默认规则包-0；安全规则包-1；内存规则包-2；编译警告包-3；
     * 系统API包-4；性能问题包-5；表达式问题包-6；可疑问题包-7；定制规则包-8；ONESDK规范规则包-9
     * 腾讯开源包-10；
     */
    enum CheckerPkgKind
    {
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
        COMMENT("19");

        private String value;

        CheckerPkgKind(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return value;
        }

        public static String getValueByName(String name)
        {
            if(values() != null)
            {
                for(CheckerPkgKind checkerPkgKind : values())
                {
                    if(checkerPkgKind.name().equalsIgnoreCase(name))
                    {
                        return checkerPkgKind.value;
                    }
                }
            }
            return null;
        }
    }

    /**
     * 任务语言
     */
    enum CodeLang
    {
        C_SHARP(1L, "C#"),
        C_CPP(2L, "C/C++"),
        JAVA(4L, "JAVA"),
        PHP(8L, "PHP"),
        OC(16L, "Objective-C/C++"),
        PYTHON(32L, "PYTHON"),
        JS(64L, "JavaScript"),
        RUBY(128L, "RUBY"),
        LUA(256L, "LUA"),
        GOLANG(512L, "GOLANG"),
        SWIFT(1024L, "SWIFT"),
        TYPESCRIPT(2048L, "TypeScript");

        private Long langValue;

        private String langName;

        CodeLang(Long langValue, String langName)
        {
            this.langValue = langValue;
            this.langName = langName;
        }

        public static String getCodeLang(Long value)
        {
            if (values() != null)
            {
                for (CodeLang lang : values())
                {
                    if (lang.langValue.equals(value))
                    {
                        return lang.langName();
                    }
                }
            }
            return null;
        }

        public Long langValue()
        {
            return langValue;
        }

        public String langName()
        {
            return langName;
        }
    }

    /**
     * 风险系数：极高-SH, 高-H，中-M，低-L
     */
    enum RiskFactor
    {
        SH(1),
        H(2),
        M(4),
        L(8);

        private int value;

        RiskFactor(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }


    /**
     * 区分蓝盾codecc任务创建来源
     */
    enum BsTaskCreateFrom
    {
        /**
         * codecc服务创建的codecc任务
         */
        BS_CODECC("bs_codecc"),

        /**
         * 蓝盾流水线创建的codecc任务
         */
        BS_PIPELINE("bs_pipeline");

        private String value;

        BsTaskCreateFrom(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return this.value;
        }
    }

    enum EslintFrameworkType
    {
        standard, vue, react
    }


    /**
     * 工具跟进状态
     */
    enum FOLLOW_STATUS
    {

        NOT_ACCESS(-1),        //未接入
        NOT_FOLLOW_UP_0(0), //未跟进
        NOT_FOLLOW_UP_1(1), //未跟进
        EXPERIENCE(2),        //体验
        ACCESSING(3),        //接入中
        ACCESSED(4),        //已接入
        HANG_UP(5),            //挂起
        WITHDRAW(6);        //下架

        private int value;

        FOLLOW_STATUS(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }


    /**
     * PHPCS规范编码
     */
    enum PHPCSStandardCode
    {
        PEAR(1),
        Generic(2),
        MySource(4),
        PSR2(8),
        PSR1(16),
        Zend(32),
        PSR12(64),
        Squiz(128);

        private int code;

        PHPCSStandardCode(int code)
        {
            this.code = code;
        }

        public int code()
        {
            return this.code;
        }
    }

    /**
     * 工具处理模式
     */
    enum ToolPattern
    {
        LINT,
        COVERITY,
        KLOCWORK,
        CCN,
        DUPC,
        TSCLUA;
    }



    /**
     * 流水线工具配置操作类型
     */
    enum PipelineToolUpdateType
    {
        ADD,
        REPLACE,
        REMOVE,
        GET
    }

    enum CommonJudge
    {
        COMMON_Y("Y"),
        COMMON_N("N");
        String value;

        CommonJudge(String value)
        {
            this.value = value;
        }

        public String value()
        {
            return this.value;
        }

    }

    /**
     * 任务文件状态
     */
    enum TaskFileStatus
    {
        NEW(1),
        PATH_MASK(8);

        private int value;

        TaskFileStatus(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }


    /**
     * 缺陷类型
     */
    enum DefectType
    {
        NEW(1),
        HISTORY(2);

        private int value;

        DefectType(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }

        public String stringValue()
        {
            return String.valueOf(value);
        }
    }

    /**
     * 缺陷状态
     */
    enum DefectStatus
    {
        NEW(1),
        FIXED(2),
        IGNORE(4),
        PATH_MASK(8),
        CHECKER_MASK(16);

        private int value;

        DefectStatus(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return value;
        }
    }


    /**
     * rdm项目coverity分析状态
     *
     */
    enum RDMCoverityStatus
    {
        success,failed
    }

    /**
     * 代码托管类型，包括SVN、GIT等
     */
    enum CodeHostingType
    {
        SVN,
        GIT,
        HTTP_DOWNLOAD,
        GITHUB;
    }


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
     * ----------------------------end----------------------------
     */


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
    String ZH_CN = "zh-cn";



    /*------------------------------- 规则包描述国际化 -----------------------*/
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


    /*------------------------------- 工具参数提示国际化 -----------------------*/
    /**
     * ESLINT参数名 - eslint_rc
     */
    String PARAM_ESLINT_RC = "eslint_rc";

    /**
     * GOML参数名 - go_path
     */
    String PARAM_GOML_GO_PATH = "go_path";

    /**
     * GOML参数名 - rel_path
     */
    String PARAM_GOML_REL_PATH = "rel_path";

    /**
     * PYLINT参数名 - py_version
     */
    String PARAM_PYLINT_PY_VERSION = "py_version";

    /**
     *  SPOTBUGS参数名 - script_type
     */
    String PARAM_SPOTBUGS_SCRIPT_TYPE = "PROJECT_BUILD_TYPE";

    /**
     *  SPOTBUGS参数名 - SpotBugs脚本
     */
    String PARAM_SPOTBUGS_SCRIPT_CONTENT = "PROJECT_BUILD_COMMAND";

    /**
     *  PHPCS参数名 - phpcs_standard
     */
    String PARAM_PHPCS_XX = "phpcs_standard";

}
