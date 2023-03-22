package com.tencent.devops.plugin.constant

object PluginCode {
    const val BK_BUILDID_NOT_FOUND = "BkBuildidNotFound" // 服务端内部异常，buildId={0}的构建未查到
    const val BK_PIPELINEID_NOT_FOUND = "BkPipelineidNotFound" // 服务端内部异常，pipelineId={0}的构建未查到

    const val BK_ETH1_NETWORK_CARD_IP_EMPTY = "BkEth1NetworkCardIpEmpty" // eth1 网卡Ip为空，因此，获取eth0的网卡ip
    const val BK_LOOPBACK_ADDRESS_OR_NIC_EMPTY = "BkLoopbackAddressOrNicEmpty" // loopback地址或网卡名称为空
    const val BK_FAILED_GET_NETWORK_CARD = "BkFailedGetNetworkCard" // 获取网卡失败
    const val BK_WETEST_FAILED_GET = "BkWetestFailedGet" // WeTest获取secretId,secretKey失败，返回码: {0}, 错误消息: {1}
    const val BK_GET_SIGNATURE_ERROR = "BkGetSignatureError" // 获取Signature错误，err:{0}
    const val BK_URL_CODING_ERROR = "BkUrlCodingError" // url编码错误, err:{0}

}