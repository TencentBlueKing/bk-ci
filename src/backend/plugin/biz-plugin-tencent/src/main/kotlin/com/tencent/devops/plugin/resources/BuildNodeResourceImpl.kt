package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.plugin.api.BuildNodeResource
import com.tencent.devops.plugin.service.JobService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @Description 为插件【作业平台-执行脚本】提供build接口
 * @author Jsonwan
 * @Date 2019/8/15
 * @version 1.0
 */
@RestResource
class BuildNodeResourceImpl @Autowired constructor(
    val jobService: JobService
) : BuildNodeResource {
    override fun listRawByHashIds(projectId: String, pipelineId: String, buildId: String, nodeHashIds: List<String>): Result<List<NodeBaseInfo>> {
        return jobService.listRawNodesByHashIds(projectId, buildId, nodeHashIds)
    }
}