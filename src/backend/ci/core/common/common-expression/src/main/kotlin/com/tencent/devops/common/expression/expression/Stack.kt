package com.tencent.devops.common.expression.expression

class Stack {
    companion object {
        fun <T> ArrayDeque<T>.push(element: T) {
            this.addLast(element)
        }

        fun <T> ArrayDeque<T>.pop(): T {
            return this.removeLast()
        }

        fun <T> ArrayDeque<T>.peek(): T {
            return this.last()
        }
    }
}
