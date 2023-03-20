package com.tencent.devops.common.client

import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.ClassUtils
import javax.ws.rs.Path

class JerseyFeignRequestRegistrar(
    private var resourceLoader: ResourceLoader,
    private var environment: Environment
) : ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    companion object {
        const val DEVOPS_BASE_PACKAGE = "com.tencent.devops"
    }

    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    override fun registerBeanDefinitions(importingClassMetadata: AnnotationMetadata, registry: BeanDefinitionRegistry) {
        // 定义class扫描器
        val scanner = getScanner()
        // 指定只扫描标注了Path的接口
        scanner.addIncludeFilter(AnnotationTypeFilter(Path::class.java))
        // 获取devops包下符合条件的类的定义
        val candidateComponents = scanner.findCandidateComponents(DEVOPS_BASE_PACKAGE)
        for (candicateComponent in candidateComponents) {
            if (candicateComponent is AnnotatedBeanDefinition) {
                val annotationMetadata = candicateComponent.metadata
                if (annotationMetadata.isInterface) {
                    registerDevopsClient(registry, annotationMetadata)
                }
            }
        }
    }

    private fun registerDevopsClient(registry: BeanDefinitionRegistry, annotationMetadata: AnnotationMetadata) {
        val className = annotationMetadata.className
        val clazz = ClassUtils.resolveClassName(className, null)
        val tag = environment.getProperty("spring.cloud.consul.discovery.tags", "turbo")
        val definition = BeanDefinitionBuilder.genericBeanDefinition(JerseyFeignClientFactoryBean::class.java)
        definition.addConstructorArgValue(clazz)
        definition.addConstructorArgValue(tag)
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
        definition.setLazyInit(true)
        val beanDefinition = definition.beanDefinition
        beanDefinition.setAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE, className)
        val holder = BeanDefinitionHolder(beanDefinition, className, null)
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry)
    }

    private fun getScanner(): ClassPathScanningCandidateComponentProvider {
        return object : ClassPathScanningCandidateComponentProvider(false, this.environment) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return beanDefinition.metadata.isInterface && beanDefinition.metadata.isIndependent &&
                        !beanDefinition.metadata.isAnnotation
            }
        }
    }
}
