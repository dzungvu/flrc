package com.luke.flyricui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Scroller
import androidx.annotation.RawRes
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import com.luke.flyricparser.models.LyricData
import com.luke.flyricparser.parser.FLRCParser
import com.luke.flyricui.utils.FLyricUIUtil.normalize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FLyricUIView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    //Lyric parser. This support SimpleLRC and EnhancedLRC
    private val lrcParser = FLRCParser()

    private var normalStaticLayouts = mutableListOf<StaticLayout>()
    private var highlightedStaticLayouts = mutableListOf<StaticLayout>()

    private val startOffset = 0f

    //region view setup
    private var textAlignMode = Layout.Alignment.ALIGN_CENTER
    private var normalTextColor = Color.WHITE
    private var highlightedTextColor = Color.RED
    private var fontSize = 16f
    private var mTypeface: Typeface? = null
    //endregion

    //view util value

    //endregion

    //region lyric data
    private var lyricData: LyricData? = null
    //endregion

    //region handle karaoke animation
    var animator: ValueAnimator? = null
    private val textPaint: TextPaint by lazy {
        TextPaint().apply {
            isAntiAlias = true
            color = normalTextColor
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                fontSize,
                context.resources.displayMetrics
            )
            typeface = mTypeface
        }
    }
    private val highlightedTextPaint: TextPaint by lazy {
        TextPaint(textPaint).apply {
            color = highlightedTextColor
        }
    }

    //region timeline
    private var currentLine = 0
    private var currentWord: LyricData.Word? = null
    private var currentTime = 0L
    //endregion

    //region smooth scroll
    private var dampingRatioForLyric: Float = SpringForce.DAMPING_RATIO_LOW_BOUNCY
    private var dampingRatioForViewPort: Float = SpringForce.DAMPING_RATIO_NO_BOUNCY
    private var stiffnessForLyric: Float = SpringForce.STIFFNESS_LOW
    private var stiffnessForViewPort: Float = SpringForce.STIFFNESS_VERY_LOW
    private var mCurrentOffset = 0f

    private var animateProgress = 0f
    private var animateStartOffset = 0f
    private var animateTargetOffset = 0f

    private val viewPortSpringAnimator = springAnimationOf(
        getter = { mViewPortOffset },
        setter = { value ->
//            if (!isShowTimeline.value && !isTouching && !isFling) {
            mViewPortOffset = value
            invalidate()
//            }
        }
    ).withSpringForceProperties {
        dampingRatio = dampingRatioForViewPort
        stiffness = stiffnessForViewPort
        finalPosition = 0f
    }

    private val progressSpringAnimator = springAnimationOf(
        getter = { mCurrentOffset },
        setter = { value ->
            animateProgress = normalize(animateStartOffset, animateTargetOffset, value)
            mCurrentOffset = value

//            if (!isShowTimeline.value && !isTouching && !isFling) {
            viewPortSpringAnimator.animateToFinalPosition(animateTargetOffset)
//            }
            invalidate()
        }
    ).withSpringForceProperties {
        dampingRatio = dampingRatioForLyric
        stiffness = stiffnessForLyric
        finalPosition = 0f
    }

    //region handle touch event
    private val scroller = Scroller(context)
    private val offsetKeeper = LinkedHashMap<Int, Float>()
    private val heightKeeper = LinkedHashMap<Int, Float>()
    private val gestureDetector by lazy {
        GestureDetector(context, mSimpleOnGestureListener)
    }
    private var mViewPortOffset = 0f
    private var isTouching = false
    private var isFling = false

    private val minOffset: Float
        get() = lyricData?.lyrics?.let {
            getOffset(it.size - 1)
        } ?: 0f

    private var maxOffset = getOffset(0)

    private val mSimpleOnGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
//            if (hasLrc() && onPlayClickListener != null) {
            if (hasLrc()) {
                scroller.forceFinished(true)
//                removeCallbacks(hideTimelineRunnable)
                isTouching = true
                invalidate()
                return true
            }
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
//            if (hasLrc()) {
            // 如果没显示 Timeline 的时候，distanceY 一段距离后再显示时间线
