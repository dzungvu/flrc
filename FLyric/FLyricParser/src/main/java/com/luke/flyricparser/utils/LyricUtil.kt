package com.luke.flyricparser.utils

import android.text.StaticLayout
import android.text.TextUtils
import android.text.format.DateUtils
import com.luke.flyricparser.models.LyricData
import java.util.regex.Matcher
import java.util.regex.Pattern

object LyricUtil {
    private val PATTERN_LINE = Pattern.compile("((\\[\\d\\d:\\d\\d\\.\\d{2,3}])+)(.+)")
    private val PATTERN_TIME = Pattern.compile("\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})]")

    private val PATTERN_WORD = Pattern.compile("((<\\d{2}:\\d{2}\\.\\d{1,3}>)+)")

    fun parseLine(line: String, lineIndex: Int): ArrayList<LyricData.Lyric>? {
        val entryList = ArrayList<LyricData.Lyric>()
        try {
            var lyricLine = line
            if (TextUtils.isEmpty(lyricLine)) {
                return null
            }
            lyricLine = lyricLine.trim { it <= ' ' }

            val lineMatcher = PATTERN_LINE.matcher(lyricLine)
            if (!lineMatcher.matches()) {
                return null
            }
            val times = lineMatcher.group(1) ?: "0"
            val text = lineMatcher.group(3) ?: ""


            val timeMatcher = PATTERN_TIME.matcher(times)
            if (timeMatcher.groupCount() < 3) {
                return null
            }

            while (timeMatcher.find()) {
                val min = timeMatcher.group(1)!!.toLong()
                val sec = timeMatcher.group(2)!!.toLong()
                val milString = timeMatcher.group(3)!!
                var mil = milString.toLong()

                when (milString.length) {
                    1 -> mil *= 100
                    2 -> mil *= 10
                    4 -> mil /= 10
                    5 -> mil /= 100
                    6 -> mil /= 1000
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil

                entryList.add(
                    splitWordsInLyricLine(
                        LyricData.Lyric(
                            lineNumber = lineIndex,
                            rawValue = line,
                            type = LyricData.LyricType.SIMPLE_LRC, // init value, this will be updated later
                            startMs = time,
                            endMs = null,
                            content = text, // init value, this will be updated later
                            words = arrayListOf() // init value, this will be updated later
                        )
                    )
                )
            }
            return entryList
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun splitWordsInLyricLine(data: LyricData.Lyric): LyricData.Lyric {
        val entryList: ArrayList<LyricData.Word> = ArrayList()

        val lyricContent = data.content.trim()
        val splitStrings: MutableList<String> = PATTERN_WORD.split(lyricContent).toMutableList()
        val matcher: Matcher = PATTERN_WORD.matcher(lyricContent)

        var wordIndex = 0
        val stringBuilder = StringBuilder()

        if (splitStrings.size == 1) {
            return data.copy(
                type = LyricData.LyricType.SIMPLE_LRC,
                words = arrayListOf()
            )
        }

        while (matcher.find()) {
            if (matcher.start() == 0 && wordIndex == 0) {
                //If matcher starts at 0, it means the first word is time,
                //so the first element in splitStrings is empty and should be removed
                splitStrings.removeFirst()
            } else if (wordIndex == 0) {
                //If matcher is not starting at 0, it means the first word is not time,
                //so the first element in splitStrings is the word
                //and the start time of the word is the start time of the sentence
                splitStrings.removeFirst().let {
                    entryList.add(
                        LyricData.Word(
                            index = wordIndex,
                            startMs = data.startMs,
                            endMs = null,
                            rawValue = it,
                            content = it,
                            startInSentenceMs = 0,
                            msPerPx = 0f,
                            wordOffset = 0f,
                            wordInLine = 0, // init value, this will update later
                        )
                    )
                    stringBuilder.append(it)
                }
                wordIndex += 1
            }
            val timeString = matcher.group(0) ?: ""
            if (splitStrings.isNotEmpty()) {
                splitStrings.removeFirst().let { word ->
                    entryList.add(
                        LyricData.Word(
                            index = wordIndex,
                            startMs = TimestampUtils.string2Timestamp(
                                timeString.substring(
                                    1,
                                    timeString.length - 1
                                )
                            ),
                            endMs = null,
                            rawValue = timeString,
                            content = word,
                            startInSentenceMs = 0,
                            msPerPx = 0f,
                            wordOffset = 0f,
                            wordInLine = 0, // init value, this will be updated later
                        )
                    )

                    stringBuilder.append(word)
                    wordIndex += 1
                }
            } else {
                //if there is no word after the time, it means the time is the end time of the sentence
                //update the end time of the last word
                try {
                    entryList.get(entryList.size - 1).endMs = TimestampUtils.string2Timestamp(
                        timeString.substring(
                            1,
                            timeString.length - 1
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        //Update time for each word
        if (entryList.isNotEmpty()) {
            var startInSentenceMs = 0L
            for (i in 0 until entryList.size - 1) {
                entryList[i].endMs = entryList[i + 1].startMs
                entryList[i].startInSentenceMs = startInSentenceMs
                startInSentenceMs += entryList[i].endMs!! - entryList[i].startMs
            }
            entryList.last().apply {
                if (this.endMs == null) {
                    this.endMs = startMs + 1000
                }
                this.startInSentenceMs = startInSentenceMs
            }
        }

        return data.copy(
            type = LyricData.LyricType.ENHANCED_LRC,
            content = stringBuilder.toString(),
            words = entryList
        )
    }

    private fun splitOneLineByProgress(
        lineStartTime: Long,
        content: String,
        rawValue: String,
        lineIndex: Int
    ): LyricData.Lyric {
        val entryList: ArrayList<LyricData.Word> = ArrayList()

        val lyricContent = content.trim()
        val splitStrings: MutableList<String> = PATTERN_WORD.split(lyricContent).toMutableList()
        val matcher: Matcher = PATTERN_WORD.matcher(lyricContent)

        var wordIndex = 0
        val stringBuilder = StringBuilder()

        if (splitStrings.size == 1) {
            return LyricData.Lyric(
                lineNumber = lineIndex,
                rawValue = rawValue,
                type = LyricData.LyricType.SIMPLE_LRC,
                startMs = lineStartTime,
                endMs = null,
                content = lyricContent,
                words = arrayListOf()
            )
        }

        while (matcher.find()) {
            if (matcher.start() == 0 && wordIndex == 0) {
                //If matcher starts at 0, it means the first word is time,
                //so the first element in splitStrings is empty and should be removed
                splitStrings.removeFirst()
            } else if (wordIndex == 0) {
                //If matcher is not starting at 0, it means the first word is not time,
                //so the first element in splitStrings is the word
                //and the start time of the word is the start time of the sentence
                splitStrings.removeFirst().let {
                    entryList.add(
                        LyricData.Word(
                            index = wordIndex,
                            startMs = lineStartTime,
                            endMs = null,
                            rawValue = it,
                            content = it,
                            startInSentenceMs = 0,
                            msPerPx = 0f,
                            wordOffset = 0f,
                            wordInLine = 0, // init value, this will update later
                        )
                    )
                    stringBuilder.append(it)
                }
                wordIndex += 1
            }
            val timeString = matcher.group(0) ?: ""
            if (splitStrings.isNotEmpty()) {
                splitStrings.removeFirst().let { word ->
                    entryList.add(
                        LyricData.Word(
                            index = wordIndex,
                            startMs = TimestampUtils.string2Timestamp(
                                timeString.substring(
                                    1,
                                    timeString.length - 1
                                )
                            ),
                            endMs = null,
                            rawValue = timeString,
                            content = word,
                            startInSentenceMs = 0,
                            msPerPx = 0f,
                            wordOffset = 0f,
                            wordInLine = 0, // init value, this will be updated later
                        )
                    )

                    stringBuilder.append(word)
                    wordIndex += 1
                }
            } else {
                //if there is no word after the time, it means the time is the end time of the sentence
                //update the end time of the last word
                try {
                    entryList.get(entryList.size - 1).endMs = TimestampUtils.string2Timestamp(
                        timeString.substring(
                            1,
                            timeString.length - 1
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        //Update time for each word
        if (entryList.isNotEmpty()) {
            var startInSentenceMs = 0L
            for (i in 0 until entryList.size - 1) {
                entryList[i].endMs = entryList[i + 1].startMs
                entryList[i].startInSentenceMs = startInSentenceMs
                startInSentenceMs += entryList[i].endMs!! - entryList[i].startMs
            }
            entryList.last().apply {
                if (this.endMs == null) {
                    this.endMs = startMs + 1000
                }
                this.startInSentenceMs = startInSentenceMs
            }
        }

        return LyricData.Lyric(
            lineNumber = lineIndex,
            rawValue = content,
            type = LyricData.LyricType.ENHANCED_LRC,
            startMs = 0,
            endMs = null,
            content = stringBuilder.toString(),
            words = entryList
        )
    }
}