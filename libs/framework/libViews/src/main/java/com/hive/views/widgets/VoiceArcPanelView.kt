package com.hive.views.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 底部三段语音操作面板，尽量贴近微信的布局：
 * - 底部是统一的大椭圆穹顶
 * - 左右是独立的肩部短弧
 * - 文案跟随各自弧线居中
 */
class VoiceArcPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val COLOR_SIDE_ARC_BASE = 0x46FFFFFF
        private const val COLOR_ARC_CANCEL = 0x90E9463C.toInt()
        private const val COLOR_ARC_SEND = 0x9072C666.toInt()
        private const val COLOR_CENTER_FILL = 0x5AFFFFFF
        private const val TEXT_SIZE_DP = 18f
        private const val ZONE_ANIM_DURATION_MS = 140
        private const val PANEL_TOP_PADDING_DP = 8f
        private const val MAIN_DOME_TOP_RATIO = 0.60f
        private const val MAIN_DOME_WIDTH_RATIO = 1.72f
        private const val MAIN_DOME_HEIGHT_RATIO = 1.02f
        private const val MAIN_DOME_TEXT_RATIO = 0.24f
        private const val SIDE_GUIDE_WIDTH_RATIO = 1.30f
        private const val SIDE_GUIDE_CENTER_FROM_DOME_TOP_RATIO = 0.18f
        private const val SIDE_ARC_START_LEFT = 208f
        private const val SIDE_ARC_START_RIGHT = 282f
        private const val SIDE_ARC_SWEEP = 50f
        private const val SIDE_TEXT_OFFSET_RATIO = 1.02f
        private const val EXTENDED_BOUNDS_X_RATIO = 0.18f
        private const val EXTENDED_BOUNDS_TOP_RATIO = 0.18f
        private const val TEXT_ALPHA_NORMAL = 228
        private const val TEXT_ALPHA_ACTIVE = 255
        private const val CENTER_TEXT_ALPHA = 248
        private const val ALPHA_THRESHOLD = 0.001f
        private const val SIDE_GUIDE_FLATNESS_RATIO = 0.76f
        private const val SIDE_HIT_ANGLE_PADDING_DEG = 5f
        private const val SIDE_HIT_DISTANCE_RATIO = 0.82f
        private const val SIDE_HIT_DISTANCE_MIN_DP = 24f
    }

    private val sideArcBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = COLOR_SIDE_ARC_BASE
    }
    private val arcCancelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = COLOR_ARC_CANCEL
    }
    private val arcSendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = COLOR_ARC_SEND
    }
    private val centerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = COLOR_CENTER_FILL
    }
    private val mainDomeRect = RectF()
    private val sideGuideRect = RectF()
    private val mainDomePath = Path()
    private val leftArcPath = Path()
    private val rightArcPath = Path()
    private val leftTextPath = Path()
    private val rightTextPath = Path()

    private var centerX = 0f
    private var sideArcStrokeWidth = 0f
    private var centerTextY = 0f
    private var visualBottom = 0f
    private var visualTop = 0f
    private var bottomSafeInset = 0f

    private var cancelLabel: String = ""
    private var sendLabel: String = ""
    private var centerLabel: String = ""

    private var cancelAlpha: Float = 0f
    private var sendAlpha: Float = 0f
    private var zoneAnimator: ValueAnimator? = null

    var zone: VoiceRecordingZone = VoiceRecordingZone.NEUTRAL
        private set

    private val leftTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = dp(TEXT_SIZE_DP)
        color = context.getColor(com.hive.i8n.R.color.textColorPrimary)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val rightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = dp(TEXT_SIZE_DP)
        color = context.getColor(com.hive.i8n.R.color.textColorPrimary)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = dp(TEXT_SIZE_DP)
        color = context.getColor(com.hive.i8n.R.color.textColorPrimary)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun setLabels(cancel: String, send: String) {
        cancelLabel = cancel
        sendLabel = send
        invalidate()
    }

    fun setCenterText(center: String) {
        centerLabel = center
        invalidate()
    }

    fun setZone(newZone: VoiceRecordingZone) {
        if (newZone == zone) return
        val fromCancel = cancelAlpha
        val fromSend = sendAlpha
        val toCancel = if (newZone == VoiceRecordingZone.CANCEL) 1f else 0f
        val toSend = if (newZone == VoiceRecordingZone.SEND) 1f else 0f
        zone = newZone
        zoneAnimator?.cancel()
        zoneAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ZONE_ANIM_DURATION_MS.toLong()
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                cancelAlpha = fromCancel + (toCancel - fromCancel) * progress
                sendAlpha = fromSend + (toSend - fromSend) * progress
                invalidate()
            }
            start()
        }
    }

    fun setBottomSafeInset(inset: Float) {
        if (bottomSafeInset == inset) return
        bottomSafeInset = inset
        if (width > 0 && height > 0) rebuildGeometry(width.toFloat(), height.toFloat())
    }

    fun hitTest(rawX: Float, rawY: Float): VoiceRecordingZone {
        if (width <= 0 || height <= 0) return VoiceRecordingZone.NEUTRAL
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        val x = rawX - loc[0]
        val y = rawY - loc[1]
        val widthF = width.toFloat()
        val heightF = height.toFloat()
        val extendedX = widthF * EXTENDED_BOUNDS_X_RATIO
        val extendedTop = heightF * EXTENDED_BOUNDS_TOP_RATIO
        if (x < -extendedX || x > widthF + extendedX || y < -extendedTop || y > heightF) {
            return VoiceRecordingZone.NEUTRAL
        }

        if (isPointNearEllipseArc(x, y, SIDE_ARC_START_LEFT, SIDE_ARC_SWEEP)) {
            return VoiceRecordingZone.CANCEL
        }
        if (isPointNearEllipseArc(x, y, SIDE_ARC_START_RIGHT, SIDE_ARC_SWEEP)) {
            return VoiceRecordingZone.SEND
        }
        return VoiceRecordingZone.NEUTRAL
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        zoneAnimator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildGeometry(w.toFloat(), h.toFloat())
    }

    private fun rebuildGeometry(w: Float, h: Float) {
        visualTop = dp(PANEL_TOP_PADDING_DP)
        visualBottom = (h - bottomSafeInset).coerceAtLeast(h * 0.78f)
        centerX = w / 2f
        val contentHeight = (visualBottom - visualTop).coerceAtLeast(dp(220f))

        val mainDomeWidth = w * MAIN_DOME_WIDTH_RATIO
        val mainDomeHeight = contentHeight * MAIN_DOME_HEIGHT_RATIO
        val mainDomeTop = visualTop + contentHeight * MAIN_DOME_TOP_RATIO
        mainDomeRect.set(
            centerX - mainDomeWidth / 2f,
            mainDomeTop,
            centerX + mainDomeWidth / 2f,
            mainDomeTop + mainDomeHeight
        )

        mainDomePath.reset()
        mainDomePath.arcTo(mainDomeRect, 180f, 180f, false)
        mainDomePath.lineTo(mainDomeRect.right + dp(24f), h + dp(48f))
        mainDomePath.lineTo(mainDomeRect.left - dp(24f), h + dp(48f))
        mainDomePath.close()

        sideArcStrokeWidth = (contentHeight * 0.18f).coerceIn(dp(48f), dp(64f))
        sideArcBasePaint.strokeWidth = sideArcStrokeWidth
        arcCancelPaint.strokeWidth = sideArcStrokeWidth
        arcSendPaint.strokeWidth = sideArcStrokeWidth
        centerTextY = mainDomeTop + mainDomeHeight * MAIN_DOME_TEXT_RATIO

        val sideGuideWidth = w * SIDE_GUIDE_WIDTH_RATIO
        val mainDomeAspectRatio = mainDomeWidth / mainDomeHeight
        val sideGuideHeight = (sideGuideWidth / mainDomeAspectRatio) * SIDE_GUIDE_FLATNESS_RATIO
        val sideGuideCenterY = mainDomeTop + sideGuideHeight * SIDE_GUIDE_CENTER_FROM_DOME_TOP_RATIO
        sideGuideRect.set(
            centerX - sideGuideWidth / 2f,
            sideGuideCenterY - sideGuideHeight / 2f,
            centerX + sideGuideWidth / 2f,
            sideGuideCenterY + sideGuideHeight / 2f
        )

        leftArcPath.reset()
        leftArcPath.addArc(sideGuideRect, SIDE_ARC_START_LEFT, SIDE_ARC_SWEEP)

        rightArcPath.reset()
        rightArcPath.addArc(sideGuideRect, SIDE_ARC_START_RIGHT, SIDE_ARC_SWEEP)

        leftTextPath.set(leftArcPath)
        rightTextPath.set(rightArcPath)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        canvas.drawPath(mainDomePath, centerFillPaint)
        canvas.drawPath(leftArcPath, sideArcBasePaint)
        canvas.drawPath(rightArcPath, sideArcBasePaint)

        if (cancelAlpha > ALPHA_THRESHOLD) {
            arcCancelPaint.alpha = (cancelAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawPath(leftArcPath, arcCancelPaint)
        }
        if (sendAlpha > ALPHA_THRESHOLD) {
            arcSendPaint.alpha = (sendAlpha * 255).toInt().coerceIn(0, 255)
            canvas.drawPath(rightArcPath, arcSendPaint)
        }

        leftTextPaint.color =
            if (zone == VoiceRecordingZone.CANCEL) {
                context.getColor(com.hive.i8n.R.color.colorRed)
            } else {
                context.getColor(com.hive.i8n.R.color.textColorPrimary)
            }
        rightTextPaint.color =
            if (zone == VoiceRecordingZone.SEND) {
                context.getColor(com.hive.i8n.R.color.colorGreen)
            } else {
                context.getColor(com.hive.i8n.R.color.textColorPrimary)
            }
        leftTextPaint.alpha =
            if (zone == VoiceRecordingZone.CANCEL) TEXT_ALPHA_ACTIVE else TEXT_ALPHA_NORMAL
        rightTextPaint.alpha =
            if (zone == VoiceRecordingZone.SEND) TEXT_ALPHA_ACTIVE else TEXT_ALPHA_NORMAL
        centerTextPaint.alpha = CENTER_TEXT_ALPHA

        drawCenteredTextOnPath(
            canvas,
            leftTextPath,
            cancelLabel,
            leftTextPaint,
            -sideArcStrokeWidth * SIDE_TEXT_OFFSET_RATIO
        )
        drawCenteredTextOnPath(
            canvas,
            rightTextPath,
            sendLabel,
            rightTextPaint,
            -sideArcStrokeWidth * SIDE_TEXT_OFFSET_RATIO
        )
        canvas.drawText(centerLabel, centerX, centerTextY, centerTextPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun isPointNearEllipseArc(
        x: Float,
        y: Float,
        startAngle: Float,
        sweepAngle: Float
    ): Boolean {
        val rx = sideGuideRect.width() / 2f
        val ry = sideGuideRect.height() / 2f
        if (rx <= 0f || ry <= 0f) return false

        val cx = sideGuideRect.centerX()
        val cy = sideGuideRect.centerY()
        val normalizedX = (x - cx) / rx
        val normalizedY = (y - cy) / ry
        var angle = Math.toDegrees(atan2(normalizedY.toDouble(), normalizedX.toDouble())).toFloat()
        if (angle < 0f) angle += 360f
        if (!isAngleInSweep(angle, startAngle, sweepAngle, SIDE_HIT_ANGLE_PADDING_DEG)) return false

        val angleRad = Math.toRadians(angle.toDouble())
        val edgeX = cx + rx * cos(angleRad).toFloat()
        val edgeY = cy + ry * sin(angleRad).toFloat()
        val dx = edgeX - x
        val dy = edgeY - y
        val threshold = maxOf(sideArcStrokeWidth * SIDE_HIT_DISTANCE_RATIO, dp(SIDE_HIT_DISTANCE_MIN_DP))
        val thresholdSquared = threshold * threshold
        return dx * dx + dy * dy <= thresholdSquared
    }

    private fun isAngleInSweep(
        angle: Float,
        startAngle: Float,
        sweepAngle: Float,
        padding: Float
    ): Boolean {
        val normalizedAngle = normalizeAngle(angle)
        val normalizedStart = normalizeAngle(startAngle - padding)
        val normalizedEnd = normalizeAngle(startAngle + sweepAngle + padding)
        return if (normalizedStart <= normalizedEnd) {
            normalizedAngle in normalizedStart..normalizedEnd
        } else {
            normalizedAngle >= normalizedStart || normalizedAngle <= normalizedEnd
        }
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) normalized += 360f
        return normalized
    }

    private fun drawCenteredTextOnPath(
        canvas: Canvas,
        path: Path,
        text: String,
        paint: Paint,
        vOffset: Float
    ) {
        if (text.isEmpty()) return
        val pathMeasure = PathMeasure(path, false)
        val hOffset = (pathMeasure.length - paint.measureText(text)) / 2f
        canvas.drawTextOnPath(text, path, hOffset, vOffset, paint)
    }
}
