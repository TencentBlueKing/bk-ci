package com.tencent.devops.common.util

import com.github.houbb.heaven.util.lang.CharUtil
import com.github.houbb.heaven.util.lang.StringUtil
import com.github.houbb.segment.support.segment.result.impl.SegmentResultHandlers
import com.taptap.pinyin.ResourceLoad
import com.taptap.pinyin.Word
import com.taptap.pinyin.analyzer.WordAnalyzer
import com.taptap.pinyin.utils.Utils
import java.util.HashMap
import java.util.StringJoiner

object PinyinUtil {

    private val words: HashMap<String, Word> = ResourceLoad.loadCedict()
    private val wordAnalyzer = WordAnalyzer.newInstance()

    /**
     *  com.taptap.pinyin.PinyinPlus#to(java.lang.String, boolean) 修改此方法逻辑, 改为词组之间不带分隔符
     *  返回拼音字符串, 不存在拼音的返回原字符
     */
    @Suppress("NestedBlockDepth")
    fun toPinyin(text: String): String {
        if (StringUtil.isBlank(text)) return text
        var word = words[text]
        return if (word != null) {
            Utils.trim(word.pinyinNoTone)
        } else {
            val joiner = StringJoiner("")
            val segmentResult = wordAnalyzer.segment(text, SegmentResultHandlers.word())
            for (segmentStr in segmentResult) {
                word = words[segmentStr]
                if (word != null) {
                    joiner.add(Utils.trim(word.pinyinNoTone))
                } else {
                    val characterList = StringUtil.toCharacterList(segmentStr)
                    for (character in characterList) {
                        if (CharUtil.isChinese(character)) {
                            word = words[character.toString()]
                            joiner.add(word!!.pinyinNoTone)
                        } else {
                            joiner.add(character.toString())
                        }
                    }
                }
            }
            joiner.toString()
        }
    }
}
