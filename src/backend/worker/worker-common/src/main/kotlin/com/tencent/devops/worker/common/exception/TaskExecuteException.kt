package com.tencent.devops.worker.common.exception

import com.tencent.devops.process.pojo.ErrorType

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 15:09 2019-10-12
 */

class TaskExecuteException(
    val errorType: ErrorType,
    val errorCode: Int,
    val errorMsg: String
) : Throwable(errorMsg)