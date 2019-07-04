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

package com.tencent.devops.common.log

import com.tencent.devops.common.log.meta.AnsiAttribute
import com.tencent.devops.common.log.meta.AnsiColor
import com.tencent.devops.common.log.meta.AnsiErase
import java.util.ArrayList
import java.util.concurrent.Callable

open class Ansi(private var builder: StringBuilder) {
    companion object {
        private val FIRST_ESC_CHAR: Char = 27.toChar()
        private val SECOND_ESC_CHAR = '['
        private val DISABLE = Ansi::class.java.name + ".disable"
        private var detector = Callable { !java.lang.Boolean.getBoolean(DISABLE) }

        fun newDetector(detector: Callable<Boolean>?) {
            if (detector == null) {
                throw IllegalArgumentException()
            }
            this.detector = detector
        }

        fun isDetected(): Boolean {
            return try {
                detector.call()
            } catch (e: Exception) {
                true
            }
        }

        private val holder = object : InheritableThreadLocal<Boolean>() {
            override fun initialValue(): Boolean {
                return isDetected()
            }
        }

        fun setEnabled(flag: Boolean) {
            holder.set(flag)
        }

        private fun isEnabled(): Boolean {
            return holder.get()
        }

        fun ansi(): Ansi {
            return if (isEnabled()) {
                Ansi()
            } else {
                NoAnsi()
            }
        }

        fun ansi(builder: StringBuilder): Ansi {
            return if (isEnabled()) {
                Ansi(builder)
            } else {
                NoAnsi(builder)
            }
        }

        fun ansi(size: Int): Ansi {
            return if (isEnabled()) {
                Ansi(size)
            } else {
                NoAnsi(size)
            }
        }
    }

    private val attributeOptions = ArrayList<Int>(5)

    constructor() : this(StringBuilder())

    constructor(size: Int) : this(StringBuilder(size))

    constructor(parent: Ansi) : this(StringBuilder(parent.builder)) {
        attributeOptions.addAll(parent.attributeOptions)
    }

    open fun fg(color: AnsiColor): Ansi {
        attributeOptions.add(color.fg())
        return this
    }

    open fun fgBlack(): Ansi {
        return this.fg(AnsiColor.BLACK)
    }

    open fun fgBlue(): Ansi {
        return this.fg(AnsiColor.BLUE)
    }

    fun fgCyan(): Ansi {
        return this.fg(AnsiColor.CYAN)
    }

    fun fgDefault(): Ansi {
        return this.fg(AnsiColor.DEFAULT)
    }

    fun fgGreen(): Ansi {
        return this.fg(AnsiColor.GREEN)
    }

    fun fgMagenta(): Ansi {
        return this.fg(AnsiColor.MAGENTA)
    }

    fun fgRed(): Ansi {
        return this.fg(AnsiColor.RED)
    }

    fun fgYellow(): Ansi {
        return this.fg(AnsiColor.YELLOW)
    }

    open fun bg(color: AnsiColor): Ansi {
        attributeOptions.add(color.bg())
        return this
    }

    fun bgCyan(): Ansi {
        return this.fg(AnsiColor.CYAN)
    }

    fun bgDefault(): Ansi {
        return this.bg(AnsiColor.DEFAULT)
    }

    fun bgGreen(): Ansi {
        return this.bg(AnsiColor.GREEN)
    }

    fun bgMagenta(): Ansi {
        return this.bg(AnsiColor.MAGENTA)
    }

    fun bgRed(): Ansi {
        return this.bg(AnsiColor.RED)
    }

    fun bgYellow(): Ansi {
        return this.bg(AnsiColor.YELLOW)
    }

    open fun fgBright(color: AnsiColor): Ansi {
        attributeOptions.add(color.fgBright())
        return this
    }

    fun fgBrightBlack(): Ansi {
        return this.fgBright(AnsiColor.BLACK)
    }

    fun fgBrightBlue(): Ansi {
        return this.fgBright(AnsiColor.BLUE)
    }

    fun fgBrightCyan(): Ansi {
        return this.fgBright(AnsiColor.CYAN)
    }

    fun fgBrightDefault(): Ansi {
        return this.fgBright(AnsiColor.DEFAULT)
    }

    fun fgBrightGreen(): Ansi {
        return this.fgBright(AnsiColor.GREEN)
    }

    fun fgBrightMagenta(): Ansi {
        return this.fgBright(AnsiColor.MAGENTA)
    }

    fun fgBrightRed(): Ansi {
        return this.fgBright(AnsiColor.RED)
    }

    fun fgBrightYellow(): Ansi {
        return this.fgBright(AnsiColor.YELLOW)
    }

    open fun bgBright(color: AnsiColor): Ansi {
        attributeOptions.add(color.bgBright())
        return this
    }

    fun bgBrightCyan(): Ansi {
        return this.fgBright(AnsiColor.CYAN)
    }

    fun bgBrightDefault(): Ansi {
        return this.bgBright(AnsiColor.DEFAULT)
    }

    fun bgBrightGreen(): Ansi {
        return this.bgBright(AnsiColor.GREEN)
    }

    fun bgBrightMagenta(): Ansi {
        return this.bg(AnsiColor.MAGENTA)
    }

