package com.luke.flyricparser.parser

import android.util.Log
import com.luke.flyricparser.models.LyricData
import com.luke.flyricparser.utils.LyricUtil
import com.luke.flyricparser.utils.TimestampUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

class FLRCParser {

//    suspend fun parseSource(url: String): LyricData {
//        val inputStream = java.net.URL(url).openStream()
//        return parseSource(inputStream)
//    }

    suspend fun parseSource(inputStream: InputStream, onDone: (LyricData) -> Unit): LyricData {
        Log.d("FLRCParser", "Start parsing")
        val lyricData = LyricData()
        val job = CoroutineScope(Dispatchers.IO).launch {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var lineText: String?
            var lineIndex = 0

            while (reader.readLine().also { lineText = it } != null) {
                lineText?.let {
//                    constructOneLine(it, lineIndex++)?.let { lyricData.lyrics.add(it) }
                    constructOneLine2(it, lineIndex++)?.let { lyrics ->
                        lyricData.lyrics.addAll(lyrics)
                    }
                }
            }
            onDone.invoke(lyricData)
        }

        job.join()
        Log.d("FLRCParser", "Parsing finished")
        return lyricData
    }

    private val timePattern = Pattern.compile("(?<=^\\[)\\d{2}:\\d{2}\\.\\d{2}(?=\\])")
    private val contentPattern = Pattern.compile("(?<=\\[\\d{2}:\\d{2}\\.\\d{2}\\]).*$")


    private fun constructOneLine2(line: String, lineIndex: Int): ArrayList<LyricData.Lyric>? {
        return LyricUtil.parseLine(line, lineIndex)
    }

    private fun constructOneLine(line: String, lineIndex: Int): LyricData.Lyric? {
        var time: Long = 0
        var content: String? = null
        val timeMatcher = timePattern.matcher(line)
        val timeString: String
        if (timeMatcher.find()) { // in case malformed
            timeString = timeMatcher.group(0) ?: ""
            time = TimestampUtils.string2Timestamp(timeString)
        } else {
            System.err.println("Time tag should format as [mm:ss:ll]")
            return null
        }
        val contentMatcher = contentPattern.matcher(line)
        content = if (contentMatcher.find()) {
            contentMatcher.group(0)
        } else {
            System.err.println("No lyric content found in line > $line")
            return null
        }
        return if (content != null) {
            LyricData.Lyric(
                lineNumber = lineIndex,
                rawValue = line,
                type = LyricData.LyricType.SIMPLE_LRC,
                startMs = time,
                endMs = null,
                content = content,
                words = arrayListOf()
            )
        } else {
            null
        }
    }
}