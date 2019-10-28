package com.tencent.devops.process.api.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import com.tencent.devops.process.service.label.PipelineGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppPipelineGroupResourceImpl @Autowired constructor(private val pipelineGroupService: PipelineGroupService) :
    AppPipelineGroupResource {
    override fun getGroups(userId: String, projectId: String): Result<List<PipelineGroup>> {
        return Result(pipelineGroupService.getGroups(userId, projectId))
    }

    override fun addGroup(userId: String, pipelineGroup: PipelineGroupCreate): Result<Boolean> {
        return Result(pipelineGroupService.addGroup(userId, pipelineGroup))
    }

    override fun updateGroup(userId: String, pipelineGroup: PipelineGroupUpdate): Result<Boolean> {
        return Result(pipelineGroupService.updateGroup(userId, pipelineGroup))
    }

    override fun deleteGroup(userId: String, groupId: String): Result<Boolean> {
        return Result(pipelineGroupService.deleteGroup(userId, groupId))
    }

    override fun addLabel(userId: String, pipelineLabel: PipelineLabelCreate): Result<Boolean> {
        return Result(pipelineGroupService.addLabel(userId, pipelineLabel))
    }

    override fun deleteLabel(userId: String, labelId: String): Result<Boolean> {
        return Result(pipelineGroupService.deleteLabel(userId, labelId))
    }

    override fun updateLabel(userId: String, pipelineLabel: PipelineLabelUpdate): Result<Boolean> {
        return Result(pipelineGroupService.updateLabel(userId, pipelineLabel))
    }
}