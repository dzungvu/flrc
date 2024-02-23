package com.luke.flyricui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
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

    private val lrcParser = FLRCParser()

//    private var staticLayout: StaticLayout? = null
//    private var highlightedStaticLayout: StaticLayout? = null

    private var normalStaticLayouts = mutableListOf<StaticLayout>()
    private var highlightedStaticLayouts = mutableListOf<StaticLayout>()

    private var textWidth = 0f

    private val startOffset = 0f

    //region lyric data
    private var lyricData: LyricData? = null
    //endregion

    //region handle karaoke animation
    private var textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f,
            context.resources.displayMetrics
        )
        typeface = Typeface.DEFAULT
    }
    private var highlightedTextPaint: TextPaint = TextPaint(textPaint).apply {
        color = Color.RED
    }
    private var currentLine = 0
    private var currentWordIndex = 0
    private var currentWord: LyricData.Word? = null
    private var currentTime = 0L
    private var text: String? = null
    private var highlightEnd = 0f
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
    private var lastTouchY = 0f
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
            if(hasLrc()) {
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
                launch(Dispatchers.Main) {
//                    startKaraokeAnimation()
                }
            }
        }
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

    fun setLyricData(lyricData: LyricData): Boolean {
        this.lyricData = lyricData
        initStaticLayoutsForLyric(lyricData)
        return true
    }

    private fun initStaticLayoutsForLyric(lyricData: LyricData) {
        var spaceWidth = textPaint.measureText(" ")
        var yOffset = 0f
        val width = width - paddingLeft - paddingRight
        lyricData.lyrics.forEachIndexed { index, it ->
            val text = it.content
            val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1f)
                .setIncludePad(true)
                .build()
            normalStaticLayouts.add(staticLayout)
            val highlightedStaticLayout =
                StaticLayout.Builder.obtain(text, 0, text.length, highlightedTextPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(true)
                    .build()
            highlightedStaticLayouts.add(highlightedStaticLayout)

            offsetKeeper[index] = yOffset
            heightKeeper[index] = staticLayout.height.toFloat()

            Log.d("FLyricUIView", "offset for ${getLineContent(index)}: ${offsetKeeper[index]}")
            yOffset += staticLayout.height

            //region calculate for word
            if(it.words.isNotEmpty()) {
                var wordOffset = 0f
                it.words.forEach {
                    val wordWidth = textPaint.measureText(it.content)
                    it.endMs?.let { endMs ->
                        val wordDuration = endMs - it.startMs
                        it.msPerPx = wordWidth / wordDuration
                    }

                    it.wordOffset = spaceWidth * it.index + wordOffset
                    wordOffset += wordWidth + spaceWidth
                }
            }
        }
    }

    private fun getLineContent(line: Int): String {
        return lyricData?.lyrics?.get(line)?.content ?: ""
    }

    private fun calculateOffsetItem(itemHeight: Int, dividerHeight: Int): Float {
        return (itemHeight - dividerHeight).toFloat()
    }

    fun hasLrc(): Boolean {
        return lyricData?.lyrics?.isNotEmpty() == true
    }

    private fun getOffset(line: Int): Float {
        return if(line in 0 until offsetKeeper.size) {
            startOffset - (offsetKeeper[line] ?: 0f)
        } else {
            0f
        }
    }

    private fun smoothScrollTo(line: Int) {
        if(!isTouching) {
            val offset = getOffset(line)
            animateStartOffset = mCurrentOffset
            animateTargetOffset = offset
            progressSpringAnimator.animateToFinalPosition(offset)
            Log.d("FLyricUIView", "current highlight line: $line")
            Log.d("FLyricUIView", "current highlight text: ${lyricData?.lyrics?.get(line)?.content}")
        }
    }

    fun startKaraokeAnimation() {
        val animator = ValueAnimator.ofFloat(0f, 277_000f)
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            setCurrentTime(animatedValue.toLong())
        }
        animator.interpolator = LinearInterpolator()
        animator.duration = 277_000
        animator.start()
        Log.d("FLyricUIView", "startKaraokeAnimation")
    }

    fun setText(text: String): Float {
        this.text = text
        textWidth = textPaint.measureText(text)
        updateStaticLayout()
        return textWidth
    }

    private fun setCurrentTime(currentTime: Long) {

        this.currentTime = currentTime
        val index = findLyricLine(currentTime)
        if (index != currentLine && index != -1) {
            currentLine = index
            smoothScrollTo(index)
        }
        currentWord = findLyricWord(currentTime, currentLine)
        Log.d("FLyricUIView", "setCurrentTime: $currentTime with line start time: ${lyricData?.lyrics?.get(index)?.startMs}")
        invalidate()

    }

    //get current time
    fun getCurrentTime(): Long {
        return lyricData?.lyrics?.get(currentLine)?.startMs ?: 0
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

    fun setHighlightEnd(end: Float) {
        this.highlightEnd = end
        invalidate()
    }

    private fun updateStaticLayout() {
//        text?.let {
//            val width = width - paddingLeft - paddingRight
//            staticLayout = StaticLayout.Builder.obtain(it, 0, it.length, textPaint, width)
//                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
//                .setLineSpacing(1f, 1f)
//                .setIncludePad(true)
//                .build()
//            highlightedStaticLayout =
//                StaticLayout.Builder.obtain(it, 0, it.length, highlightedTextPaint, width)
//                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
//                    .setLineSpacing(1f, 1f)
//                    .setIncludePad(true)
//                    .build()
//            invalidate() // Redraw the view
//        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(0f, mViewPortOffset)
        lyricData?.lyrics?.let { lyrics ->
            lyrics.forEachIndexed { index, lyric ->
                if(index == currentLine) {
                    Log.d("FLyricUIView", "listWordSize = ${lyric.words.size} currentWord: ${currentWord?.content} curTime: $currentTime wordStartMs: ${currentWord?.startMs} curWordMsPx: ${currentWord?.msPerPx}")
                    if(lyric.words.isEmpty()) {
                        val staticLayout = highlightedStaticLayouts[index]
                        canvas.save()
                        canvas.translate(0f, offsetKeeper[index] ?: 0f)
                        staticLayout.draw(canvas)
                        canvas.restore()
                    } else {
                        currentWord?.let { currentWord ->
                            highlightEnd = currentWord.wordOffset + ((currentTime - currentWord.startMs.toFloat()) * currentWord.msPerPx)
                            Log.d("FLyricUIView", "curWord: ${currentWord.content} curTime: $currentTime wordStartMs: ${currentWord.startMs} curWordMsPx: ${currentWord.msPerPx} ==> highlightEnd: $highlightEnd")
                            val staticLayout = normalStaticLayouts[index]
                            val highlightedStaticLayout = highlightedStaticLayouts[index]
                            canvas.save()
                            canvas.translate(0f, offsetKeeper[index] ?: 0f)
                            canvas.clipRect(0f, 0f, highlightEnd, height.toFloat())
                            highlightedStaticLayout.draw(canvas)
                            canvas.restore()

                            canvas.save()
                            canvas.translate(0f, offsetKeeper[index] ?: 0f)
                            canvas.clipRect(highlightEnd, 0f, width.toFloat(), height.toFloat())
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
//                val staticLayout = normalStaticLayouts[index]
//                val highlightedStaticLayout = highlightedStaticLayouts[index]
//                canvas.save()
//                canvas.translate(0f, staticLayout.height * index.toFloat())
//                staticLayout.draw(canvas)
//                canvas.restore()
//                canvas.save()
//                canvas.clipRect(0f, 0f, highlightEnd, staticLayout.height.toFloat())
//                canvas.translate(0f, staticLayout.height * index.toFloat())
//                highlightedStaticLayout.draw(canvas)
//                canvas.restore()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            isTouching = false
            if (hasLrc() && !isFling) {
                // TODO 应该为Timeline独立设置一个Enable开关, 这样就可以不需要等待Timeline消失
//                postDelayed(hideTimelineRunnable, TIMELINE_KEEP_TIME)
            }
        }
        return gestureDetector.onTouchEvent(event)
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
        text = null
        textWidth = 0f
        invalidate()
    }

}
