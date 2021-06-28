package com.tencent.devops.auth.service.v0

import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.AuthServiceCode
import com.tencent.devops.common.auth.code.BSCommonAuthServiceCode
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import com.tencent.devops.common.auth.code.ExperienceAuthServiceCode
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.auth.code.RepoAuthServiceCode
import com.tencent.devops.common.auth.code.TicketAuthServiceCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceCodeService @Autowired constructor(
    val pipelineAuthServiceCode: PipelineAuthServiceCode,
    val ticketAuthServiceCode: TicketAuthServiceCode,
    val environmentAuthServiceCode: EnvironmentAuthServiceCode,
    val qualityAuthServiceCode: QualityAuthServiceCode,
    val repoAuthServiceCode: RepoAuthServiceCode,
    val experienceAuthServiceCode: ExperienceAuthServiceCode,
    val commonAuthServiceCode: BSCommonAuthServiceCode
) {
    fun getServiceCodeByResource(authResourceType: String): AuthServiceCode {
        val serviceCode = when (authResourceType) {
            AuthResourceType.PIPELINE_DEFAULT.value -> pipelineAuthServiceCode
            AuthResourceType.EXPERIENCE_GROUP.value -> experienceAuthServiceCode
            AuthResourceType.EXPERIENCE_TASK.value -> experienceAuthServiceCode
            AuthResourceType.ENVIRONMENT_ENV_NODE.value -> environmentAuthServiceCode
            AuthResourceType.ENVIRONMENT_ENVIRONMENT.value -> environmentAuthServiceCode
            AuthResourceType.QUALITY_RULE.value -> qualityAuthServiceCode
            AuthResourceType.QUALITY_GROUP.value -> qualityAuthServiceCode
            AuthResourceType.TICKET_CREDENTIAL.value -> ticketAuthServiceCode
            AuthResourceType.TICKET_CERT.value -> ticketAuthServiceCode
            AuthResourceType.CODE_REPERTORY.value -> repoAuthServiceCode
            else -> commonAuthServiceCode
        }
        return serviceCode
    }
}
