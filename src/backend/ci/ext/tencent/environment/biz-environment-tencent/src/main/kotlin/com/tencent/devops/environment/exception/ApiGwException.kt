package com.tencent.devops.environment.exception

import okhttp3.Response

class ApiGwException(val resp: Response, message: String?) : RuntimeException(message)
