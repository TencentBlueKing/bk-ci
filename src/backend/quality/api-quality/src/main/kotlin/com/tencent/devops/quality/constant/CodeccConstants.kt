package com.tencent.devops.quality.constant

val codeccToolUrlPathMap = mapOf(
        "COVERITY" to "procontrol/buglist",
        "KLOCWORK" to "procontrol/buglist",
        "CPPLINT" to "procontrol/multidefectmanage",
        "ESLINT" to "procontrol/multidefectmanage",
        "PYLINT" to "procontrol/multidefectmanage",
        "GOML" to "procontrol/multidefectmanage",
        "CCN" to "procontrol/ccndefectmanage",
        "DUPC" to "backend/duplicatecode/warnlist",
        "CHECKSTYLE" to "procontrol/multidefectmanage",
        "STYLECOP" to "procontrol/multidefectmanage",
        "DETEKT" to "procontrol/multidefectmanage",
        "PHPCS" to "procontrol/multidefectmanage",
        "SENSITIVE" to "procontrol/multidefectmanage")