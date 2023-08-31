package com.tencent.devops.common.web

import io.micrometer.core.annotation.Timed
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.jersey.server.JerseyTags
import io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider
import org.glassfish.jersey.server.internal.routing.UriRoutingContext
import org.glassfish.jersey.server.model.ResourceMethodInvoker
import org.glassfish.jersey.server.monitoring.RequestEvent

class BkJerseyTagProvider : JerseyTagsProvider {
    override fun httpRequestTags(event: RequestEvent): MutableIterable<Tag> {
        val response = event.containerResponse
        return if (isTimed(event)) {
            Tags.of(
                JerseyTags.method(event.containerRequest), JerseyTags.uri(event),
                JerseyTags.exception(event), JerseyTags.status(response), JerseyTags.outcome(response)
            )
        } else {
            Tags.of(JerseyTags.outcome(response))
        }
    }

    override fun httpLongRequestTags(event: RequestEvent): MutableIterable<Tag> {
        return if (isTimed(event)) {
            Tags.of(JerseyTags.method(event.containerRequest), JerseyTags.uri(event))
        } else {
            Tags.of(JerseyTags.method(event.containerRequest))
        }
    }

    private fun isTimed(event: RequestEvent): Boolean {
        val uriInfo = event.containerRequest.uriInfo
        return if (uriInfo is UriRoutingContext) {
            val endpoint = uriInfo.endpoint
            endpoint is ResourceMethodInvoker && endpoint.resourceMethod.isAnnotationPresent(Timed::class.java)
        } else {
            false
        }
    }
}
