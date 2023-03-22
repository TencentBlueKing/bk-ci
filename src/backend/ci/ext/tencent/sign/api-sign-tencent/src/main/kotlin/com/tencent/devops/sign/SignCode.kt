package com.tencent.devops.sign

object SignCode {
    const val BK_IOS_ENTERPRISE_RESIGNATURE = "BkIosEnterpriseResignature" //用户({0})无权限在工程({1})的流水线({2})中发起iOS企业重签名.
    const val BK_SIGNING_TASK_SIGNATURE_INFORMATION = "BkSigningTaskSignatureInformation" //签名任务签名信息(resignId={0})不存在。
    const val BK_SIGNING_TASK_SIGNATURE_HISTORY  = "BkSigningTaskSignatureHistory" //签名任务签名历史(resignId=${0})不存在。
    const val BK_FAILED_CREATE_DOWNLOAD_CONNECTION  = "BkFailedCreateDownloadConnection" //创建下载连接失败(resignId={0})
    const val BK_FAILED_INSERT  = "BkFailedInsert" //插入entitlement文件({0})的keychain-access-groups失败。
    const val BK_DESCRIPTION_FILE_FOR_CERTIFICATE  = "BkDescriptionFileForCertificate" //未找到证书[{0}]对应的描述文件，返回空值
    const val BK_FAILED_PARSE_SIGNATURE_INFORMATION  = "BkFailedParseSignatureInformation" //解析签名信息失败
    const val BK_FAILED_ENCODE_SIGNATURE_INFORMATION  = "BkFailedEncodeSignatureInformation" //编码签名信息失败

}