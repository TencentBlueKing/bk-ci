package com.tencent.bkrepo.oci

import com.tencent.bkrepo.common.service.condition.MicroService
import org.springframework.boot.runApplication

/**
 * oci registry
 */
@MicroService
class OciRegistryApplication

fun main(args: Array<String>) {
    runApplication<OciRegistryApplication>(*args)
}
