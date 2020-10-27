apply(plugin="maven-publish")

val isSnapShot = project.version.toString().endsWith("-SNAPSHOT")

val sourceSets = extensions.getByType(SourceSetContainer::class.java)
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
    repositories {
        maven {
            val releaseRepoUrl: String by project
            val releaseRepoUsername: String by project
            val releaseRepoPassword: String by project
            val snapshotRepoUrl: String by project
            val snapshotRepoUsername: String by project
            val snapshotRepoPassword: String by project

            if (isSnapShot) {
                url = uri(snapshotRepoUrl)
                credentials {
                    username = snapshotRepoUsername
                    password = snapshotRepoPassword
                }
            } else {
                url = uri(releaseRepoUrl)
                credentials {
                    username = releaseRepoUsername
                    password = releaseRepoPassword
                }
            }
        }
    }
}
