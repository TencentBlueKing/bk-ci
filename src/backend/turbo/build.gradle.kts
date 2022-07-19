plugins {
	id("com.tencent.devops.boot") version "0.0.6-SNAPSHOT"
	id("org.owasp.dependencycheck") version "7.1.0.1"
}

allprojects {
	group = "com.tencent.bk.devops.turbo"
	version = "0.0.1"

	apply(plugin = "com.tencent.devops.boot")


	configurations.all {
		exclude(group = "org.slf4j", module = "log4j-over-slf4j")
		exclude(group = "org.slf4j", module = "slf4j-log4j12")
		exclude(group = "org.slf4j", module = "slf4j-nop")
		resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.MINUTES)
	}

	dependencyManagement {
		dependencies {
			dependency("javax.ws.rs:javax.ws.rs-api:${Versions.jaxrsVersion}")
			dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${Versions.jasyptVersion}")
			dependency("org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastleVersion}")
			dependency("com.google.guava:guava:${Versions.guavaVersion}")
			dependency("commons-io:commons-io:${Versions.commonIo}")
			dependencySet("io.swagger:${Versions.swaggerVersion}") {
                entry("swagger-annotations")
                entry("swagger-jersey2-jaxrs")
                entry("swagger-models")
            }
		}
	}
}
