package com.tencent.bk.codecc.quartz.resources

import com.tencent.bk.codecc.quartz.api.UserJobRestResource
import com.tencent.bk.codecc.quartz.core.CustomSchedulerManager
import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.NodeInfoVO
import com.tencent.bk.codecc.quartz.pojo.ShardInfoVO
import com.tencent.bk.codecc.quartz.pojo.ShardingResultVO
import com.tencent.bk.codecc.quartz.service.JobManageService
import com.tencent.devops.common.api.pojo.CodeCCResult
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserJobRestResourceImpl @Autowired constructor(
    private val customSchedulerManager: CustomSchedulerManager,
    private val jobManageService: JobManageService
) : UserJobRestResource {

    override fun getExistingJob(): CodeCCResult<List<JobInfoVO>> {
        return CodeCCResult(customSchedulerManager.getExistingJob())
    }


    override fun deleteAllJobs(dataDelete : Int) : CodeCCResult<Boolean> {
        customSchedulerManager.deleteAllJobs(dataDelete)
        return CodeCCResult(true)
    }


    override fun initAllJobs() : CodeCCResult<Boolean> {
        customSchedulerManager.initAllJobs()
        return CodeCCResult(true)
    }


    override fun refreshOpenSourceCronExpression(period : Int, startTime : Int) : CodeCCResult<Boolean> {
        jobManageService.refreshOpensourceCronExpression(period, startTime)
        return CodeCCResult(true)
    }


    override fun getShardingResult(): CodeCCResult<ShardingResultVO?> {
        val shardingResult =
            CustomSchedulerManager.shardingStrategy.getShardingStrategy().getShardingResult() ?: return CodeCCResult(null)
        return with(shardingResult) {
            val shardingResultVO = ShardingResultVO(
                currentShard = ShardInfoVO(
                    shardNum = currentShard.shardNum,
                    tag = currentShard.tag,
                    nodeList = currentShard.nodeList.map {
                        NodeInfoVO(
                            nodeNum = it.nodeNum,
                            serviceId = it.serviceId,
                            host = it.host,
                            port = it.port
                        )
                    }
                ),
                currentShardCount = currentShardCount,
                currentNode = NodeInfoVO(
                    nodeNum = currentNode.nodeNum,
                    serviceId = currentNode.serviceId,
                    host = currentNode.host,
                    port = currentNode.port
                ),
                currentNodeCount = currentNodeCount,
                shardList = shardList.map {
                    ShardInfoVO(
                        shardNum = it.shardNum,
                        tag = it.tag,
                        nodeList = it.nodeList.map { nodeInfo ->
                            NodeInfoVO(
                                nodeNum = nodeInfo.nodeNum,
                                serviceId = nodeInfo.serviceId,
                                host = nodeInfo.host,
                                port = nodeInfo.port
                            )
                        }
                    )
                }
            )
            CodeCCResult(shardingResultVO)
        }
    }
}