package com.tencent.devops.quality.cron

import com.tencent.devops.quality.service.v2.QualityHisMetadataService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@EnableScheduling
@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class QualityCleaningJob @Autowired constructor(
    private val qualityHisMetadataService: QualityHisMetadataService
) {

    @Scheduled(cron = "0 0 2 * * ?")
    fun cleanMetaDetail() {
        qualityHisMetadataService.cleanMetaDetail()
    }
}
