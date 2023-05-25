package com.tencent.devops.common.kubernetes.client

import io.kubernetes.client.informer.SharedInformer
import io.kubernetes.client.informer.SharedInformerFactory
import io.kubernetes.client.informer.cache.Lister
import io.kubernetes.client.openapi.models.V1Endpoints
import io.kubernetes.client.openapi.models.V1Service
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.kubernetes.client.discovery.KubernetesInformerDiscoveryClient
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesServiceInstance
import org.springframework.util.StringUtils

@SuppressWarnings("LongParameterList", "ReturnCount")
class BkKubernetesDiscoveryClient constructor(
    namespace: String,
    sharedInformerFactory: SharedInformerFactory,
    private val serviceLister: Lister<V1Service>,
    private val endpointsLister: Lister<V1Endpoints>,
    serviceInformer: SharedInformer<V1Service>,
    endpointsInformer: SharedInformer<V1Endpoints>,
    properties: KubernetesDiscoveryProperties
) : KubernetesInformerDiscoveryClient(
    namespace,
    sharedInformerFactory,
    serviceLister,
    endpointsLister,
    serviceInformer,
    endpointsInformer,
    properties
) {
    override fun getInstances(serviceId: String?): List<ServiceInstance> {
        if (!StringUtils.hasText(serviceId)) {
            logger.error("ServiceId is null or empty , please check the serviceId")
            return emptyList()
        }
        if (!serviceId!!.contains(".")) {
            logger.error("ServiceId must contain '.' , the kubernetes svc must {serviceName}.{namespace}")
            return emptyList()
        }
        val serviceData = serviceId.split('.')
        val serviceName = serviceData[0]
        val namespace = serviceData[1]
        val service = serviceLister.namespace(namespace).get(serviceName)
        if (service == null) {
            logger.debug("Can not find service , service id : $serviceId")
            return emptyList()
        }
        val svcMetadata = mutableMapOf<String, String>()
        service.metadata?.labels?.let { svcMetadata.putAll(it) }
        service.metadata?.annotations?.let { svcMetadata.putAll(it) }

        val ep = endpointsLister.namespace(service.metadata!!.namespace)[service.metadata!!.name]
        if (ep == null || ep.subsets == null) {
            logger.debug("Can not find endpoint , service id : $serviceId")
            return emptyList()
        }
        return ep.subsets!!.filterNot { it.ports.isNullOrEmpty() }.flatMap {
            val endpointPort = it.ports?.get(0)?.port ?: 80
            val addresses = it.addresses ?: emptyList()
            addresses.map { addr ->
                KubernetesServiceInstance(
                    addr.targetRef?.uid ?: "",
                    serviceName,
                    addr.ip,
                    endpointPort,
                    svcMetadata,
                    false
                )
            }
        }.toList()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkKubernetesDiscoveryClient::class.java)
    }
}