//                if (!isShowTimeline.value && abs(distanceY) >= 10) {
            // 滚动显示时间线
//                    isShowTimeline.value = true
//                }
            mViewPortOffset += -distanceY
            mViewPortOffset = mViewPortOffset.coerceIn(minOffset, maxOffset)
//            Log.d("FLyricUIView", "onScroll: $mViewPortOffset")
            invalidate()
            return true
//            }
//            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (hasLrc()) {
                scroller.fling(
                    0, mViewPortOffset.toInt(), 0,
                    velocityY.toInt(), 0, 0,
//                    getOffset(lyricEntryList.size - 1).toInt(),
//                    getOffset(0).toInt()
                    minOffset.toInt(), maxOffset.toInt()
                )
                isFling = true
                return true
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }
    //endregion


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FLyricUIView,
            0, 0
        ).apply {
            try {
                textAlignMode = when (getInt(R.styleable.FLyricUIView_textAlignMode, 1)) {
                    0 -> Layout.Alignment.ALIGN_NORMAL
                    1 -> Layout.Alignment.ALIGN_CENTER
                    2 -> Layout.Alignment.ALIGN_OPPOSITE
                    else -> Layout.Alignment.ALIGN_CENTER
                }
                normalTextColor = getColor(R.styleable.FLyricUIView_normalTextColor, Color.WHITE)
                highlightedTextColor =
                    getColor(R.styleable.FLyricUIView_highlightedTextColor, Color.RED)
                fontSize = getDimension(R.styleable.FLyricUIView_textSize, fontSize)
//                mTypeface = getString(R.styleable.FLyricUIView_typeface)?.let {
//                    Typeface.createFromAsset(context.assets, it)
//                }
            } finally {
                recycle()
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            mViewPortOffset = scroller.currY.toFloat()
            invalidate()
        }
        if (isFling && scroller.isFinished) {
            isFling = false
//            if (hasLrc() && !isTouching) {
//                adjustCenter()
//                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
//            }
        }
    }

    fun setLyricData(@RawRes res: Int) {
        Log.d("FLyricUIView", "setLyricData: $res")
        CoroutineScope(Dispatchers.IO).launch {
            resources.openRawResource(res).use {
                lrcParser.parseSource(it) {
                    setLyricData(it)
                    invalidate()
                }
            }
        }
    }

    private fun setLyricData(lyricData: LyricData): Boolean {
        this.lyricData = lyricData
        initStaticLayoutsForLyric(lyricData)
        return true
    }

    private fun findLyricLine(time: Long): Int {
        lyricData?.lyrics?.let { lyricEntryList ->
            var left = 0
            var right = lyricEntryList.size

            while (left <= right) {
                val middle = (left + right) / 2
                val middleTime = lyricEntryList[middle].startMs
                if (time < middleTime) {
                    right = middle - 1
                } else {
                    if (middle + 1 >= lyricEntryList.size || time < lyricEntryList[middle + 1].startMs) {
                        return middle
                    }
                    left = middle + 1
                }
            }
        }
        return 0
    }

    private fun findLyricWord(time: Long, line: Int): LyricData.Word? {
        lyricData?.lyrics?.get(line)?.words?.let { wordEntryList ->
            if (wordEntryList.isEmpty()) {
                return null
            }

            var left = 0
            var right = wordEntryList.size

            while (left <= right) {
                val middle = (left + right) / 2
                val middleTime = wordEntryList[middle].startMs
                if (time < middleTime) {
                    right = middle - 1
                } else {
                    if (middle + 1 >= wordEntryList.size || time < wordEntryList[middle + 1].startMs) {
                        return wordEntryList[middle]
                    }
                    left = middle + 1
                }
            }
        }
        return null
    }

    private fun initStaticLayoutsForLyric(lyricData: LyricData) {
        val spaceWidth = textPaint.measureText(" ")
        var yOffset = 0f
        val width = width - paddingLeft - paddingRight
        lyricData.lyrics.forEachIndexed { index, lyric ->
            var lyricLine = 0 // 0 means single line, greater than 0 means multi line (initial value is 0)


            val text = lyric.content
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(textAlignMode)
                .setLineSpacing(1f, 1f)
                .setIncludePad(true)
                .build()
            lyric.width = textPaint.measureText(lyric.content)
            normalStaticLayouts.add(staticLayout)
            val highlightedStaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, highlightedTextPaint, width)
                    .setAlignment(textAlignMode)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(true)
                    .build()
            highlightedStaticLayouts.add(highlightedStaticLayout)

            offsetKeeper[index] = yOffset
            heightKeeper[index] = staticLayout.height.toFloat()

            Log.d("FLyricUIView", "offset for ${getLineContent(index)}: ${offsetKeeper[index]}")
            yOffset += staticLayout.height

            //region calculate for word
            if (lyric.words.isNotEmpty()) {
                var wordOffset = getStartTextOffsetInStaticLayout(
                    staticLayout = staticLayout,
                    lyric = lyric
                )
                lyric.words.forEachIndexed { wordIndex, it ->
                    val wordWidth = staticLayout.paint.measureText(it.content)
                    it.endMs?.let { endMs ->
                        val wordDuration = endMs - it.startMs
                        it.msPerPx = wordWidth / wordDuration
                    }

                    it.wordOffset = wordOffset
                    it.wordInLine = lyricLine
                    wordOffset += (wordWidth)

                    // Check if the next word can fit in the current line
                    // if not, move to the next line
                    if (wordIndex < lyric.words.size - 1) {
                        val nextWordWidth =
                            staticLayout.paint.measureText(lyric.words[wordIndex + 1].content)
                        if (wordOffset + nextWordWidth > width) {
                            lyricLine += 1
                            wordOffset = getStartTextOffsetInStaticLayout(
                                staticLayout = staticLayout,
                                lyric = lyric.copy(
                                    width = textPaint.measureText(
                                        joinFromIndex(
                                            lyric.words.map { it.content },
                                            wordIndex
                                        )
                                    )
                                )
                            )
                        }
                    }

//                    if(wordOffset > width) {
//                        lyricLine += 1
//
//                        wordOffset = getStartTextOffsetInStaticLayout(
//                            staticLayout = staticLayout,
//                            lyric = lyric.copy(
//                                width = textPaint.measureText(joinFromIndex(lyric.words.map { it.content }, wordIndex))
//                            )
//                        )
//                    }
                }
            }
        }
    }

    private fun getStartTextOffsetInStaticLayout(
        staticLayout: StaticLayout,
        lyric: LyricData.Lyric
    ): Float {
        val layoutWidth = staticLayout.width
        val textWidth = lyric.width
        return when (staticLayout.alignment) {
            null,
            Layout.Alignment.ALIGN_NORMAL -> {
                0f
            }

            Layout.Alignment.ALIGN_CENTER -> {
                if (layoutWidth > textWidth) {
                    (layoutWidth - textWidth) / 2
                } else {
                    0f
                }
            }

            Layout.Alignment.ALIGN_OPPOSITE -> {
                layoutWidth - textWidth
            }
        }
    }

    private fun getLineContent(line: Int): String {
        return lyricData?.lyrics?.get(line)?.content ?: ""
    }

    fun hasLrc(): Boolean {
        return lyricData?.lyrics?.isNotEmpty() == true
    }

    private fun getOffset(line: Int): Float {
        return if (line in 0 until offsetKeeper.size) {
            startOffset - (offsetKeeper[line] ?: 0f)
        } else {
            0f
        }
    }

    private fun smoothScrollTo(line: Int) {
        if (!isTouching) {
            val offset = getOffset(line)
            animateStartOffset = mCurrentOffset
            animateTargetOffset = offset
            progressSpringAnimator.animateToFinalPosition(offset)
            Log.d("FLyricUIView", "current highlight line: $line")
            Log.d(
                "FLyricUIView",
                "current highlight text: ${lyricData?.lyrics?.get(line)?.content}"
            )
        }
    }

    fun startKaraokeAnimation(duration: Long? = null) {
        val endValue = lyricData?.lyrics?.last()?.let { lastLyric ->
            if (lastLyric.words.isNotEmpty()) {
                lastLyric.words.last().endMs
            } else {
                lastLyric.endMs ?: duration ?: (lastLyric.startMs + 10_000)
            }
        } ?: duration ?: 0

        animator = ValueAnimator.ofFloat(0f, endValue.toFloat()).apply {
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                setCurrentTime(animatedValue.toLong())
            }
            interpolator = LinearInterpolator()
            setDuration(endValue)
        }
        animator?.start()

        Log.d("FLyricUIView", "startKaraokeAnimation")
    }

    fun stopKaraokeAnimation() {
        animator?.cancel()
        animator = null
        Log.d("FLyricUIView", "stopKaraokeAnimation")
    }

    fun pauseKaraokeAnimation() {
        animator?.pause()
        Log.d("FLyricUIView", "pauseKaraokeAnimation")
    }

    fun resumeKaraokeAnimation() {
        animator?.resume()
        Log.d("FLyricUIView", "resumeKaraokeAnimation")
    }

    fun isKaraokeAnimationRunning(): Boolean {
        return animator?.isRunning == true
    }

    fun seekAnimationToValue(value: Long) {
        animator?.currentPlayTime = value
    }


    private fun setCurrentTime(currentTime: Long) {
        this.currentTime = currentTime
        val index = findLyricLine(currentTime)
        if (index != currentLine && index != -1) {
            currentLine = index
            smoothScrollTo(index)
        }
        currentWord = findLyricWord(currentTime, currentLine)
//        Log.d(
//            "FLyricUIView",
//            "setCurrentTime: $currentTime with line start time: ${lyricData?.lyrics?.get(index)?.startMs}"
//        )
        invalidate()

    }

    private fun scrollToLine(index: Int) {
        val offset = offsetKeeper[index] ?: 0f
        mViewPortOffset = -offset
        mViewPortOffset = mViewPortOffset.coerceIn(minOffset, maxOffset)
        Log.d("FLyricUIView", "current highlight line: $index")
        Log.d("FLyricUIView", "current highlight offset: $mViewPortOffset")
        Log.d("FLyricUIView", "current highlight text: ${lyricData?.lyrics?.get(index)?.content}")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(0f, mViewPortOffset)
        lyricData?.lyrics?.let { lyrics ->
            lyrics.forEachIndexed { index, lyric ->
                if (index == currentLine) {
//                    Log.d(
//                        "FLyricUIView",
//                        "listWordSize = ${lyric.words.size} currentWord: ${currentWord?.content} curTime: $currentTime wordStartMs: ${currentWord?.startMs} curWordMsPx: ${currentWord?.msPerPx}"
//                    )
                    if (lyric.words.isEmpty()) {
                        val staticLayout = highlightedStaticLayouts[index]
                        canvas.save()
                        canvas.translate(0f, offsetKeeper[index] ?: 0f)
                        staticLayout.draw(canvas)
                        canvas.restore()
                    } else {
                        currentWord?.let { currentWord ->
                            val staticLayout = normalStaticLayouts[index]
                            val highlightedStaticLayout = highlightedStaticLayouts[index]

                            val highlightedRect = calculateOffsetForHighlightEnd(
                                currentTime = currentTime,
                                currentWord = currentWord,
                            )

                            // If the current highlighted line index is greater than 0,
                            // highlight all the previous lines
                            if (currentWord.wordInLine != 0) {
                                canvas.save()
                                canvas.translate(0f, offsetKeeper[index] ?: 0f)
                                canvas.clipRect(
                                    0f,
                                    0f,
                                    width.toFloat(),
                                    highlightedRect.top.toFloat()
                                )
                                highlightedStaticLayout.draw(canvas)
                                canvas.restore()
                            }

                            // If the current highlighted line index is less than the total line count,
                            // draw all the next lines with non-highlighted text
                            if(currentWord.wordInLine < staticLayout.lineCount - 1) {
                                canvas.save()
                                canvas.translate(0f, offsetKeeper[index] ?: 0f)
                                canvas.clipRect(
                                    0f,
                                    highlightedRect.bottom.toFloat(),
                                    width.toFloat(),
                                    height.toFloat()
                                )
                                staticLayout.draw(canvas)
                                canvas.restore()
                            }

                            canvas.save()
                            canvas.translate(0f, offsetKeeper[index] ?: 0f)
                            canvas.clipRect(
                                highlightedRect.left.toFloat(),
                                highlightedRect.top.toFloat(),
                                highlightedRect.right.toFloat(),
                                highlightedRect.bottom.toFloat()
                            )
                            highlightedStaticLayout.draw(canvas)
                            canvas.restore()

                            canvas.save()
                            canvas.translate(0f, offsetKeeper[index] ?: 0f)
                            canvas.clipRect(
                                highlightedRect.right.toFloat(),
                                highlightedRect.top.toFloat(),
                                width.toFloat(),
                                highlightedRect.bottom.toFloat()
                            )
                            staticLayout.draw(canvas)
                            canvas.restore()
                        }
                    }
                } else {
                    val staticLayout = normalStaticLayouts[index]
                    canvas.save()
                    canvas.translate(0f, offsetKeeper[index] ?: 0f)
                    staticLayout.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
//                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    private fun calculateOffsetForHighlightEnd(
        currentTime: Long,
        currentWord: LyricData.Word,
    ): Rect {
        val startHighlightOffset = 0f
        val endHighlightOffset =
            currentWord.wordOffset + ((currentTime - currentWord.startMs) * currentWord.msPerPx)

        val oneLineHeight = -1 * textPaint.fontMetrics.top + textPaint.fontMetrics.bottom
        val topHighlightOffset = (currentWord.wordInLine) * oneLineHeight

        // Calculate the line index based on the word offset

        val bottomHighlightOffset = (currentWord.wordInLine + 1) * oneLineHeight
        Log.d(
            "FLyricUIView",
            "curWord: ${currentWord.content} curTime: $currentTime wordStartMs: ${currentWord.startMs} curWordMsPx: ${currentWord.msPerPx} curWordOffset: ${currentWord.wordOffset} - currentScreenWidth: ${
                getScreenWidth(
                    context
                )
            } ==> highlightEnd: $endHighlightOffset, verticalOffset: $bottomHighlightOffset"
        )

        Log.d(
            "FLyricUIView",
            "textPaint.fontMetrics.bottom: ${textPaint.fontMetrics.bottom} textPaint.fontMetrics.top: ${textPaint.fontMetrics.top}"
        )

        Log.d(
            "FLyricUIView",
            "curWord: ${currentWord.content} wordInternalLine: ${currentWord.wordInLine} wordOffset = ${currentWord.wordOffset}"
        )
        return Rect(
            startHighlightOffset.toInt(),
            topHighlightOffset.toInt(),
            endHighlightOffset.toInt(),
            bottomHighlightOffset.toInt()
        )
    }

    private fun reset() {
        scroller.forceFinished(true)
        isTouching = false
        isFling = false
        lyricData = null
        normalStaticLayouts.clear()
        highlightedStaticLayouts.clear()
        offsetKeeper.clear()
        heightKeeper.clear()
        mCurrentOffset = 0f
        mViewPortOffset = 0f
        currentLine = 0
        invalidate()
    }

}


fun getScreenWidth(context: Context): Int {
    val displayMetrics = DisplayMetrics()
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

fun joinFromIndex(list: List<String>, startIndex: Int): String {
    return if (startIndex in list.indices) {
        list.subList(startIndex, list.size).joinToString(" ")
    } else {
        "" // Return an empty string if the startIndex is out of bounds
    }
}