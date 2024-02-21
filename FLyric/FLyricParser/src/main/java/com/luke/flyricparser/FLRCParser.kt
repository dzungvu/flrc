package com.luke.flyricparser

import com.luke.flyricparser.models.EnhancedLyricData
import com.luke.flyricparser.models.ILyricData
import com.luke.flyricparser.models.SimpleLyricData
import com.luke.flyricparser.parser.EnhancedLrcParser
import com.luke.flyricparser.parser.SimpleLrcParser
import java.io.InputStream

class FLRCParser(private val builder: Builder) {

    companion object {
        private const val TAG = "FLRCParser"
    }

    private val simpleLrcParser by lazy { SimpleLrcParser() }
    private val enhancedLrcParser by lazy { EnhancedLrcParser() }

    private val fileDataType: FileDataType = builder.fileDataType

    fun parseSource(inputStream: InputStream): ILyricData {
        when (fileDataType) {
            FileDataType.ELRC -> {
                val lyric = enhancedLrcParser.parseSource(inputStream)
                val lyricMetaData = enhancedLrcParser.getMetaData()
                return EnhancedLyricData(metaData = lyricMetaData, lyric = lyric)
            }

            FileDataType.LRC -> {
                val lyric = simpleLrcParser.parseSource(inputStream)
                val lyricMetaData = simpleLrcParser.getMetaData()
                return SimpleLyricData(metaData = lyricMetaData, lyric = lyric)
            }

            FileDataType.AUTO_DETECT -> {
                val simpleLyric = simpleLrcParser.parseSource(inputStream)
                val enhancedLyric = enhancedLrcParser.parseSource(inputStream)

                if (enhancedLyric != null) {
                    val lyricMetaData = enhancedLrcParser.getMetaData()
                    return EnhancedLyricData(metaData = lyricMetaData, lyric = enhancedLyric)
                } else if (simpleLyric != null) {
                    val lyricMetaData = simpleLrcParser.getMetaData()
                    return SimpleLyricData(metaData = lyricMetaData, lyric = simpleLyric)
                } else {
                    throw IllegalArgumentException("Failed to parse the file")
                }
            }
        }
    }


    /**
     * Builder class for [FLRCParser]
     * @property fileDataType the type of file data to parse
     */
    class Builder {

        var fileDataType: FileDataType = FileDataType.AUTO_DETECT
            private set

        fun setFileDataType(fileDataType: FileDataType): Builder {
            this.fileDataType = fileDataType
            return this
        }


        fun build(): FLRCParser {
            return FLRCParser(this)
        }


    }

    enum class FileDataType {
        ELRC, LRC, AUTO_DETECT
    }
}