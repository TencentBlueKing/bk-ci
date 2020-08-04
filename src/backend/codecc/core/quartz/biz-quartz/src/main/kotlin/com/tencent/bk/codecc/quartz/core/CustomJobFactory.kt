package com.tencent.bk.codecc.quartz.core

import org.quartz.spi.TriggerFiredBundle
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scheduling.quartz.AdaptableJobFactory

class CustomJobFactory : AdaptableJobFactory(), ApplicationContextAware {

    override fun setApplicationContext(appContext: ApplicationContext) {
        applicationContext = appContext
    }

    companion object {
        var applicationContext: ApplicationContext? = null
    }

    override fun createJobInstance(bundle: TriggerFiredBundle): Any {
        val jobObj = super.createJobInstance(bundle)
        applicationContext!!.autowireCapableBeanFactory.autowireBean(jobObj)
        return jobObj
    }
}