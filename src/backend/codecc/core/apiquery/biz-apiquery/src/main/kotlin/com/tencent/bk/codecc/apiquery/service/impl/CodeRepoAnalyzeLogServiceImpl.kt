package com.tencent.bk.codecc.apiquery.service.impl

import com.tencent.bk.codecc.apiquery.defect.dao.CodeRepoAnalyzeLogDao
import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoFromAnalyzeLogModel
import com.tencent.bk.codecc.apiquery.service.ICodeRepoAnalyzeLogService
import com.tencent.bk.codecc.apiquery.task.TaskQueryReq
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.constant.CommonMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CodeRepoAnalyzeLogServiceImpl @Autowired constructor(
    private val codeRepoAnalyzeLogDao: CodeRepoAnalyzeLogDao
) : ICodeRepoAnalyzeLogService{
    companion object{
        private val logger = LoggerFactory.getLogger(CodeRepoAnalyzeLogServiceImpl::class.java)
    }

    override fun getCodeRepoListByTaskIdList(taskQueryReq: TaskQueryReq): List<CodeRepoFromAnalyzeLogModel>{
        val taskIdList = taskQueryReq.taskIdList
        if(taskIdList.isNullOrEmpty() || taskIdList.size > 500){
            logger.info("task id not qualified")
            throw CodeCCException(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf("task id list"))
        }
        return codeRepoAnalyzeLogDao.getRepoListByTaskIdList(taskIdList)
    }

}
