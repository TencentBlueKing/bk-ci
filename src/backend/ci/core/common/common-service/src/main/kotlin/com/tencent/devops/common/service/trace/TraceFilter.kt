package com.tencent.devops.common.service.trace

import com.tencent.devops.common.api.util.UUIDUtil
import org.apache.skywalking.apm.toolkit.trace.TraceContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest

@Component
class TraceFilter : Filter {
    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpServletRequest = request as HttpServletRequest
        val bizId = httpServletRequest?.getHeader(TraceTag.BIZID)
        if (bizId == null || bizId.isNullOrEmpty()) {
            val traceId = TraceContext.traceId()
            if (traceId != null && !traceId.isNullOrEmpty() && !traceId.contains("N/A") && !traceId.contains("Ignored Trace")) {
                MDC.put(TraceTag.BIZID, traceId)
            } else {
                MDC.put(TraceTag.BIZID, UUIDUtil.generate())
            }
        } else {
            MDC.put(TraceTag.BIZID, bizId.toString())
        }
        logger.debug("servlet Filter bizId ${MDC.get(TraceTag.BIZID)} , traceId ${TraceContext.traceId()}")
        chain?.doFilter(request, response)
    }

    override fun init(filterConfig: FilterConfig?) {
    }

    companion object {
        val logger = LoggerFactory.getLogger(this:: class.java)
    }
}