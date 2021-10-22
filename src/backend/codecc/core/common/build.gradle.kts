subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")

    dependencies {
        api("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        api("org.jetbrains.kotlin:kotlin-reflect")
//        api("org.apache.tomcat.embed:tomcat-embed-core")
    }
}