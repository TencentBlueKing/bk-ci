package com.tencent.bk.codecc.task.filter

import com.tencent.bk.codecc.task.dao.mongorepository.UserLogInfoRepository
import com.tencent.bk.codecc.task.model.UserLogInfoEntity
import com.tencent.bk.codecc.task.model.UserLogInfoStatEntity
import com.tencent.devops.common.api.annotation.UserLogin
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.web.mq.EXCHANGE_USER_LOG_INFO_STAT
import com.tencent.devops.common.web.mq.ROUTE_USER_LOG_INFO_STAT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter

@UserLogin
class UserLogInfoFilter @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val userLogInfoRepository: UserLogInfoRepository
) : ContainerRequestFilter {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(UserLogInfoFilter::class.java)
    }

    override fun filter(requestContext: ContainerRequestContext) {
        logger.info("user login record filter!")
        val user = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_USER_ID)
        if (user.isNullOrBlank()) {
            logger.info("user id is blank!")
            return
        }
        val userLogInfoEntity = UserLogInfoEntity()
        userLogInfoEntity.userName = user
        userLogInfoEntity.url = requestContext.uriInfo.absolutePath.path
        userLogInfoEntity.loginDate = LocalDate.now()
        userLogInfoEntity.loginTime = LocalDateTime.now()
        userLogInfoRepository.save(userLogInfoEntity)

        val statEntity = UserLogInfoStatEntity()
        statEntity.userName = user
        statEntity.firstLogin = System.currentTimeMillis()
        rabbitTemplate.convertAndSend(EXCHANGE_USER_LOG_INFO_STAT, ROUTE_USER_LOG_INFO_STAT, statEntity)
    }
}