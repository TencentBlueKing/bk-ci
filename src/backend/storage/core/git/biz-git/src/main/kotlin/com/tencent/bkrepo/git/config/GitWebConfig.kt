package com.tencent.bkrepo.git.config

import com.tencent.bkrepo.git.artifact.GitRepoInterceptor
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(GitProperties::class)
class GitWebConfig : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(clientAuthInterceptor())
            .addPathPatterns("/**")
            .order(0)
        super.addInterceptors(registry)
    }

    @Bean
    fun clientAuthInterceptor() = GitRepoInterceptor()
}
