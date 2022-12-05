dependencies {
    implementation("com.google.guava:guava")
    implementation("com.squareup.okhttp3:okhttp")
    implementation(project(":common:common-api"))
    implementation("ch.qos.logback:logback-classic")
    implementation("com.squareup.okhttp3:okhttp-sse")
}
plugins {
    id("me.champeau.gradle.jmh") version Versions.JMH
}
