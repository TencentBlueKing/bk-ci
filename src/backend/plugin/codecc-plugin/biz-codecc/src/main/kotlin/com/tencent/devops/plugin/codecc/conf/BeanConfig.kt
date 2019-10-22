package com.tencent.devops.plugin.codecc.conf

import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.permission.impl.PipelinePermissionServiceImpl
import org.jooq.DSLContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@ConditionalOnWebApplication
class BeanConfig {

    @Bean
    @Primary
    fun pipelinePermissionService(dslContext: DSLContext,
                                  authProjectApi: AuthProjectApi,
                                  authResourceApi: AuthResourceApi,
                                  authPermissionApi: AuthPermissionApi,
                                  pipelineAuthServiceCode: PipelineAuthServiceCode) =
            PipelinePermissionServiceImpl(dslContext, PipelineInfoDao(), authProjectApi, authResourceApi, authPermissionApi, pipelineAuthServiceCode)
}