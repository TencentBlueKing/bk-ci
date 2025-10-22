/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
import utils.MavenUtil
import java.net.URI

plugins {
    `maven-publish`
    signing
//    maven
    java
}

val sourceJar = tasks.register<Jar>("sourceJar") {
    archiveClassifier.set("sources")
    from(project.the<SourceSetContainer>()["main"].allSource)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    val javadoc = tasks.getByName("javadoc")
    dependsOn(javadoc)
    archiveClassifier.set("javadoc")
    from(javadoc)
}

tasks.getByName<Javadoc>("javadoc") {
    description = "javadoc for bk-ci"
    val options: StandardJavadocDocletOptions = options as StandardJavadocDocletOptions
    options.memberLevel = JavadocMemberLevel.PROTECTED
    options.header = project.name
    options.isAuthor = true
    options.isVersion = true
    // 不检查：非标的javadoc注解不报错
    options.addStringOption("Xdoclint:none", "-quiet")
    options.addStringOption("charset", "UTF-8")
    logging.captureStandardError(LogLevel.INFO)
    logging.captureStandardOutput(LogLevel.INFO)
    options.encoding = "UTF-8"
    options.charSet = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(sourceJar)
            artifact(javadocJar)

            pom {
                name.set("bk-ci")
                description.set("Tencent blueking ci project")
                url.set("https://github.com/Tencent/bk-ci")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/Tencent/bk-ci/blob/master/LICENSE.txt")
                        distribution.set("repo")
                        comments.set("A business-friendly OSS license")
                    }
                }

                developers {
                    developer {
                        name.set("bk-ci")
                        email.set("devops@tencent.com")
                        roles.set(listOf("Manager"))
                        url.set("https://bk.tencent.com")
                    }
                }

                scm {
                    url.set("https://github.com/Tencent/bk-ci")
                    connection.set("scm:git:https://github.com/Tencent/bk-ci.git")
                    developerConnection.set("scm:git:git@github.com:Tencent/bk-ci.git")
                }
            }

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
    repositories {
        maven {
            name = "nexus3"
            url = URI(MavenUtil.getUrl(project))
            credentials {
                username = MavenUtil.getUserName(project)
                password = MavenUtil.getPassword(project)
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}

val shouldPublish = project.the<SourceSetContainer>()["main"].allSource.files.isNotEmpty() ||
        project.name == "common-dependencies"

tasks.forEach {
    if (it.group == "publishing") {
        it.onlyIf { shouldPublish }
    }
}
