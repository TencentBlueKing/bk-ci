package com.tencent.devops.common.web.config

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.tencent.devops.common.web.jasypt.DefaultEncryptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("MaxLineLength")
@Configuration
class WebAutoConfiguration : WebMvcConfigurer {

    @Bean("jasyptStringEncryptor")
    @Primary
    fun stringEncryptor(@Value("\${enc.key:rAFOey00bcuMNMrt}") key: String) = DefaultEncryptor(key)

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.forEach {
            if (it is MappingJackson2HttpMessageConverter) {
                val simpleModule = SimpleModule()
                simpleModule.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                simpleModule.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                it.objectMapper.registerModule(simpleModule)
            }
        }
        super.configureMessageConverters(converters)
    }
}
