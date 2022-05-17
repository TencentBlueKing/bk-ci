val detekt: Configuration by configurations.creating

tasks.register<JavaExec>("detektCheck") {
    group = "verification"
    main = "io.gitlab.arturbosch.detekt.cli.Main"
    classpath = detekt
    val input = "$projectDir"
    val config = "$projectDir/../detekt.yml"
    // 输出结果的文件格式：[ html|xml|txt ]
    val output = "$projectDir/build/report/detekt-report.html"
    val exclude = "**/test/**,**/build/**,**/resources/**,**.gradle.kts"
    val params = listOf("-i", input, "-c", config, "-ex", exclude, "-r", "html:$output")
    args(params)
}

dependencies {
    // detekt classpath中引用, 不会被打包到jar包中
    detekt("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.6.21")
    detekt("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:1.20.0")
}

// detekt1.16需要kotlin1.4的兼容方案
configurations.all {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
}
