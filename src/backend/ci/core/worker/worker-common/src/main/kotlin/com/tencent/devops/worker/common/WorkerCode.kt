package com.tencent.devops.worker.common

object WorkerCode {
    const val BK_CANNING_SENSITIVE_INFORMATION = "BkCanningSensitiveInformation" //开始敏感信息扫描，待排除目录
    const val BK_SENSITIVE_INFORMATION = "BkSensitiveInformation" //敏感信息扫描报告
    const val BK_NO_SENSITIVE_INFORMATION = "BkNoSensitiveInformation" //无敏感信息，无需生成报告
    const val BK_RELATIVE_PATH_KEYSTORE = "BkRelativePathKeystore" //keystore安装相对路径
    const val BK_KEYSTORE_INSTALLED_SUCCESSFULLY = "BkKeystoreInstalledSuccessfully" //Keystore安装成功
    const val BK_FAILED_UPLOAD_BUGLY_FILE = "BkFailedUploadBuglyFile" //上传bugly文件失败
    const val BK_FAILED_GET_BUILDER_INFORMATION  = "BkFailedGetBuilderInformation" //获取构建机基本信息失败
    const val BK_FAILED_GET_WORKER_BEE = "BkFailedGetWorkerBee" //获取工蜂CI项目Token失败！
    const val BK_FAILED_GET_PLUG = "BkFailedGetPlug" //获取插件执行环境信息失败
    const val BK_FAILED_UPDATE_PLUG = "BkFailedUpdatePlug" //更新插件执行环境信息失败
    const val BK_FAILED_SENSITIVE_INFORMATION = "BkFailedSensitiveInformation" //获取插件敏感信息失败
    const val BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION = "BkFailedEnvironmentVariableInformation" //获取插件开发语言相关的环境变量信息失败
    const val BK_FAILED_ADD_INFORMATION = "BkFailedAddInformation" //添加插件对接平台信息失败
    const val BK_ARCHIVE_PLUG_FILES = "BkArchivePlugFiles" //归档插件文件
    const val BK_FAILED_IOS_CERTIFICATE = "BkFailedIosCertificate" //获取IOS证书失败
    const val BK_FAILED_ANDROID_CERTIFICATE = "BkFailedAndroidCertificate" //获取Android证书失败
    const val BK_ENTERPRISE_SIGNATURE_FAILED = "BkEnterpriseSignatureFailed" //企业签名失败
}