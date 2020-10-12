package com.tencent.devops.process.service

import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import org.springframework.stereotype.Service

@Service
class PipelineBuildExtServiceImpl : PipelineBuildExtService {
	override fun buildExt(task: PipelineBuildTask): Map<String, String> {
		return emptyMap()
	}
}