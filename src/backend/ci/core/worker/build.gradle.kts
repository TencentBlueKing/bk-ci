/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
subprojects {
    group = "com.tencent.bk.devops.ci.worker"

    configurations.forEach {
        it.exclude(group = "com.perforce", module = "*")
        it.exclude(group = "com.google.guava", module = "*")
        it.exclude(group = "com.googlecode.javaewah", module = "*")
        it.exclude(group = "com.vdurmont", module = "*")
        it.exclude(group = "com.github.ulisesbocchio", module = "*")
        it.exclude(group = "org.jasypt", module = "*")
        it.exclude(group = "joda-time", module = "*")
        it.exclude(group = "org.jolokia", module = "*")
        it.exclude(group = "com.github.taptap", module = "*")
        it.exclude(group = "jakarta.xml.bind", module = "*")
        it.exclude(group = "org.jboss.spec.javax.websocket", module = "*")
        it.exclude(group = "org.jboss.spec.javax.servlet", module = "*")
        it.exclude(group = "org.jboss.spec.javax.annotation", module = "*")
        it.exclude(group = "net.sf.jopt-simple", module = "*")
        it.exclude(group = "com.ctc.wstx", module = "*")
        it.exclude(group = "jakarta.ws.rs", module = "*")
        it.exclude(group = "jakarta.activation", module = "*")
        it.exclude(group = "jakarta.annotation", module = "*")
        it.exclude(group = "jakarta.servlet", module = "*")
        it.exclude(group = "jakarta.validation", module = "*")
        it.exclude(group = "org.apache.lucene", module = "*")
        it.exclude(group = "org.hashids", module = "*")
        it.exclude(group = "org.glassfish", module = "*")
        it.exclude(group = "org.glassfish.jersey", module = "*")
        it.exclude(group = "org.glassfish.jersey.media", module = "*")
        it.exclude(group = "org.glassfish.jersey.core", module = "*")
        it.exclude(group = "org.glassfish.hk2", module = "*")
        it.exclude(group = "org.glassfish.hk2.external", module = "*")
        it.exclude(group = "jakarta.annotation", module = "*")
        it.exclude(group = "javax.servlet", module = "*")
        it.exclude(group = "javax.validation", module = "*")
        it.exclude(group = "javax.mail", module = "*")
        it.exclude(group = "io.swagger", module = "*")
        it.exclude(group = "io.micrometer", module = "*")
        it.exclude(group = "io.jsonwebtoken", module = "*")
        it.exclude(group = "io.github.openfeign.form", module = "*")
        it.exclude(group = "io.github.openfeign", module = "*")
//        it.exclude(group = "de.regnis.q.sequence", module = "*") // svnkit 间接依赖了他
        it.exclude(group = "com.cronutils", module = "*")
        it.exclude(group = "org.aspectj", module = "*")
        it.exclude(group = "org.jooq", module = "*")
        it.exclude(group = "org.objectweb.asm", module = "*")
        it.exclude(group = "org.springframework", module = "*")
        it.exclude(group = "org.springframework.boot", module = "*")
        it.exclude(group = "org.springframework.cloud", module = "*")
        it.exclude(group = "org.springframework.security", module = "*")
        it.exclude("org.springframework.boot", "spring-boot-starter-log4j2")
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api")
        testImplementation("org.junit.jupiter:junit-jupiter-engine")
        testImplementation("org.junit.jupiter:junit-jupiter-params")
    }
}
