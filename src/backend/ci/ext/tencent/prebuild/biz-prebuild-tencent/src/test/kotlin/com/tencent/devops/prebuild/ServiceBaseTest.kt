package com.tencent.devops.prebuild

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.StartUpReq
import org.mockito.ArgumentMatchers

open class ServiceBaseTest {
    protected val YAML_CONTENT = "i am yaml"
    protected val PIPELINE_ID = "p_123456"
    protected val BUILD_ID = "b_123456"
    protected val channelCode = ChannelCode.BS
    protected val userId = "userA"
    private val hostname = "${userId}_hostname"
    private val ip = "x.x.x.x"
    protected val startUpReq = StartUpReq(
        workspace = "/data/landun/workspace/$userId",
        yaml = YAML_CONTENT,
        os = OS.LINUX,
        ip = ip,
        hostname = hostname,
        extraParam = null
    )
    protected val agentInfo = ThirdPartyAgentStaticInfo(
        agentId = "${userId}_agent_id",
        os = "",
        projectId = ArgumentMatchers.anyString(),
        script = ArgumentMatchers.anyString(),
        secretKey = ArgumentMatchers.anyString(),
        status = ArgumentMatchers.anyInt(),
        createdUser = userId,
        hostName = "${userId}_hostname",
        gateway = ArgumentMatchers.anyString(),
        link = ArgumentMatchers.anyString(),
        ip = ip
    )
}