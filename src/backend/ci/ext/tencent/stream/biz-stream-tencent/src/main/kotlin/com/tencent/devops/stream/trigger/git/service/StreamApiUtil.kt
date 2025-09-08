package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import org.slf4j.Logger

object StreamApiUtil {
    fun <T> doRetryFun(
        logger: Logger,
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        return if (retry.retry) {
            retryFun(
                retry = retry,
                logger = logger,
                log = log,
                apiErrorCode = apiErrorCode
            ) {
                action()
            }
        } else {
            action()
        }
    }

    private fun <T> retryFun(
        retry: ApiRequestRetryInfo,
        logger: Logger,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        try {
            return RetryUtils.clientRetry(
                retry.retryTimes,
                retry.retryPeriodMills
            ) {
                action()
            }
        } catch (e: ClientException) {
            logger.warn("TGitApiService|retryFun|retry 5 times $log", e)
            throw ErrorCodeException(errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString())
        } catch (e: RemoteServiceException) {
            logger.warn("TGitApiService|retryFun|GIT_API_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("TGitApiService|retryFun|GIT_SCM_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.message}"
            )
        } catch (e: Throwable) {
            logger.error("TGitApiService|retryFun|retryFun error $log", e)
            throw ErrorCodeException(
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = if (e.message.isNullOrBlank()) {
                    "$log: ${apiErrorCode.getErrorMessage()}"
                } else {
                    "$log: ${e.message}"
                }
            )
        }
    }
}
