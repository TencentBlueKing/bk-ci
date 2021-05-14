package com.tencent.devops.project.service

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

internal class ProjectTagServiceTest {
    @Test
    fun systemRouteTag() {
        val routeMap = mutableMapOf<String, String>()
        routeMap["systemA"] = "routerA"
        routeMap["systemB"] = "routerB"
        println(routeMap.toString())
    }
}
