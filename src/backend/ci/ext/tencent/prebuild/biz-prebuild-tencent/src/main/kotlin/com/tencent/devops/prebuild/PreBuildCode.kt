package com.tencent.devops.prebuild

object PreBuildCode {
    const val BK_AGENT_NOT_INSTALLED = "BkAgentNotInstalled" // Agent未安装，请安装Agent.
    const val BK_ILLEGAL_YAML = "BkIllegalYaml" // YAML非法:{0}
    const val BK_MANUAL_TRIGGER = "BkManualTrigger" // 手动触发
    const val BK_BUILD_TRIGGER = "BkBuildTrigger" // 构建触发
    const val BK_NO_COMPILATION_ENVIRONMENT = "BkNoCompilationEnvironment" // 无编译环境
    const val BK_POOL_PARAMETER_CANNOT_EMPTY = "BkPoolParameterCannotEmpty" // 当 resourceType = REMOTE, pool参数不能为空
    const val BK_CURRENT_PROJECT_NOT_INITIALIZED = "BkCurrentProjectNotInitialized" // 当前工程未初始化，请初始化工程，工程名： {0}
    const val BK_USER_NOT_PERMISSION_OPERATE = "BkUserNotPermissionOperate" // 用户{0}没有操作权限
    const val BK_TYPE_ALREADY_EXISTS_CANNOT_ADD = "BkTypeAlreadyExistsCannotAdd" // 已存在当前插件类型的版本信息，无法新增
    const val BK_TBUILD_ENVIRONMENT_LINUX = "BkTbuildEnvironmentLinux" // 构建环境-LINUX
    const val BK_CHECK_YML_CONFIGURATION = "BkCheckYmlConfiguration" // run命令不支持在agentless下执行，请检查yml配置.
    const val BK_SYNCHRONIZE_LOCAL_CODE = "BkSynchronizeLocalCode" // 同步本地代码
    const val BK_PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST= "BkPublicBuildResourcePoolNotExist" // 公共构建资源池不存在，请检查yml配置.
    const val BK_PIPELINE_NAME_CREATOR_CANNOT_EMPTY = "BkPipelineNameCreatorCannotEmpty" // 流水线名称、创建人均不能为空
    const val BK_PIPELINE_MUST_AT_LEAST_ONE = "BkPipelineMustAtLeastOne" // 流水线Stages至少为1个
    const val BK_ALPHABET_NUMBER_UNDERSCORE = "BkAlphabetNumberUnderscore" // 变量名称必须是英文字母、数字或下划线(_)
    const val BK_STAGES_JOBS_STEPS = "BkStagesJobsSteps" // stages, jobs, steps, extends 必须存在一个
    const val BK_PRECI_SUPPORTS_REMOTE_TEMPLATES = "BkPreciSupportsRemoteTemplates" // PreCI仅支持远程模板
    const val BK_REMOTE_WAREHOUSE_KEYWORD_CANNOT_EMPTY = "BkRemoteWarehouseKeywordCannotEmpty" // 远程仓关键字不能为空: repository, name
    const val BK_CODE_CHECKOUT_NOT_SUPPORTED = "BkCodeCheckoutNotSupported" // 不支持checkout关键字进行代码检出
    const val BK_SERVICES_KEYWORD_NOT_SUPPORTED= "BkServicesKeywordNotSupported" // 不支持checkout关键字进行代码检出

}