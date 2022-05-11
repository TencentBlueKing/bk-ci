/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.plugin.processor

import com.tencent.bkrepo.common.plugin.api.EXTENSION_LOCATION
import com.tencent.bkrepo.common.plugin.api.Extension
import com.tencent.bkrepo.common.plugin.api.ExtensionType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

/**
 * [Extension]注解处理器，将标记了[Extension]的类自动写入extensions.properties
 */
class ExtensionAnnotationProcessor : AbstractProcessor() {

    private val extensions = mutableMapOf<ExtensionType, MutableSet<String>>()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Extension::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        if (roundEnv.processingOver()) {
            generateExtensionFile()
        } else {
            roundEnv.getElementsAnnotatedWith(Extension::class.java).forEach {
                processExtensionElement(it)
            }
        }
        return false
    }

    private fun processExtensionElement(element: Element) {
        log("Found @Extension element: $element")
        if (element.kind != ElementKind.CLASS || element !is TypeElement) {
            error("Invalid element type, class expected", element)
            return
        }
        val type = determineExtensionType(element)
        extensions.getOrPut(type) { mutableSetOf() }.add(element.qualifiedName.toString())
    }

    private fun determineExtensionType(element: Element): ExtensionType {
        if (element.getAnnotation(Controller::class.java) != null ||
            element.getAnnotation(RestController::class.java) != null
        ) {
            return ExtensionType.CONTROLLER
        }
        return ExtensionType.POINT
    }

    private fun generateExtensionFile() {
        val filter = processingEnv.filer
        try {
            val extensionFile = filter.createResource(StandardLocation.CLASS_OUTPUT, "", EXTENSION_LOCATION)
            ExtensionFiles.write(extensions, extensionFile.openOutputStream())
            log("Success to generate extension factories file")
        } catch (exception: IOException) {
            error(exception.message.orEmpty())
        }
    }

    private fun log(message: String) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, message)
    }

    private fun error(message: String, element: Element? = null) {
        processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message, element)
    }
}
