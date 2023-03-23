package com.tencent.devops.prebuild
/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表蓝盾平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-蓝盾环境 06：experience-版本体验 07：image-镜像 08：log-蓝盾日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-蓝盾支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22：sign-签名服务  23：metrics 24：external-外部 25：prebuild-预建 26：stream）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 * @since: 2019-03-05
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object PreBuildMessageCode {
    const val POOL_PARAMETER_CANNOT_EMPTY = "2125001" // 当 resourceType = REMOTE, pool参数不能为空
    const val CURRENT_PROJECT_NOT_INITIALIZED = "2125002" // 当前工程未初始化，请初始化工程，工程名： {0}
    const val USER_NOT_PERMISSION_OPERATE = "2125003" // 用户{0}没有操作权限
    const val TYPE_ALREADY_EXISTS_CANNOT_ADD = "2125004" // 已存在当前插件类型的版本信息，无法新增
    const val CHECK_YML_CONFIGURATION = "2125005" // run命令不支持在agentless下执行，请检查yml配置.
    const val PUBLIC_BUILD_RESOURCE_POOL_NOT_EXIST= "2125006" // 公共构建资源池不存在，请检查yml配置.
    const val PIPELINE_NAME_CREATOR_CANNOT_EMPTY = "2125007" // 流水线名称、创建人均不能为空
    const val PIPELINE_MUST_AT_LEAST_ONE = "2125008" // 流水线Stages至少为1个
    const val ALPHABET_NUMBER_UNDERSCORE = "2125009" // 变量名称必须是英文字母、数字或下划线(_)
    const val STAGES_JOBS_STEPS = "2125010" // stages, jobs, steps, extends 必须存在一个
    const val PRECI_SUPPORTS_REMOTE_TEMPLATES = "2125011" // PreCI仅支持远程模板
    const val REMOTE_WAREHOUSE_KEYWORD_CANNOT_EMPTY = "2125012" // 远程仓关键字不能为空: repository, name
    const val CODE_CHECKOUT_NOT_SUPPORTED = "2125013" // 不支持checkout关键字进行代码检出
    const val SERVICES_KEYWORD_NOT_SUPPORTED= "2125014" // 不支持services关键字

}