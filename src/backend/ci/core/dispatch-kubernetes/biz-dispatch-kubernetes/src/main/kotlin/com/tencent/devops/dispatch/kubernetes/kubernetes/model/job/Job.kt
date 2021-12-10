package com.tencent.devops.dispatch.kubernetes.kubernetes.model.job

import com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod.Pod
import io.kubernetes.client.openapi.models.V1Job
import io.kubernetes.client.openapi.models.V1JobSpec
import io.kubernetes.client.openapi.models.V1ObjectMeta

object Job {

    fun job(
        job: JobData
    ): V1Job {
        with(job) {
            return V1Job()
                .apiVersion(apiVersion)
                .kind("Job")
                .metadata(
                    V1ObjectMeta()
                        .name(name)
                        .namespace(nameSpace)
                )
                .spec(spec(job))
        }
    }

    private fun spec(
        job: JobData
    ): V1JobSpec {
        with(job) {
            val spec = V1JobSpec()
                .backoffLimit(backoffLimit)
                .template(Pod.template(job.pod))
            if (activeDeadlineSeconds != null) {
                spec.activeDeadlineSeconds(activeDeadlineSeconds.toLong())
            }
            return spec
        }
    }
}
