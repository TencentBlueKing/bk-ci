@Suppress("NewLineAtEndOfFile")
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    jcenter()
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
