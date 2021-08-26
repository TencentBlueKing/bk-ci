plugins {
	id("com.tencent.devops.boot") version "0.0.4"
}

allprojects {
	group = "com.tencent.bk.devops.turbo"
	version = "0.0.1"

	apply(plugin = "com.tencent.devops.boot")


	configurations.all {
		exclude(group = "org.slf4j", module = "log4j-over-slf4j")
		exclude(group = "org.slf4j", module = "slf4j-log4j12")
		exclude(group = "org.slf4j", module = "slf4j-nop")
		resolutionStrategy {
			cacheChangingModulesFor(0, TimeUnit.MINUTES)
		}
	}

	dependencyManagement {
		dependencies {
			dependency("javax.ws.rs:javax.ws.rs-api:${DependencyVersions.jaxrsVersion}")
			dependency("com.github.ulisesbocchio:jasypt-spring-boot-starter:${DependencyVersions.jasyptVersion}")
			dependency("org.bouncycastle:bcprov-jdk16:${DependencyVersions.bouncyCastleVersion}")
			dependency("io.springfox:springfox-boot-starter:${DependencyVersions.swaggerVersion}")
			dependency("com.google.guava:guava:${DependencyVersions.guavaVersion}")
		}
	}
}
