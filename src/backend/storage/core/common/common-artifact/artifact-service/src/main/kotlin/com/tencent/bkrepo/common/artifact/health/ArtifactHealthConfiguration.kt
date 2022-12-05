package com.tencent.bkrepo.common.artifact.health

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    StorageHealthIndicator::class,
    LocalDiskSpaceHealthIndicator::class
)
class ArtifactHealthConfiguration