    fun bgBrightRed(): Ansi {
        return this.bgBright(AnsiColor.RED)
    }

    fun bgBrightYellow(): Ansi {
        return this.bgBright(AnsiColor.YELLOW)
    }

    open fun a(attribute: AnsiAttribute): Ansi {
        attributeOptions.add(attribute.value)
        return this
    }

    open fun cursor(x: Int, y: Int): Ansi {
        return appendEscapeSequenceArray('H', arrayListOf(x, y))
    }

    open fun cursorToColumn(x: Int): Ansi {
        return appendEscapeSequence('G', x)
    }

    open fun cursorUp(y: Int): Ansi {
        return appendEscapeSequence('A', y)
    }

    open fun cursorDown(y: Int): Ansi {
        return appendEscapeSequence('B', y)
    }

    open fun cursorRight(x: Int): Ansi {
        return appendEscapeSequence('C', x)
    }

    open fun cursorLeft(x: Int): Ansi {
        return appendEscapeSequence('D', x)
    }

    open fun cursorDownLine(): Ansi {
        return appendEscapeSequence('E')
    }

    open fun cursorDownLine(n: Int): Ansi {
        return appendEscapeSequence('E', n)
    }

    open fun cursorUpLine(): Ansi {
        return appendEscapeSequence('F')
    }

    open fun cursorUpLine(n: Int): Ansi {
        return appendEscapeSequence('F', n)
    }

    open fun eraseScreen(): Ansi {
        return appendEscapeSequence('J', AnsiErase.ALL.value)
    }

    open fun eraseScreen(kind: AnsiErase): Ansi {
        return appendEscapeSequence('J', kind.value)
    }

    open fun eraseLine(): Ansi {
        return appendEscapeSequence('K')
    }

    open fun eraseLine(kind: AnsiErase): Ansi {
        return appendEscapeSequence('K', kind.value)
    }

    open fun scrollUp(rows: Int): Ansi {
        return appendEscapeSequence('S', rows)
    }

    open fun scrollDown(rows: Int): Ansi {
        return appendEscapeSequence('T', rows)
    }

    open fun saveCursorPosition(): Ansi {
        return appendEscapeSequence('s')
    }

    open fun restoreCursorPosition(): Ansi {
        return appendEscapeSequence('u')
    }

    open fun reset(): Ansi {
        return a(AnsiAttribute.RESET)
    }

    fun bold(): Ansi {
        return a(AnsiAttribute.INTENSITY_BOLD)
    }

    fun boldOff(): Ansi {
        return a(AnsiAttribute.INTENSITY_BOLD_OFF)
    }

    fun a(value: String?): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun a(value: Boolean): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun a(value: Char): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

//    fun a(value: CharArray, offset: Int, len: Int): Ansi {
//        flushAttributes()
//        builder.append(value, offset, len)
//        return this
//    }
//
//    fun a(value: CharArray): Ansi {
//        flushAttributes()
//        builder.append(value)
//        return this
//    }
//
//    fun a(value: CharSequence, start: Int, end: Int): Ansi {
//        flushAttributes()
//        builder.append(value, start, end)
//        return this
//    }
//
//    fun a(value: CharSequence): Ansi {
//        flushAttributes()
//        builder.append(value)
//        return this
//    }

    fun a(value: Double): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun a(value: Float): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun a(value: Int): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun a(value: Long): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

//    fun a(value: Any): Ansi {
//        flushAttributes()
//        builder.append(value)
//        return this
//    }

    fun a(value: StringBuffer): Ansi {
        flushAttributes()
        builder.append(value)
        return this
    }

    fun newline(): Ansi {
        flushAttributes()
        builder.append(System.getProperty("line.separator"))
        return this
    }

    fun format(pattern: String, vararg args: Any): Ansi {
        flushAttributes()
        builder.append(String.format(pattern, args))
        return this
    }

    override fun toString(): String {
        flushAttributes()
        return builder.toString()
    }

    private fun appendEscapeSequence(command: Char): Ansi {
        flushAttributes()
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        builder.append(command)
        return this
    }

    private fun appendEscapeSequence(command: Char, option: Int): Ansi {
        flushAttributes()
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        builder.append(option)
        builder.append(command)
        return this
    }

    private fun appendEscapeSequenceArray(command: Char, options: ArrayList<Int>): Ansi {
        flushAttributes()
        return innerAppendEscapeSequence(command, options)
    }

    private fun flushAttributes() {
        if (attributeOptions.isEmpty())
            return
        if (attributeOptions.size == 1 && attributeOptions[0] == 0) {
            builder.append(FIRST_ESC_CHAR)
            builder.append(SECOND_ESC_CHAR)
            builder.append('m')
        } else {
            innerAppendEscapeSequence('m', attributeOptions)
        }
        attributeOptions.clear()
    }

    private fun innerAppendEscapeSequence(command: Char, options: ArrayList<Int>): Ansi {
        builder.append(FIRST_ESC_CHAR)
        builder.append(SECOND_ESC_CHAR)
        val size = options.size
        for (i in 0 until size) {
            if (i != 0) {
                builder.append(';')
            }
            builder.append(options[i])
        }
        builder.append(command)
        return this
    }
}
