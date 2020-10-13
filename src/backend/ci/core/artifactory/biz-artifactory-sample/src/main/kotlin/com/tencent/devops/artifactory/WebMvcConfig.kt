package com.tencent.devops.artifactory

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@Configuration
class WebMvcConfig : WebMvcConfigurerAdapter() {

    @Value("\${artifactory.archiveLocalBasePath:/data/bkce/public/ci/artifactory}")
    private lateinit var archiveLocalBasePath: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 把自定义插件UI文件目录映射成服务器静态资源
        val bkPluginFeDir = "$archiveLocalBasePath/${FileTypeEnum.BK_PLUGIN_FE.fileType}/"
        registry.addResourceHandler("/resource/**").addResourceLocations("file:$bkPluginFeDir")
    }
}
