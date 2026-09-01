package com.tencent.devops.ai.config

import com.tencent.devops.ai.dao.AiAgentStateDao
import com.tencent.devops.ai.session.AiMysqlSession
import io.agentscope.core.session.Session
import org.jooq.DSLContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 会话存储配置类，创建 AiMysqlSession Bean。 */
@Configuration
class AiSessionConfig {

    @Bean
    fun aiSession(
        dslContext: DSLContext,
        agentStateDao: AiAgentStateDao
    ): Session {
        return AiMysqlSession(dslContext, agentStateDao)
    }
}
