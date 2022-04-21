import java.net.URI


listOf(
    "api-turbo"
).map { project(it) }.forEach {
    val mirrorsFlag = System.getenv("mavenRepoDeployUrl") != null
    if (!mirrorsFlag) {
        it.apply(plugin = "com.tencent.devops.publish")
    } else {
        it.pluginManager.apply(MavenPublishPlugin::class.java)
        it.pluginManager.apply(SigningPlugin::class.java)
    }
    it.configure<PublishingExtension> {
        if (mirrorsFlag) {
            publications.create("mavenJava", MavenPublication::class.java) {
                this.from(components.getByName("java"))
            }
        }
        publications.withType<MavenPublication> {
            println("project name: ${it.name}")
            artifactId = it.name
            pom {
                name.set(it.name)
                description.set(it.description ?: it.name)
                url.set("https://github.com/Tencent/bk-ci")
                licenses {
                    license {
                        name.set("The MIT License (MIT)")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        name.set("bk-ci")
                        email.set("devops@tencent.com")
                        url.set("https://bk.tencent.com")
                        roles.set(listOf("Manager"))
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Tencent/bk-ci.get")
                    developerConnection.set("scm:git:ssh://github.com/Tencent/bk-ci.git")
                    url.set("https://github.com/Tencent/bk-ci")
                }
            }
        }

        if (mirrorsFlag) {
            repositories {
                maven {
                    // 正式包
                    var mavenRepoDeployUrl = System.getProperty("mavenRepoDeployUrl")
                    var mavenRepoUsername = System.getProperty("mavenRepoUsername")
                    var mavenRepoPassword = System.getProperty("mavenRepoPassword")

                    if (mavenRepoDeployUrl == null) {
                        mavenRepoDeployUrl = System.getenv("mavenRepoDeployUrl")
                    }

                    if (mavenRepoUsername == null) {
                        mavenRepoUsername = System.getenv("mavenRepoUsername")
                    }

                    if (mavenRepoPassword == null) {
                        mavenRepoPassword = System.getenv("mavenRepoPassword")
                    }

                    // 快照包
                    var snapshotMavenRepoDeployUrl = System.getProperty("snapshotMavenRepoDeployUrl")
                    var snapshotMavenRepoUsername = System.getProperty("snapshotMavenRepoUsername")
                    var snapshotMavenRepoPassword = System.getProperty("snapshotMavenRepoPassword")

                    if (snapshotMavenRepoDeployUrl == null) {
                        snapshotMavenRepoDeployUrl = System.getenv("snapshotMavenRepoDeployUrl")
                    }

                    if (snapshotMavenRepoUsername == null) {
                        snapshotMavenRepoUsername = System.getenv("snapshotMavenRepoUsername")
                    }

                    if (snapshotMavenRepoPassword == null) {
                        snapshotMavenRepoPassword = System.getenv("snapshotMavenRepoPassword")
                    }

                    // 需要配置以上环境变量
                    url = URI(
                        if (version.toString().endsWith("SNAPSHOT")) snapshotMavenRepoDeployUrl else mavenRepoDeployUrl
                    )
                    credentials {
                        username =
                            if (version.toString()
                                    .endsWith("SNAPSHOT")) snapshotMavenRepoUsername else mavenRepoUsername
                        password =
                            if (version.toString()
                                    .endsWith("SNAPSHOT")) snapshotMavenRepoPassword else mavenRepoPassword
                    }
                }
            }
        }
    }

    it.configure<SigningExtension> {
        if (mirrorsFlag) {
            sign(it.extensions.getByType(PublishingExtension::class.java).publications)
        }
    }
}
