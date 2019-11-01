package com.tencent.devops.common.pipeline.type.devcloud

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.DispatchType

/**
 * class PCGDockerImage(
 *   val img_ver: String,
 *   val os: String,
 *   val img_name: String,
 *   val language: String
 * )
 * image:
 * img_name:img_ver:os:language
 *
 * ie.
 * {
 *   "img_ver":"3.2.2.3.rc",
 *   "os":"tlinux",
 *   "img_name":"tc/tlinux/qbgdev",
 *   "language":"C++",
 * }
 * image:
 * tc/tlinux/qbgdev:3.2.2.3.rc:tlinux:C++
 */
data class PublicDevCloudDispathcType(@JsonProperty("value") var image: String)
    : DispatchType(
    image,
    DispatchRouteKeySuffix.DEVCLOUD
) {
    override fun replaceField(variables: Map<String, String>) {
        image = EnvUtils.parseEnv(image, variables)
    }
}