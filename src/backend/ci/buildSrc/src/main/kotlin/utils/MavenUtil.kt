package utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra

object MavenUtil {

    fun getUrl(project: Project): String {
        return if (System.getProperty("snapshot") == "true") {
            System.getProperty("snapshotMavenRepoDeployUrl")
                ?: System.getenv("build_snapshotMavenRepoDeployUrl")
                ?: project.extra["MAVEN_REPO_SNAPSHOT_DEPLOY_URL"]?.toString()
        } else {
            System.getProperty("mavenRepoDeployUrl")
                ?: System.getenv("build_mavenRepoDeployUrl")
                ?: project.extra["MAVEN_REPO_DEPLOY_URL"]?.toString()
        } ?: ""
    }

    fun getUserName(project: Project): String? {
        return if (System.getProperty("snapshot") == "true") {
            System.getProperty("snapshotMavenRepoUsername")
                ?: System.getenv("build_snapshotMavenRepoUsername")
                ?: project.extra["MAVEN_REPO_SNAPSHOT_USERNAME"]?.toString()
        } else {
            System.getProperty("mavenRepoUsername")
                ?: System.getenv("build_mavenRepoUsername")
                ?: project.extra["MAVEN_REPO_USERNAME"]?.toString()
        }
    }

    fun getPassword(project: Project): String? {
        return if (System.getProperty("snapshot") == "true") {
            System.getProperty("snapshotMavenRepoPassword")
                ?: System.getenv("build_snapshotMavenRepoPassword")
                ?: project.extra["MAVEN_REPO_SNAPSHOT_PASSWORD"]?.toString()
        } else {
            System.getProperty("mavenRepoPassword")
                ?: System.getenv("build_mavenRepoPassword")
                ?: project.extra["MAVEN_REPO_PASSWORD"]?.toString()
        }
    }
}
