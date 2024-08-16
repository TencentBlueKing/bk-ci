package com.tencent.devops.monitoring.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.kafka.KafkaClient
import com.tencent.devops.common.kafka.KafkaTopic.BUILD_ATOM_METRICS_TOPIC_PREFIX
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.monitoring.api.service.BuildAtomMetricsResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildAtomMetricsResourceImpl @Autowired constructor(
    private val kafkaClient: KafkaClient
) : BuildAtomMetricsResource {

    override fun reportAtomMetrics(atomCode: String, data: String): Result<Boolean> {
        kafkaClient.send(topic = "$BUILD_ATOM_METRICS_TOPIC_PREFIX-$atomCode", msg = data)
        return Result(true)
    }
}
