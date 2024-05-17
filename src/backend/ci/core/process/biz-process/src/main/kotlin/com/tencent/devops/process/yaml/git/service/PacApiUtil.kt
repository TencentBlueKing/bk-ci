package com.tencent.devops.process.yaml.git.service

import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.service.utils.RetryUtils
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.git.pojo.ApiRequestRetryInfo
import org.slf4j.Logger

object PacApiUtil {
    fun <T> doRetryFun(
        logger: Logger,
        retry: ApiRequestRetryInfo,
        log: String,
        errorCode: String,
        action: () -> T
    ): T {
        return if (retry.retry) {
            retryFun(
                retry = retry,
                logger = logger,
                log = log,
                errorCode = errorCode
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
        errorCode: String,
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
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_DEVNET_TIMEOUT)
        } catch (e: RemoteServiceException) {
            logger.warn("TGitApiService|retryFun|GIT_API_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = errorCode,
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("TGitApiService|retryFun|GIT_SCM_ERROR $log", e)
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = errorCode,
                defaultMessage = "$log: ${e.message}"
            )
        } catch (e: Throwable) {
            logger.error("TGitApiService|retryFun|retryFun error $log", e)
            throw ErrorCodeException(
                errorCode = errorCode,
                defaultMessage = if (e.message.isNullOrBlank()) {
                    "$log: $errorCode"
                } else {
                    "$log: ${e.message}"
                }
            )
        }
    }
}
