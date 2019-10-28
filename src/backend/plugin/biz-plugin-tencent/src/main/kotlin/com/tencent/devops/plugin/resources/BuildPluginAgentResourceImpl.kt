// package com.tencent.devops.plugin.resources
//
// import com.tencent.devops.common.api.pojo.Result
// import com.tencent.devops.common.web.RestResource
// import com.tencent.devops.plugin.api.BuildPluginAgentResource
// import com.tencent.devops.plugin.service.PluginTaskService
// import org.springframework.beans.factory.annotation.Autowired
//
// @RestResource
// class BuildPluginAgentResourceImpl @Autowired constructor(
//    private val pluginTaskService: PluginTaskService
// ) : BuildPluginAgentResource {
//    override fun startup(projectId: String, pipelineId: String, buildId: String, vmSeqId: String): Result<Boolean> {
//        return Result(true)
//    }
//
//    override fun end(projectId: String, pipelineId: String, buildId: String, vmSeqId: String): Result<Boolean> {
//        pluginTaskService.end(projectId, pipelineId, buildId, vmSeqId)
//        return Result(true)
//    }
// }