package com.tencent.devops.process.listener

import com.tencent.devops.common.pipeline.pojo.PipelineBuildBaseInfo
import org.jooq.DSLContext

/**
 * @Description
 * @Date 2020/2/27
 * @Version 1.0
 */
interface PipelineHardDeleteListener {
    /**
     * 流水线被硬删除时的回调函数
     * @param pipelineBuildBaseInfoList 流水线及其构建信息
     * @return 当前删除任务是否成功
     */
    fun onPipelineDeleteHardly(dslContext: DSLContext, operator: String, pipelineBuildBaseInfoList: List<PipelineBuildBaseInfo>): Boolean
}
