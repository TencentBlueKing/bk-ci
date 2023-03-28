package com.tencent.devops.stream.constant
/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21：auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26：stream 27：worker 28：lambda）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object StreamMessageCode {
    const val PROJECT_ALREADY_EXISTS = "2126001" //项目已存在
    const val PROJECT_NOT_EXIST = "2126002" //项目不存
    const val NOT_AUTHORIZED_BY_OAUTH = "2126003" //用户[{0}]尚未进行OAUTH授权，请先授权。
    const val PROJECT_STREAM_NOT_ENABLED = "2126004" //工蜂项目{0}未开启Stream
    const val NO_RECORD_MIRROR_VERSION = "2126004" //没有此镜像版本记录
    const val MIRROR_VERSION_NOT_AVAILABLE = "2126005" //镜像版本不可用
    const val VARIABLE_NAME = "2126006" //变量名称必须是英文字母、数字或下划线(_)
    const val MUST_HAVE_ONE = "2126007" //stages, jobs, steps, extends 必须存在一个
    const val STARTUP_CONFIGURATION_MISSING = "2126008" //启动配置缺少 rtx.v2GitUrl
    const val GIT_CI_NO_RECOR = "2126009" //Git CI没有此镜像版本记录
    const val PROJECT_CANNOT_OPEN_STREAM = "2126010" //项目无法开启Stream，请联系蓝盾助手
    const val PROJECT_CANNOT_QUERIED = "2126011" //项目未开启Stream，无法查询
    const val PIPELINE_NOT_EXIST_OR_DELETED = "2126012" //流水线不存在或已删除，如有疑问请联系蓝盾助手
    const val BUILD_TASK_NOT_EXIST = "2126013" //构建任务不存在，无法重试
    const val USER_NOT_PERMISSION_FOR_WORKER_BEE = "2126014" //用户没有工蜂项目权限，无法获取下载链接
    const val INCORRECT_ID_BLUE_SHIELD_PROJECT = "2126015" //蓝盾项目ID不正确


}