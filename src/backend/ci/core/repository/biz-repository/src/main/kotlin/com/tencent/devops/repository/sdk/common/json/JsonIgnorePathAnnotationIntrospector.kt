package com.tencent.devops.repository.sdk.common.json

import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector

class JsonIgnorePathAnnotationIntrospector : JacksonAnnotationIntrospector() {

    override fun hasIgnoreMarker(m: AnnotatedMember): Boolean {
        return m.getAnnotation(JsonIgnorePath::class.java) != null || super.hasIgnoreMarker(m)
    }
}
