package com.tencent.bkrepo.maven.exception

class MavenBadRequestException(
    error: String? = "Missing groupId or artifactId or version or classifier"
) : RuntimeException(error)
