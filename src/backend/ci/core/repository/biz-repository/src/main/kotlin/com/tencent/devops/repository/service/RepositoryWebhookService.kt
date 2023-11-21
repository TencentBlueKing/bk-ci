package com.tencent.devops.repository.service

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.repository.dao.RepositoryWebhookRequestDao
import com.tencent.devops.repository.pojo.RepositoryWebhookRequest
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("ALL")
class RepositoryWebhookService @Autowired constructor(
    val repositoryWebhookRequestDao: RepositoryWebhookRequestDao,
    val dslContext: DSLContext
) {
    fun saveWebhookRequest(repositoryWebhookRequest: RepositoryWebhookRequest) {
        with(repositoryWebhookRequest) {
            if (externalId.isBlank()) {
                throw ParamBlankException("Invalid eventSource")
            }
            if (repositoryType.isBlank()) {
                throw ParamBlankException("Invalid triggerType")
            }
            if (eventType.isBlank()) {
                throw ParamBlankException("Invalid eventType")
            }
            repositoryWebhookRequestDao.saveWebhookRequest(
                dslContext = dslContext,
                webhookRequest = repositoryWebhookRequest
            )
        }
    }

    fun getWebhookRequest(requestId: String): RepositoryWebhookRequest? {
        return repositoryWebhookRequestDao.get(
            dslContext = dslContext,
            requestId = requestId
        )
    }
}
