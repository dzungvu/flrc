package com.luke.flyricparser.parser

import android.util.Log
import com.luke.flyricparser.models.LyricData
import com.luke.flyricparser.utils.LyricUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class FLRCParser {

    suspend fun parseSource(inputStream: InputStream, onDone: (LyricData) -> Unit): LyricData {
        Log.d("FLRCParser", "Start parsing")
        val lyricData = LyricData()
        val job = CoroutineScope(Dispatchers.IO).launch {
            val reader = BufferedReader(InputStreamReader(inputStream))
            var lineText: String?
            var lineIndex = 0

            while (reader.readLine().also { lineText = it } != null) {
                lineText?.let {
                    constructOneLine(it, lineIndex++)?.let { lyrics ->
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


    private fun constructOneLine(line: String, lineIndex: Int): ArrayList<LyricData.Lyric>? {
        return LyricUtil.parseLine(line, lineIndex)
    }
}