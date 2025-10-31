package com.tencent.devops.notify.api.annotation

import kotlin.annotation.AnnotationRetention.RUNTIME

@Target(AnnotationTarget.FIELD)
@Retention(RUNTIME)
annotation class BkNotifyReceivers
