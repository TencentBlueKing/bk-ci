package com.tencent.devops.common.web.handler

import com.fasterxml.jackson.databind.JsonMappingException
import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_GENERAL_SYSTEM_FAIL
import com.tencent.devops.common.api.exception.code.TURBO_PARAM_INVALID
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import org.springframework.http.HttpStatus
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionControllerAdvice {

    @ResponseBody
    @ExceptionHandler(ClientException::class)
    fun clientExceptionHandler(clientException: ClientException): Response<Void> {
        return Response.fail(TURBO_THIRDPARTY_SYSTEM_FAIL.toInt(), "内部服务异常")
    }

    @ResponseBody
    @ExceptionHandler(OperationException::class)
    fun operationExceptionHandler(operationException: OperationException): Response<Void> {
        return Response.fail(TURBO_GENERAL_SYSTEM_FAIL.toInt(), operationException.message!!)
    }

    @ResponseBody
    @ExceptionHandler(RemoteServiceException::class)
    fun remoteServiceExceptionHandler(remoteServiceException: RemoteServiceException): Response<Void> {
        return Response.fail(TURBO_THIRDPARTY_SYSTEM_FAIL.toInt(), remoteServiceException.errorMessage)
    }

    @ResponseBody
    @ExceptionHandler(TurboException::class)
    fun turboExceptionHandler(turboException: TurboException): Response<Void> {
        return Response.fail(turboException.errorCode.toInt(), turboException.message.orEmpty())
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validateExceptionHandler(methodArgumentNotValidException: MethodArgumentNotValidException): Response<Void> {
        return Response.fail(
            TURBO_PARAM_INVALID.toInt(),
            methodArgumentNotValidException.bindingResult.fieldErrors
                .fold("") { acc, fieldError -> acc.plus("${fieldError.defaultMessage},") }.trimEnd(',')
        )
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(BindException::class)
    fun validateExceptionHandler(bindException: BindException): Response<Void> {
        return Response.fail(
            TURBO_PARAM_INVALID.toInt(),
            bindException.bindingResult.fieldErrors
                .fold("") { acc, fieldError -> acc.plus("${fieldError.defaultMessage},") }.trimEnd(',')
        )
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    @ResponseBody
    @ExceptionHandler(JsonMappingException::class)
    fun jsonMappingExceptionHandler(jsonMappingException: JsonMappingException): Response<Void> {
        return Response.fail(
            TURBO_PARAM_INVALID.toInt(),
            "请求参数错误"
        )
    }
}
