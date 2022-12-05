package com.tencent.bkrepo.maven.exception

class MavenRequestForbiddenException(
    error: String? = "The request has been forbidden by the server"
) : RuntimeException(error)
