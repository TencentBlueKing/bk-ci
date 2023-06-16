package com.tencent.devops.common.web

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.jersey.server.JerseyTags
import io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider
import org.glassfish.jersey.server.monitoring.RequestEvent

class BkJerseyTagProvider : JerseyTagsProvider {
    override fun httpRequestTags(event: RequestEvent): MutableIterable<Tag> {
        val response = event.containerResponse
        return Tags.of(JerseyTags.outcome(response))
    }

    override fun httpLongRequestTags(event: RequestEvent): MutableIterable<Tag> {
        return Tags.of(JerseyTags.method(event.containerRequest))
    }
}
