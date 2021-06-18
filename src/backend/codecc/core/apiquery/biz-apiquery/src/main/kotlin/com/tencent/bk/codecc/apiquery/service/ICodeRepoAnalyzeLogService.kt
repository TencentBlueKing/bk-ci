package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoFromAnalyzeLogModel
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq

interface ICodeRepoAnalyzeLogService {

    /**
     * 根据任务id清单获取代码库信息清单
     */
    fun getCodeRepoListByTaskIdList(taskQueryReq: TaskQueryReq): List<CodeRepoFromAnalyzeLogModel>
}
