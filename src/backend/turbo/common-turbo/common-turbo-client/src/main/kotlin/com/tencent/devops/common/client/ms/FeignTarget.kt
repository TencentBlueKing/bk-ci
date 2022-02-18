package com.tencent.devops.common.client.ms

import feign.Target

interface FeignTarget<T> : Target<T>
