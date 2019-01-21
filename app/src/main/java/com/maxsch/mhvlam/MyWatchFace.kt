package com.maxsch.mhvlam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder
import android.view.WindowInsets
import java.lang.ref.WeakReference
import java.util.*

class MyWatchFace : CanvasWatchFaceService() {

    companion object {
        private const val INTERACTIVE_UPDATE_RATE_MS = 30000
        private const val MSG_UPDATE_TIME = 0
        private const val SONGS = "21:10"
        private const val LOOK = "22:20"
        private const val HAIR = "23:30"
        private const val KING = "00:40"
        private const val MARKET = "12:34"
        private var mXAlignment: Float = 0F
        private var isTwentyTwo: Boolean = false
    }

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        //Variables block
        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false

        private var mXOffset: Float = 0F
        private var mYOffset: Float = 0F

        private lateinit var mTimePaintMid: Paint
        private lateinit var mTextPaint: Paint
        private lateinit var mBackground: Bitmap
        private var mScale: Float = 0F

        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false
        private var mAmbient: Boolean = false

        private val mUpdateTimeHandler: Handler = EngineHandler(this)

        private val mTimeZoneReceiver: BroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            mBackground = getBitmapByDay()

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(false)
                    .build())

            val normalTypeface: Typeface = resources.getFont(R.font.ptp)

            mCalendar = Calendar.getInstance()

            val resources = this@MyWatchFace.resources
            mYOffset = resources.getDimension(R.dimen.digital_y_offset)

            mTimePaintMid = Paint().apply {
                typeface = normalTypeface
                isAntiAlias = false
                color = ContextCompat.getColor(applicationContext, R.color.digital_text_start)
            }
            mTextPaint = Paint().apply {
                typeface = normalTypeface
                isAntiAlias = false
                color = ContextCompat.getColor(applicationContext, R.color.date_text)
                textSize = resources.getDimension(R.dimen.digital_text_size)
            }
        }

        //calculating the scale of picture
        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            mScale = width / mBackground.width.toFloat()
            mBackground = Bitmap.createScaledBitmap(
                    mBackground,
                    (mBackground.width * mScale).toInt(),
                    (mBackground.height * mScale).toInt(),
                    true
            )
            mXAlignment = (width / 2).toFloat()
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode
            if (mLowBitAmbient) {
            }
            updateTimer()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            isTwentyTwo = false
            if (!isInAmbientMode)
                canvas.drawBitmap(mBackground, 0f, 0f, null)
            else
                canvas.drawColor(Color.BLACK)

            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            var text = ""
            var secondLine = ""

            val date = String.format(
                    "%s.%s",
                    mCalendar.get(Calendar.DAY_OF_MONTH),
                    mCalendar.get(Calendar.MONTH) + 1
            )
            val day = String.format("%s", mCalendar.getDisplayName(
                    Calendar.DAY_OF_WEEK,
                    Calendar.SHORT,
                    Locale.getDefault()
            ))
            val time = String.format(
                    "%d:%02d",
                    mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE)
            )
            val offset = mTimePaintMid.measureText(time)
            when (time) {
                SONGS -> {
                    text = "Наслушался"
                    secondLine = "песен"
                    isTwentyTwo = true
                }
                LOOK -> {
                    text = "Стал модно"
                    secondLine = "одеваться"
                    isTwentyTwo = true
                }
                HAIR -> {
                    text = "Красиво"
                    secondLine = "подстригся"
                    isTwentyTwo = true
                }
                KING -> {
                    text = "Король"
                    secondLine = "тусовок"
                    isTwentyTwo = true
                }
                MARKET -> {
                    text = "Говна"
                    secondLine = "рынок"
                    isTwentyTwo = true
                }
            }
            if (mCalendar.get(Calendar.MINUTE) == 22) {
                text = "Это везде"
                isTwentyTwo = true
            }
            canvas.drawText(
                    time,
                    mXAlignment - offset / 2,
                    mYOffset - 55,
                    mTimePaintMid
            )
            if (isTwentyTwo)
                canvas.drawText(
                        secondLine,
                        mXAlignment - mTextPaint.measureText(secondLine) / 2,
                        mYOffset + 170,
                        mTextPaint
                )
            else {
                canvas.drawText(
                        date,
                        mXAlignment - mTextPaint.measureText(date) / 2,
                        mYOffset + 150,
                        mTextPaint
                )
                canvas.drawText(
                        day,
                        mXAlignment - mTextPaint.measureText(day) / 2,
                        mYOffset + 180,
                        mTextPaint
                )
            }
            canvas.drawText(
                    text,
                    mXAlignment - mTextPaint.measureText(text) / 2,
                    mYOffset + 130,
                    mTextPaint
            )
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        override fun onApplyWindowInsets(insets: WindowInsets) {
            super.onApplyWindowInsets(insets)
            val resources = this@MyWatchFace.resources
            val isRound = insets.isRound
            mXOffset = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_x_offset_round
                    else
                        R.dimen.digital_x_offset
            )
            val timeSize = resources.getDimension(
                    if (isRound)
                        R.dimen.digital_time_size_round
                    else
                        R.dimen.digital_time_size
            )
            mTimePaintMid.textSize = timeSize
        }

        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        private fun shouldTimerBeRunning() = isVisible && !isInAmbientMode

        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }

        private fun getBitmapByDay(): Bitmap {
            return BitmapFactory.decodeResource(resources, R.drawable.wednesday)
        }
    }
}
