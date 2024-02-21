package com.luke.flyricparser.parser

import com.luke.flyricparser.FLRCParser
import com.luke.flyricparser.models.EnhancedLyric
import com.luke.flyricparser.models.LyricMetaData
import com.luke.flyricparser.utils.TimestampUtils.string2Timestamp
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Matcher
import java.util.AbstractMap
import java.util.regex.Pattern


class EnhancedLrcParser: LrcParser<EnhancedLyric> {
    private val simpleParser = SimpleLrcParser()

    fun parseSource(inputStream: InputStream): EnhancedLyric {
        val lyric = EnhancedLyric()
        parseSource(lyric, inputStream)
        return lyric
    }

    fun getMetaData(): LyricMetaData {
        return LyricMetaData(dataType = FLRCParser.FileDataType.ELRC)
    }


    override fun parseSource(lyric: EnhancedLyric, inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            line?.let {
                simpleParser.constructOneLine(lyric, it)
                splitOneLineByProgress(lyric)
            }
        }
    }

    private fun splitOneLineByProgress(lyric: EnhancedLyric) {
        lyric.remove()?.let { entry ->
            val line: String = entry.value
            val splitReg = "<\\d{2}:\\d{2}\\.\\d{1,3}>"
            val pattern: Pattern = Pattern.compile(splitReg)
            val matcher: Matcher = pattern.matcher(line)
            val splitStrings: Array<String> = pattern.split(line)
            val stringBuilder = StringBuilder()
            var segStartIdx = 0
            var i = 0
            var timeString: String
            val progressArray: ArrayList<AbstractMap.SimpleEntry<Long, Int>> =
                ArrayList()
            for (seg in splitStrings) {
                if(matcher.groupCount() != 0 && i in 0 until matcher.groupCount()) {
                    timeString = matcher.group(i++) ?: ""
                    progressArray.add(
                        AbstractMap.SimpleEntry(
                            string2Timestamp(timeString.substring(1, timeString.length - 1)),
                            segStartIdx
                        )
                    )
                    stringBuilder.append(seg)
                    segStartIdx += seg.length
                }
            }
            lyric.append(progressArray)
            lyric.append(entry.key, stringBuilder.toString())
        }
    }
}