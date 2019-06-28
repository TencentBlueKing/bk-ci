/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.util

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.Random
import javax.imageio.ImageIO

object ImageUtil {

    private const val FontSize = 64

    fun drawImage(logoStr: String, width: Int, height: Int): File {
        val logoBackgroundColor = arrayOf("#FF5656", "#FFB400", "#30D878", "#3C96FF")
        val max = logoBackgroundColor.size - 1
        val min = 0
        val random = Random()
        val backgroundIndex = random.nextInt(max) % (max - min + 1) + min
        // 创建BufferedImage对象
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        // 获取Graphics2D
        val g2d = bi.createGraphics()
        // 设置透明度
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f)

        when (backgroundIndex) {
            0 -> {
                g2d.background = Color.RED
            }
            1 -> {
                g2d.background = Color.YELLOW
            }
            2 -> {
                g2d.background = Color.GREEN
            }
            3 -> {
                g2d.background = Color.BLUE
            }
        }
        g2d.clearRect(0, 0, width, height)
        g2d.color = Color.WHITE
        g2d.stroke = BasicStroke(1.0f)

        val font = Font("宋体", Font.PLAIN, FontSize)
        g2d.font = font
        val fontMetrics = g2d.fontMetrics
        val heightAscent = fontMetrics.ascent

        val context = g2d.fontRenderContext
        val stringBounds = font.getStringBounds(logoStr, context)
        val fontWidth = stringBounds.width.toFloat()

        g2d.drawString(
            logoStr,
            (width / 2 - fontWidth / 2),
            (height / 2 + heightAscent / 2).toFloat()
        )
        // 透明度设置 结束
        g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
        // 释放对象
        g2d.dispose()
        // 保存文件
        val logo = Files.createTempFile("default_", ".png").toFile()
        ImageIO.write(bi, "png", logo)
        return logo
    }
}