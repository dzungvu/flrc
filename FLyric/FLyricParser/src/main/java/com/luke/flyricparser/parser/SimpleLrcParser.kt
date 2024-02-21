package com.luke.flyricparser.parser

import com.luke.flyricparser.FLRCParser
import com.luke.flyricparser.models.ArrayLyric
import com.luke.flyricparser.models.Lyric
import com.luke.flyricparser.models.LyricMetaData
import com.luke.flyricparser.utils.TimestampUtils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class SimpleLrcParser: LrcParser<Lyric> {

    fun parseSource(inputStream: InputStream): Lyric {
        val lyric = ArrayLyric()
        try {
            parseSource(lyric, inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return lyric
    }

    fun getMetaData(): LyricMetaData {
        return LyricMetaData(dataType = FLRCParser.FileDataType.LRC)
    }

    @Throws(IOException::class)
    override fun parseSource(lyric: Lyric, inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            line?.let {
                constructOneLine(lyric, it)
            }
        }
    }

    private val timePattern = Pattern.compile("(?<=^\\[)\\d{2}:\\d{2}\\.\\d{2}(?=\\])")
    private val contentPattern = Pattern.compile("(?<=\\[\\d{2}:\\d{2}\\.\\d{2}\\]).*$")


    fun constructOneLine(lyric: Lyric, line: String) {
        var time: Long = 0
        var content: String? = null
        val timeMatcher = timePattern.matcher(line)
        val timeString: String
        if (timeMatcher.find()) { // in case malformed
            timeString = timeMatcher.group(0) ?: ""
            time = TimestampUtils.string2Timestamp(timeString)
        } else {
            System.err.println("Time tag should format as [mm:ss:ll]")
            return
        }
        val contentMatcher = contentPattern.matcher(line)
        content = if (contentMatcher.find()) {
            contentMatcher.group(0)
        } else {
            System.err.println("No lyric content found in line > $line")
            return
        }
        if (content != null) {
            lyric.append(time, content)
        }
    }
}