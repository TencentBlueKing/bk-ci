package com.tencent.devops.prebuild

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.StartUpReq

open class ServiceBaseTest {
    protected val YAML_CONTENT = "i am yaml"
    protected val PIPELINE_ID = "p_123456"
    protected val BUILD_ID = "b_123456"
    protected val channelCode = ChannelCode.BS
    protected val userId = "userA"
    protected val pipelineName = "this is pipeline name"
    protected val description = "this is pipeline description"

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
        os = "LINUX",
        projectId = "ArgumentMatchers.anyString()",
        script = "ArgumentMatchers.anyString()",
        secretKey = "ArgumentMatchers.anyString()",
        status = 0,
        createdUser = userId,
        hostName = "${userId}_hostname",
        gateway = "ArgumentMatchers.anyString()",
        link = "ArgumentMatchers.anyString()",
        ip = ip
    )

    /**
     * docker vm
     */
    protected fun getYamlForDockerVM(): String {
        return ServiceBaseTest::class.java.getResource("/docker_vm.yml").readText(Charsets.UTF_8)
    }

    /**
     *  devcloud构建机
     */
    protected fun getYamlForDevCloud(): String {
        return ServiceBaseTest::class.java.getResource("/docker_devcloud.yml").readText(Charsets.UTF_8)
    }

    /**
     * 本地构建机
     */
    protected fun getYamlForLocal(): String {
        return ServiceBaseTest::class.java.getResource("/local.yml").readText(Charsets.UTF_8)
    }

    /**
     * 无编译环境
     */
    protected fun getYamlForAgentLess(): String {
        return ServiceBaseTest::class.java.getResource("/agentless.yml").readText(Charsets.UTF_8)
    }

    protected fun getYamlForInvalidDispatchType(): String {
        return ServiceBaseTest::class.java.getResource("/error_dispatch.yml").readText(Charsets.UTF_8)
    }

    /**
     * 存在extends以及其他顶级关键字
     */
    protected fun getYamlForCheckEntendsBiz(): String {
        return ServiceBaseTest::class.java.getResource("/entends_with_stages_finally.yml").readText(Charsets.UTF_8)
    }
}