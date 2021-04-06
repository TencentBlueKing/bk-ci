val detekt: Configuration by configurations.creating

tasks.register<JavaExec>("detektCheck") {
    group = "verification"
    main = "io.gitlab.arturbosch.detekt.cli.Main"
    classpath = detekt
    val input = "$projectDir"
    val config = "$projectDir/../detekt.yml"
    // 输出结果的文件格式：[ html|xml|txt ]
    val output = "$projectDir/build/eport/detekt-report.html"
    val exclude = "**/test/**,**/build/**,**/resources/**"
    val params = listOf("-i", input, "-c", config, "-ex", exclude, "-r", "html:$output")
    args(params)
}

dependencies {
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:1.16.0")
}
