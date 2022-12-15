package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.dao.label.PipelineViewDao
import com.tencent.devops.process.dao.label.PipelineViewGroupDao
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpPipelineViewResourceImpl @Autowired constructor(
    private val pipelineViewDao: PipelineViewDao,
    private val pipelineViewGroupDao: PipelineViewGroupDao
) : OpPipelineViewResource{
    override fun getViewSettings(userId: String): Result<Boolean> {
        TODO("Not yet implemented")
    }
}
