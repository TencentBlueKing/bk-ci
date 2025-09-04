package com.tencent.devops.environment.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BinarySearchUtilsTest {
    @Test
    fun testBinarySearchIndexOrCeil() {
        // 1.奇数个元素列表
        testBinarySearchIndexOrCeilWithList(listOf(1, 3, 5, 7, 9))
        // 2.偶数个元素列表
        testBinarySearchIndexOrCeilWithList(listOf(1, 3, 5, 7, 9, 11))
    }

    private fun testBinarySearchIndexOrCeilWithList(list: List<Int>) {
        // 1.如果存在恰好相等的值，返回对应下标
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, 1)).isEqualTo(0)
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, 5)).isEqualTo(2)
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, 9)).isEqualTo(4)
        // 2.如果所有值比目标值小或列表为空，返回-1
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(emptyList(), 100)).isEqualTo(-1)
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, 100)).isEqualTo(-1)
        // 3.如果所有值比目标值大，返回0
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, -100)).isEqualTo(0)
        // 4.如果目标值位于首尾值之间但不存在恰好相等的值，返回与目标值距离最近且比目标值大的值的下标
        assertThat(BinarySearchUtils.binarySearchIndexOrCeil(list, 2)).isEqualTo(1)
    }
}
