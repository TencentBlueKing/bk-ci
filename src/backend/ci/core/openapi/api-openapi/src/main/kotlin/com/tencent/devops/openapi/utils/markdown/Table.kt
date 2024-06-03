package com.tencent.devops.openapi.utils.markdown

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.BK_NO_SUCH_PARAMETER
import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.FILL
import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.MIN_FILL
import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.SEPARATOR
import com.tencent.devops.openapi.utils.markdown.MarkdownCharacter.WHITESPACE

class Table(
    var header: TableRow = TableRow(),
    var rows: List<TableRow> = emptyList(),
    override val key: String = ""
) : MarkdownElement(key) {
    companion object {
        const val classType = "table"
    }

    override fun toString(): String {
        val body = StringBuffer()
        if (rows.isEmpty()) {
            body.append(
                Text(6, MessageUtil.getMessageByLocale(BK_NO_SUCH_PARAMETER, "zh_CN"), "")
            )
            return body.toString()
        }
        body.append('\n')
        val interval = getColumnWidths(rows.plus(header), MIN_FILL)
        header.columns.tableJoinToString(
            buffer = body,
            separator = SEPARATOR,
            prefix = SEPARATOR,
            postfix = SEPARATOR
        ) { index, element ->
            WHITESPACE + element.padEnd(interval[index] ?: 0, ' ') + WHITESPACE
        }.append('\n')
        List(header.columns.size) { "-" }.tableJoinToString(
            buffer = body,
            separator = SEPARATOR,
            prefix = SEPARATOR,
            postfix = SEPARATOR
        ) { index, element ->
            WHITESPACE + element.padEnd(interval[index] ?: 0, FILL) + WHITESPACE
        }.append('\n')
        rows.forEach {
            it.columns.tableJoinToString(
                buffer = body,
                separator = SEPARATOR,
                prefix = SEPARATOR,
                postfix = SEPARATOR
            ) { index, element ->
                WHITESPACE + element.padEnd(interval[index] ?: 0, ' ') + WHITESPACE
            }.append('\n')
        }
        return body.append('\n').toString()
    }

    fun setRow(vararg row: String): Table {
        val columns = row.toList()
        val new = rows.toMutableList()
        new.removeIf { it.columns[0] == columns[0] }
        new.add(0, TableRow(columns))
        rows = new
        return this
    }

    fun removeRow(key: String): Table {
        val new = rows.toMutableList()
        new.removeIf { it.columns[0] == key }
        rows = new
        return this
    }

    fun checkLoadModel(loadModel: MutableList<String>): Table {
        rows.forEach {
            if (it.columns[1].contains("](")) {
                loadModel.addNoRepeat(it.columns[1].split("[")[1].split("]")[0])
            }
        }
        return this
    }

    private fun getColumnWidths(rows: List<TableRow>, minimumColumnWidth: Int): Map<Int, Int> {
        val columnWidths: MutableMap<Int, Int> = HashMap()
        if (rows.isEmpty()) {
            return columnWidths
        }
        for (columnIndex in 0 until rows[0].columns.size) {
            columnWidths[columnIndex] =
                getMaximumItemLength(rows, columnIndex, minimumColumnWidth)
        }
        return columnWidths
    }

    private fun getMaximumItemLength(rows: List<TableRow>, columnIndex: Int, minimumColumnWidth: Int): Int {
        var maximum = minimumColumnWidth
        for (row in rows) {
            if (row.columns.size < columnIndex + 1) {
                continue
            }
            val value: Any = row.columns[columnIndex]
            maximum = Math.max(value.toString().length, maximum)
        }
        return maximum
    }

    private fun <T> MutableList<T>.addNoRepeat(text: T) {
        if (text !in this) {
            this.add(text)
        }
    }

    private fun <T, A : Appendable> Iterable<T>.tableJoinToString(
        buffer: A,
        separator: CharSequence = ", ",
        prefix: CharSequence = "",
        postfix: CharSequence = "",
        transform: ((Int, T) -> CharSequence)
    ): A {
        buffer.append(prefix)
        var count = 0
        for (element in this) {
            if (++count > 1) buffer.append(separator)
            buffer.append(transform(count - 1, element))
        }
        buffer.append(postfix)
        return buffer
    }
}

class TableRow(
    var columns: List<String> = emptyList()
) {
    constructor(vararg rows: Any?) : this() {
        columns = rows.map { it?.toString() ?: "" }
    }
}
