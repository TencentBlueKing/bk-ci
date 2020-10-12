package com.tencent.devops.process.engine.service

import com.tencent.devops.process.engine.pojo.PipelineBuildTask

interface PipelineBuildExtService {

	fun buildExt(task: PipelineBuildTask) : Map<String, String>
}