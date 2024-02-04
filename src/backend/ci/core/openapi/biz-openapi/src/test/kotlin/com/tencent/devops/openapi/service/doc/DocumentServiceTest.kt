package com.tencent.devops.openapi.service.doc

import com.tencent.devops.common.web.JerseyConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [JerseyConfig::class, DocumentService::class])
class DocumentServiceTest @Autowired constructor(
    private val document: DocumentService
) {
    @Test
    fun docInit() {
        try {
            val config = ConfigurationBuilder()
            config.addUrls(ClasspathHelper.forPackage("com.tencent.devops"))
            config.setExpandSuperTypes(true)
            config.setScanners(Scanners.TypesAnnotated)
            val reflections = Reflections(config)

            val doc = document.docInit(
                checkMetaData = true,
                checkMDData = true,
                polymorphism = DocumentService.getAllSubType(reflections),
                outputPath = "build/swaggerDoc/",
                parametersInfo = DocumentService.getAllApiModelInfo(reflections)
            )
            println("${doc.size}|${doc.keys}")
        } catch (e: Throwable) {
            // 抛错时不影响Test流程
            println("docInit error")
            e.printStackTrace()
        }
    }
}
