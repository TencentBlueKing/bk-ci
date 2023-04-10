package com.tencent.devops.worker.common

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object WorkerMessageCode {
    const val FOLDER_NOT_EXIST = "2130409" //文件夹{0}不存在
    const val CERTIFICATE_ID_EMPTY = "2130012" //证书ID为空

    const val BK_CANNING_SENSITIVE_INFORMATION = "bkCanningSensitiveInformation" //开始敏感信息扫描，待排除目录
    const val BK_SENSITIVE_INFORMATION = "bkSensitiveInformation" //敏感信息扫描报告
    const val BK_NO_SENSITIVE_INFORMATION = "bkNoSensitiveInformation" //无敏感信息，无需生成报告
    const val BK_RELATIVE_PATH_KEYSTORE = "bkRelativePathKeystore" //keystore安装相对路径
    const val BK_KEYSTORE_INSTALLED_SUCCESSFULLY = "bkKeystoreInstalledSuccessfully" //Keystore安装成功
    const val BK_FAILED_UPLOAD_BUGLY_FILE = "bkFailedUploadBuglyFile" //上传bugly文件失败
    const val BK_FAILED_GET_BUILDER_INFORMATION = "bkFailedGetBuilderInformation" //获取构建机基本信息失败
    const val BK_FAILED_GET_WORKER_BEE = "bkFailedGetWorkerBee" //获取工蜂CI项目Token失败！
    const val BK_FAILED_GET_PLUG = "bkFailedGetPlug" //获取插件执行环境信息失败
    const val BK_FAILED_UPDATE_PLUG = "bkFailedUpdatePlug" //更新插件执行环境信息失败
    const val BK_FAILED_SENSITIVE_INFORMATION = "bkFailedSensitiveInformation" //获取插件敏感信息失败
    const val BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION = "bkFailedEnvironmentVariableInformation" //获取插件开发语言相关的环境变量信息失败
    const val BK_FAILED_ADD_INFORMATION = "bkFailedAddInformation" //添加插件对接平台信息失败
    const val BK_ARCHIVE_PLUG_FILES = "bkArchivePlugFiles" //归档插件文件
    const val BK_FAILED_IOS_CERTIFICATE = "bkFailedIosCertificate" //获取IOS证书失败
    const val BK_FAILED_ANDROID_CERTIFICATE = "bkFailedAndroidCertificate" //获取Android证书失败
    const val BK_ENTERPRISE_SIGNATURE_FAILED = "bkEnterpriseSignatureFailed" //企业签名失败
}