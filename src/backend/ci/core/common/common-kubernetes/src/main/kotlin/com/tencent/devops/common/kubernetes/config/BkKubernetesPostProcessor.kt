package com.tencent.devops.common.kubernetes.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties
import org.springframework.core.Ordered

class BkKubernetesPostProcessor(
    private val discoveryProperties: KubernetesDiscoveryProperties
) : BeanFactoryPostProcessor, Ordered {
    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        logger.debug("postProcessBeanFactory , set isAllNamespaces to true")
        discoveryProperties.isAllNamespaces = true
    }

    override fun getOrder(): Int {
        return -1
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkKubernetesPostProcessor::class.java)
    }
}
