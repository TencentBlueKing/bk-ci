package com.tencent.bkrepo.common.api.collection

data class MutablePair<A, B>(
    var first: A,
    var second: B
) {

    /**
     * Returns string representation of the [Triple] including its [first], [second] values.
     */
    override fun toString(): String = "($first, $second)"

    fun setValue(first: A, second: B) {
        this.first = first
        this.second = second
    }
}
