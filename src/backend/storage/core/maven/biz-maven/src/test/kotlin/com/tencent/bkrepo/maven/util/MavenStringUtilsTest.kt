package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.pojo.MavenVersion
import com.tencent.bkrepo.maven.util.MavenStringUtils.setVersion
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

internal class MavenStringUtilsTest {

    @Test
    fun resolverName() {
        val jarName = "my-app-4.0-20220110.065755-5-jar-with-dependencies.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0-SNAPSHOT",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0-SNAPSHOT", mavenVersion.version) },
            { Assertions.assertEquals("20220110.065755", mavenVersion.timestamp) },
            { Assertions.assertEquals("5", mavenVersion.buildNo) },
            { Assertions.assertEquals("jar-with-dependencies", mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }

    @Test
    fun resolverName1() {
        val jarName = "my-app-4.0.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0", mavenVersion.version) },
            { Assertions.assertEquals(null, mavenVersion.timestamp) },
            { Assertions.assertEquals(null, mavenVersion.buildNo) },
            { Assertions.assertEquals(null, mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }

    @Test
    fun resolverName2() {
        val jarName = "my-app-4.0-jar-with-dependencies.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0", mavenVersion.version) },
            { Assertions.assertEquals(null, mavenVersion.timestamp) },
            { Assertions.assertEquals(null, mavenVersion.buildNo) },
            { Assertions.assertEquals("jar-with-dependencies", mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }

    @Test
    fun resolverName3() {
        val jarName = "my-app-4.0-20220110.065755-5.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0-SNAPSHOT",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0-SNAPSHOT", mavenVersion.version) },
            { Assertions.assertEquals("20220110.065755", mavenVersion.timestamp) },
            { Assertions.assertEquals("5", mavenVersion.buildNo) },
            { Assertions.assertEquals(null, mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }

    @Test
    fun resolverName4() {
        val jarName = "my-app-4.0-jar-with-dependencies.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0-jar-with-dependencies",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0-jar-with-dependencies", mavenVersion.version) },
            { Assertions.assertEquals(null, mavenVersion.timestamp) },
            { Assertions.assertEquals(null, mavenVersion.buildNo) },
            { Assertions.assertEquals(null, mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }

    @Test
    fun resolverName5() {
        val jarName = "my-app-4.0-SNAPSHOT-01-jar-with-dependencies.jar"
        val mavenVersion = MavenVersion(
            artifactId = "my-app",
            version = "4.0-SNAPSHOT-01",
            packaging = "jar"
        )
        mavenVersion.setVersion(jarName)
        assertAll(
            { Assertions.assertEquals("my-app", mavenVersion.artifactId) },
            { Assertions.assertEquals("4.0-SNAPSHOT-01", mavenVersion.version) },
            { Assertions.assertEquals(null, mavenVersion.timestamp) },
            { Assertions.assertEquals(null, mavenVersion.buildNo) },
            { Assertions.assertEquals("jar-with-dependencies", mavenVersion.classifier) },
            { Assertions.assertEquals("jar", mavenVersion.packaging) }
        )
        println("$mavenVersion")
    }
}
