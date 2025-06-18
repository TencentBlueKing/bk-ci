plugins {
    id("io.github.gradle-nexus.publish-plugin")
}

nexusPublishing {
    repositories {
        sonatype {
            // 正式包
            var mavenRepoDeployUrl = System.getProperty("mavenRepoDeployUrl")
            var mavenRepoUsername = System.getProperty("mavenRepoUsername")
            var mavenRepoPassword = System.getProperty("mavenRepoPassword")

            if (mavenRepoDeployUrl == null) {
                mavenRepoDeployUrl = System.getenv("build_mavenRepoDeployUrl")
            }

            if (mavenRepoUsername == null) {
                mavenRepoUsername = System.getenv("build_mavenRepoUsername")
            }

            if (mavenRepoPassword == null) {
                mavenRepoPassword = System.getenv("build_mavenRepoPassword")
            }

            if (mavenRepoDeployUrl == null) {
                mavenRepoDeployUrl = project.extra["MAVEN_REPO_DEPLOY_URL"]?.toString()
            }

            if (mavenRepoUsername == null) {
                mavenRepoUsername = project.extra["MAVEN_REPO_USERNAME"]?.toString()
            }

            if (mavenRepoPassword == null) {
                mavenRepoPassword = project.extra["MAVEN_REPO_PASSWORD"]?.toString()
            }

            // 快照包
            var snapshotMavenRepoDeployUrl = System.getProperty("snapshotMavenRepoDeployUrl")
            var snapshotMavenRepoUsername = System.getProperty("snapshotMavenRepoUsername")
            var snapshotMavenRepoPassword = System.getProperty("snapshotMavenRepoPassword")

            if (snapshotMavenRepoDeployUrl == null) {
                snapshotMavenRepoDeployUrl = System.getenv("build_snapshotMavenRepoDeployUrl")
            }

            if (snapshotMavenRepoUsername == null) {
                snapshotMavenRepoUsername = System.getenv("build_snapshotMavenRepoUsername")
            }

            if (snapshotMavenRepoPassword == null) {
                snapshotMavenRepoPassword = System.getenv("build_snapshotMavenRepoPassword")
            }

            if (snapshotMavenRepoDeployUrl == null) {
                snapshotMavenRepoDeployUrl = project.extra["MAVEN_REPO_SNAPSHOT_DEPLOY_URL"]?.toString()
            }

            if (snapshotMavenRepoUsername == null) {
                snapshotMavenRepoUsername = project.extra["MAVEN_REPO_SNAPSHOT_USERNAME"]?.toString()
            }

            if (snapshotMavenRepoPassword == null) {
                snapshotMavenRepoPassword = project.extra["MAVEN_REPO_SNAPSHOT_PASSWORD"]?.toString()
            }

            nexusUrl.set(uri(mavenRepoDeployUrl))
            snapshotRepositoryUrl.set(uri(snapshotMavenRepoDeployUrl))
            username.set(if (System.getProperty("snapshot") == "true") snapshotMavenRepoUsername else mavenRepoUsername)
            password.set(if (System.getProperty("snapshot") == "true") snapshotMavenRepoPassword else mavenRepoPassword)
        }
    }
}