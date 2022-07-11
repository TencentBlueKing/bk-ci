package com.tencent.devops.common.util

import org.junit.jupiter.api.Test

class PinyinUtilTest {

    @Test
    fun test() {
        assert("chongzhiliushuixianzhuangtai TEST.テスト-jinめ_ceshi" ==
            PinyinUtil.toPinyin("重置流水线状态 TEST.テスト-進め_測試"))
        assert("zhongyao zhongda" == PinyinUtil.toPinyin("重要 重大"))
        assert(PinyinUtil.toPinyin("奇偶") == "jiou")
        assert(PinyinUtil.toPinyin("奇怪奇异") == "qiguaiqiyi")
        assert(PinyinUtil.toPinyin("屏风 屏障 屏蔽") == "pingfeng pingzhang pingbi")
        assert(PinyinUtil.toPinyin("屏息 屏气") == "bingxi bingqi")
    }
}
