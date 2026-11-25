/**
 * Echo Music
 * Dynamic Island Manager - Universal floating playback capsule for Android devices
 * Following Nothing OS 5.0 Design Language
 */

package echo.music.iad1tya.playback

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import echo.music.iad1tya.MainActivity
import echo.music.iad1tya.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DynamicIslandManager(private val context: Context) {

  interface Listener {
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onOpenApp()
  }

  var listener: Listener? = null

  private val fallbackWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
  private var addedWindowManager: WindowManager? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private var isAdded = false
  private var isExpanded = false
  @Volatile var isEnabled: Boolean = false

  // View components
  private val rootContainer = FrameLayout(context)
  private val capsuleView = LinearLayout(context)
  private val expandedView = LinearLayout(context)

  // Compact View children
  private val compactArtwork = ImageView(context)
  private val compactTitle = TextView(context)
  private val compactVisualizer = DynamicVisualizerView(context)

  // Expanded View children
  private val expandedArtwork = ImageView(context)
  private val expandedTitle = TextView(context)
  private val expandedArtist = TextView(context)
  private val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
  private val timeCurrent = TextView(context)
  private val timeTotal = TextView(context)
  private val playPauseIcon = ImageView(context)

  private var cachedArtworkUri: String? = null
  private var imageLoadJob: Job? = null
  private var collapseRunnable: Runnable? = null
  private var savedCapsuleX: Int = 0
  private var savedCapsuleY: Int = 0

  // Smooth gradient state for expanded card
  private var gradTopColor: Int = Color.parseColor("#2A1E3D")
  private var gradMidColor: Int = Color.parseColor("#181624")
  private var gradBotColor: Int = Color.parseColor("#0C0C10")
  private var gradBorderColor: Int = Color.parseColor("#3DFFFFFF")
  private var gradientAnimator: ValueAnimator? = null
  private lateinit var expandedBg: GradientDrawable

  private fun resolveWindowManager(): WindowManager? {
    val service = DynamicIslandAccessibilityService.instance
    return if (service != null) {
      service.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    } else {
      fallbackWindowManager
    }
  }

  private fun resolveOverlayType(): Int {
    return if (DynamicIslandAccessibilityService.instance != null &&
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
    ) {
      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }
  }

  private fun updateOverlayLayout() {
    try {
      (addedWindowManager ?: resolveWindowManager())?.updateViewLayout(rootContainer, layoutParams)
    } catch (_: Exception) {}
  }

  private val layoutParams: WindowManager.LayoutParams by lazy {
    WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      resolveOverlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
      val statusBarHeight = getStatusBarHeight()
      y = statusBarHeight + dp(6)
    }
  }

  init {
    setupViews()
    DynamicIslandAccessibilityService.onServiceStateChanged = {
      reattachOverlayWindow()
    }
  }

  fun hasOverlayPermission(): Boolean {
    if (DynamicIslandAccessibilityService.instance != null) return true
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(context)
    } else {
      true
    }
  }

  private fun dp(v: Int): Int =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      v.toFloat(),
      context.resources.displayMetrics
    ).toInt()

  private fun dpF(v: Float): Float =
    TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      v,
      context.resources.displayMetrics
    )

  private fun getStatusBarHeight(): Int {
    var result = 0
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
      result = context.resources.getDimensionPixelSize(resourceId)
    }
    return if (result > 0) result else dp(24)
  }

  private fun View.addSpringTouchFeedback(onClick: () -> Unit) {
    setOnTouchListener { v, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          v.animate().cancel()
          v.animate().scaleX(0.90f).scaleY(0.90f).alpha(0.85f).setDuration(70).start()
          true
        }
        MotionEvent.ACTION_UP -> {
          v.animate().cancel()
          v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(110).start()
          onClick()
          true
        }
        MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
          v.animate().cancel()
          v.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(70).start()
          true
        }
        else -> false
      }
    }
  }

  private fun setupViews() {
    rootContainer.clipChildren = false
    rootContainer.clipToPadding = false
    rootContainer.setOnTouchListener { _, event ->
      if (isExpanded && event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
        collapse()
        true
      } else {
        false
      }
    }

    // 1. Build Compact Capsule View (Dynamic Island / Origin Island Pill)
    capsuleView.orientation = LinearLayout.HORIZONTAL
    capsuleView.gravity = Gravity.CENTER_VERTICAL
    val capsuleBg =
      GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        intArrayOf(Color.parseColor("#141418"), Color.parseColor("#08080A"))
      ).apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpF(50f)
        setStroke(dp(1), Color.parseColor("#2E2E34"))
      }
    capsuleView.background = capsuleBg
    capsuleView.elevation = dpF(8f)
    capsuleView.setPadding(dp(7), dp(5), dp(10), dp(5))

    // Circular mini album artwork (non-clickable so capsuleView receives all touches)
    compactArtwork.layoutParams =
      LinearLayout.LayoutParams(dp(26), dp(26)).apply {
        gravity = Gravity.CENTER_VERTICAL
      }
    compactArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
    compactArtwork.setImageResource(R.drawable.music_note)
    compactArtwork.outlineProvider =
      object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
          outline.setOval(0, 0, view.width, view.height)
        }
      }
    compactArtwork.clipToOutline = true
    compactArtwork.isClickable = false
    compactArtwork.isFocusable = false
    capsuleView.addView(compactArtwork)

    // Compact Track Title
    compactTitle.layoutParams =
      LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply {
        setMargins(dp(8), 0, dp(8), 0)
        gravity = Gravity.CENTER_VERTICAL
      }
    compactTitle.maxWidth = dp(136)
    compactTitle.setTextColor(Color.WHITE)
    compactTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    compactTitle.isSingleLine = true
    compactTitle.ellipsize = TextUtils.TruncateAt.END
    compactTitle.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
    compactTitle.text = context.getString(R.string.app_name)
    compactTitle.isClickable = false
    compactTitle.isFocusable = false
    capsuleView.addView(compactTitle)

    // Dynamic Visualizer (3 animated Nothing Red bars)
    compactVisualizer.layoutParams =
      LinearLayout.LayoutParams(dp(16), dp(14)).apply {
        gravity = Gravity.CENTER_VERTICAL
      }
    compactVisualizer.isClickable = false
    compactVisualizer.isFocusable = false
    capsuleView.addView(compactVisualizer)

    // Unified Drag and Tap detection directly on Capsule
    val touchSlop = maxOf(ViewConfiguration.get(context).scaledTouchSlop, dp(8))
    var startX = 0f
    var startY = 0f
    var touchDownX = 0f
    var touchDownY = 0f
    var touchDownTime = 0L
    var isDragging = false
    var isTouchActive = false

    capsuleView.setOnTouchListener { v, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          startX = layoutParams.x.toFloat()
          startY = layoutParams.y.toFloat()
          touchDownX = event.rawX
          touchDownY = event.rawY
          touchDownTime = System.currentTimeMillis()
          isDragging = false
          isTouchActive = true
          v.animate().cancel()
          v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80).start()
          true
        }
        MotionEvent.ACTION_MOVE -> {
          if (!isTouchActive) return@setOnTouchListener false
          val dx = event.rawX - touchDownX
          val dy = event.rawY - touchDownY
          val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
          if (!isDragging && dist > touchSlop) {
            isDragging = true
          }
          if (isDragging) {
            layoutParams.x = (startX + dx).toInt()
            layoutParams.y = (startY + dy).toInt()
            updateOverlayLayout()
          }
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!isTouchActive) return@setOnTouchListener false
          isTouchActive = false
          v.animate().cancel()
          v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
          val elapsed = System.currentTimeMillis() - touchDownTime
          val dx = event.rawX - touchDownX
          val dy = event.rawY - touchDownY
          val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
          // Fire expand for any tap — even if slightly dragged (forgiving 20dp threshold)
          if (!isDragging || (dist < dp(20) && elapsed < 500)) {
            mainHandler.post { expand() }
          }
          true
        }
        MotionEvent.ACTION_CANCEL -> {
          isTouchActive = false
          isDragging = false
          v.animate().cancel()
          v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start()
          true
        }
        else -> false
      }
    }

    // Safety fallback click listener — fires expand() if touch listener misses it
    capsuleView.isClickable = true
    capsuleView.isFocusable = false
    capsuleView.setOnClickListener { expand() }

    // 2. Build Expanded Player Card View (Smooth Gradient Rectangular Card)
    expandedView.orientation = LinearLayout.VERTICAL
    expandedBg =
      GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(gradTopColor, gradMidColor, gradBotColor)
      ).apply {
        shape = GradientDrawable.RECTANGLE
        gradientType = GradientDrawable.LINEAR_GRADIENT
        cornerRadius = dpF(24f)
        setStroke(dp(1), gradBorderColor)
      }
    expandedView.background = expandedBg
    expandedView.elevation = dpF(18f)
    expandedView.setPadding(dp(18), dp(16), dp(18), dp(16))
    expandedView.layoutParams =
      FrameLayout.LayoutParams(dp(340), ViewGroup.LayoutParams.WRAP_CONTENT)
    expandedView.visibility = View.GONE

    // Top Header: Artwork + Title/Artist + Close Button
    val headerLayout = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
    }

    expandedArtwork.layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
    expandedArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
    expandedArtwork.setImageResource(R.drawable.music_note)
    expandedArtwork.outlineProvider =
      object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
          outline.setRoundRect(0, 0, view.width, view.height, dpF(14f))
        }
      }
    expandedArtwork.clipToOutline = true
    expandedArtwork.addSpringTouchFeedback {
      listener?.onOpenApp()
      collapse()
    }
    headerLayout.addView(expandedArtwork)

    val textCol = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL
      layoutParams =
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
          setMargins(dp(14), 0, dp(10), 0)
        }
      addSpringTouchFeedback {
        listener?.onOpenApp()
        collapse()
      }
    }

    expandedTitle.setTextColor(Color.WHITE)
    expandedTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
    expandedTitle.isSingleLine = true
    expandedTitle.ellipsize = TextUtils.TruncateAt.END
    expandedTitle.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
    expandedTitle.text = context.getString(R.string.app_name)
    textCol.addView(expandedTitle)

    expandedArtist.setTextColor(Color.parseColor("#B8FFFFFF"))
    expandedArtist.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    expandedArtist.isSingleLine = true
    expandedArtist.ellipsize = TextUtils.TruncateAt.END
    expandedArtist.setPadding(0, dp(2), 0, 0)
    expandedArtist.text = ""
    textCol.addView(expandedArtist)

    headerLayout.addView(textCol)

    val closeBtn = ImageView(context).apply {
      layoutParams = LinearLayout.LayoutParams(dp(30), dp(30))
      background =
        GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.parseColor("#24FFFFFF"))
        }
      setImageResource(R.drawable.close)
      setColorFilter(Color.parseColor("#DDFFFFFF"))
      setPadding(dp(7), dp(7), dp(7), dp(7))
      addSpringTouchFeedback { collapse() }
    }
    headerLayout.addView(closeBtn)

    expandedView.addView(headerLayout)

    // Progress bar with translucent frosted track and crisp white fill
    progressBar.layoutParams =
      LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(4)
      ).apply {
        setMargins(0, dp(16), 0, dp(6))
      }
    val trackBg =
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpF(2f)
        setColor(Color.parseColor("#33FFFFFF"))
      }
    val fillDrawable =
      GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpF(2f)
        setColor(Color.WHITE)
      }
    val clipFill = ClipDrawable(fillDrawable, Gravity.START, ClipDrawable.HORIZONTAL)
    val progressLayer =
      LayerDrawable(arrayOf(trackBg, clipFill)).apply {
        setId(0, android.R.id.background)
        setId(1, android.R.id.progress)
      }
    progressBar.progressDrawable = progressLayer
    expandedView.addView(progressBar)

    // Timestamps
    val timeRow = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    timeCurrent.setTextColor(Color.parseColor("#A8FFFFFF"))
    timeCurrent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    timeCurrent.fontFeatureSettings = "tnum"
    timeCurrent.text = "0:00"
    timeRow.addView(timeCurrent)

    val timeSpacer = View(context).apply {
      layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
    }
    timeRow.addView(timeSpacer)

    timeTotal.setTextColor(Color.parseColor("#A8FFFFFF"))
    timeTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
    timeTotal.fontFeatureSettings = "tnum"
    timeTotal.text = "0:00"
    timeRow.addView(timeTotal)

    expandedView.addView(timeRow)

    // Control Buttons (Previous, Play/Pause, Next)
    val controlsRow = LinearLayout(context).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
      layoutParams =
        LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
          setMargins(0, dp(12), 0, dp(2))
        }
    }

    // Previous Button (46dp translucent glass circle)
    val prevFrame = FrameLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
      background =
        GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.parseColor("#20FFFFFF"))
          setStroke(dp(1), Color.parseColor("#2AFFFFFF"))
        }
      val icon = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
        setImageResource(R.drawable.skip_previous)
        setColorFilter(Color.WHITE)
      }
      addView(icon)
      addSpringTouchFeedback {
        listener?.onPrevious()
        resetAutoCollapse()
      }
    }
    controlsRow.addView(prevFrame)

    // Play / Pause Button (56dp white circle hero)
    val playPauseFrame = FrameLayout(context).apply {
      layoutParams =
        LinearLayout.LayoutParams(dp(56), dp(56)).apply {
          setMargins(dp(32), 0, dp(32), 0)
        }
      background =
        GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.WHITE)
        }
      elevation = dpF(6f)
      addSpringTouchFeedback {
        listener?.onPlayPause()
        resetAutoCollapse()
      }
    }

    playPauseIcon.layoutParams =
      FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER)
    playPauseIcon.setImageResource(R.drawable.play)
    playPauseIcon.setColorFilter(Color.parseColor("#101014"))
    playPauseFrame.addView(playPauseIcon)
    controlsRow.addView(playPauseFrame)

    // Next Button (46dp translucent glass circle)
    val nextFrame = FrameLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
      background =
        GradientDrawable().apply {
          shape = GradientDrawable.OVAL
          setColor(Color.parseColor("#20FFFFFF"))
          setStroke(dp(1), Color.parseColor("#2AFFFFFF"))
        }
      val icon = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
        setImageResource(R.drawable.skip_next)
        setColorFilter(Color.WHITE)
      }
      addView(icon)
      addSpringTouchFeedback {
        listener?.onNext()
        resetAutoCollapse()
      }
    }
    controlsRow.addView(nextFrame)

    expandedView.addView(controlsRow)

    // Add capsule and expanded views to root container
    rootContainer.addView(capsuleView)
    rootContainer.addView(expandedView)
  }

  fun expand() {
    if (isExpanded) return
    isExpanded = true
    collapseRunnable?.let { mainHandler.removeCallbacks(it) }

    savedCapsuleX = layoutParams.x
    savedCapsuleY = layoutParams.y

    // Center the expanded card below the status bar
    layoutParams.x = 0
    layoutParams.y = getStatusBarHeight() + dp(6)

    capsuleView.animate().cancel()
    expandedView.animate().cancel()

    capsuleView.visibility = View.GONE
    capsuleView.alpha = 1f
    capsuleView.scaleX = 1f
    capsuleView.scaleY = 1f

    expandedView.alpha = 0f
    expandedView.scaleX = 0.88f
    expandedView.scaleY = 0.88f
    expandedView.visibility = View.VISIBLE

    rootContainer.requestLayout()
    updateOverlayLayout()

    expandedView.animate()
      .alpha(1f)
      .scaleX(1f)
      .scaleY(1f)
      .setDuration(220)
      .setInterpolator(AccelerateDecelerateInterpolator())
      .start()

    resetAutoCollapse()
  }

  fun collapse() {
    if (!isExpanded) return
    isExpanded = false
    collapseRunnable?.let { mainHandler.removeCallbacks(it) }

    layoutParams.x = savedCapsuleX
    layoutParams.y = savedCapsuleY

    capsuleView.animate().cancel()
    expandedView.animate().cancel()

    expandedView.visibility = View.GONE
    expandedView.alpha = 1f
    expandedView.scaleX = 1f
    expandedView.scaleY = 1f

    capsuleView.alpha = 0f
    capsuleView.scaleX = 0.92f
    capsuleView.scaleY = 0.92f
    capsuleView.visibility = View.VISIBLE

    rootContainer.requestLayout()
    updateOverlayLayout()

    capsuleView.animate()
      .alpha(1f)
      .scaleX(1f)
      .scaleY(1f)
      .setDuration(180)
      .setInterpolator(AccelerateDecelerateInterpolator())
      .start()
  }

  private fun resetAutoCollapse() {
    collapseRunnable?.let { mainHandler.removeCallbacks(it) }
    collapseRunnable = Runnable { collapse() }
    mainHandler.postDelayed(collapseRunnable!!, 8000)
  }

  fun updatePlayback(
    title: String,
    artist: String,
    artworkUri: String?,
    isPlaying: Boolean,
    duration: Long,
    currentPosition: Long
  ) {
    mainHandler.post {
      if (!isEnabled || !hasOverlayPermission()) {
        if (isAdded) hide()
        return@post
      }

      if (title.isBlank() && !isPlaying) {
        if (isAdded) hide()
        return@post
      }

      if (!isAdded) {
        show()
      }

      val displayTitle = title.ifBlank { context.getString(R.string.app_name) }
      if (compactTitle.text?.toString() != displayTitle) {
        compactTitle.text = displayTitle
      }
      if (expandedTitle.text?.toString() != displayTitle) {
        expandedTitle.text = displayTitle
      }
      val displayArtist = artist.ifBlank { "" }
      if (expandedArtist.text?.toString() != displayArtist) {
        expandedArtist.text = displayArtist
      }

      compactVisualizer.isPlaying = isPlaying
      playPauseIcon.setImageResource(if (isPlaying) R.drawable.pause else R.drawable.play)

      if (duration > 0) {
        progressBar.max = duration.toInt()
        progressBar.progress = currentPosition.coerceIn(0, duration).toInt()
        timeCurrent.text = formatTime(currentPosition)
        timeTotal.text = formatTime(duration)
      } else {
        progressBar.progress = 0
        timeCurrent.text = "0:00"
        timeTotal.text = "0:00"
      }

      loadArtwork(artworkUri)
    }
  }

  private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
  }

  private fun loadArtwork(uri: String?) {
    if (uri == cachedArtworkUri) return
    cachedArtworkUri = uri
    imageLoadJob?.cancel()

    if (uri.isNullOrBlank()) {
      compactArtwork.setImageResource(R.drawable.music_note)
      expandedArtwork.setImageResource(R.drawable.music_note)
      updateCardGradientFromBitmap(null)
      return
    }

    imageLoadJob =
      scope.launch(Dispatchers.IO) {
        val bitmap =
          try {
            val req =
              ImageRequest.Builder(context)
                .data(uri)
                .size(dp(120), dp(120))
                .allowHardware(false)
                .crossfade(200)
                .build()
            context.imageLoader.execute(req).image?.toBitmap()
          } catch (e: Exception) {
            null
          }

        withContext(Dispatchers.Main) {
          if (bitmap != null) {
            compactArtwork.setImageBitmap(bitmap)
            expandedArtwork.setImageBitmap(bitmap)
          } else {
            compactArtwork.setImageResource(R.drawable.music_note)
            expandedArtwork.setImageResource(R.drawable.music_note)
          }
          updateCardGradientFromBitmap(bitmap)
        }
      }
  }

  private fun updateCardGradientFromBitmap(bitmap: Bitmap?) {
    if (bitmap == null) {
      animateGradientColors(
        targetTop = Color.parseColor("#2A1E3D"),
        targetMid = Color.parseColor("#181624"),
        targetBot = Color.parseColor("#0C0C10"),
        targetBorder = Color.parseColor("#3DFFFFFF")
      )
      return
    }

    scope.launch(Dispatchers.Default) {
      val palette = try {
        Palette.from(bitmap).generate()
      } catch (e: Exception) {
        null
      }

      val accent =
        palette?.getVibrantColor(0)?.takeIf { it != 0 }
          ?: palette?.getLightVibrantColor(0)?.takeIf { it != 0 }
          ?: palette?.getDarkVibrantColor(0)?.takeIf { it != 0 }
          ?: palette?.getDominantColor(0)?.takeIf { it != 0 }
          ?: Color.parseColor("#3D2B56")

      val secondary =
        palette?.getMutedColor(0)?.takeIf { it != 0 }
          ?: palette?.getDarkMutedColor(0)?.takeIf { it != 0 }
          ?: accent

      val deepBase = Color.parseColor("#0B0B10")
      val targetTop = ColorUtils.blendARGB(deepBase, accent, 0.54f)
      val targetMid = ColorUtils.blendARGB(deepBase, secondary, 0.32f)
      val targetBot = Color.parseColor("#0A0A0E")
      val targetBorder =
        ColorUtils.setAlphaComponent(
          ColorUtils.blendARGB(accent, Color.WHITE, 0.35f),
          85
        )

      withContext(Dispatchers.Main) {
        animateGradientColors(targetTop, targetMid, targetBot, targetBorder)
      }
    }
  }

  private fun animateGradientColors(
    targetTop: Int,
    targetMid: Int,
    targetBot: Int,
    targetBorder: Int
  ) {
    gradientAnimator?.cancel()
    val startTop = gradTopColor
    val startMid = gradMidColor
    val startBot = gradBotColor
    val startBorder = gradBorderColor
    val evaluator = ArgbEvaluator()

    gradientAnimator =
      ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 380
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener { anim ->
          val f = anim.animatedFraction
          gradTopColor = evaluator.evaluate(f, startTop, targetTop) as Int
          gradMidColor = evaluator.evaluate(f, startMid, targetMid) as Int
          gradBotColor = evaluator.evaluate(f, startBot, targetBot) as Int
          gradBorderColor = evaluator.evaluate(f, startBorder, targetBorder) as Int
          expandedBg.colors = intArrayOf(gradTopColor, gradMidColor, gradBotColor)
          expandedBg.setStroke(dp(1), gradBorderColor)
        }
        start()
      }
  }

  private fun showInternal() {
    if (isAdded || !isEnabled || !hasOverlayPermission()) return
    val wm = resolveWindowManager() ?: return
    layoutParams.type = resolveOverlayType()
    try {
      wm.addView(rootContainer, layoutParams)
      addedWindowManager = wm
      isAdded = true
      rootContainer.visibility = View.VISIBLE
    } catch (e: Exception) {
      Log.e("DynamicIsland", "Error adding overlay view: ${e.message}")
    }
  }

  fun show() {
    mainHandler.post { showInternal() }
  }

  private fun hideInternal() {
    if (!isAdded) return
    try {
      collapseRunnable?.let { mainHandler.removeCallbacks(it) }
      compactVisualizer.isPlaying = false
      (addedWindowManager ?: resolveWindowManager())?.removeView(rootContainer)
    } catch (e: Exception) {
      Log.e("DynamicIsland", "Error removing overlay view: ${e.message}")
    } finally {
      addedWindowManager = null
      isAdded = false
      isExpanded = false
    }
  }

  fun hide() {
    mainHandler.post { hideInternal() }
  }

  fun reattachOverlayWindow() {
    mainHandler.post {
      if (!isEnabled || !hasOverlayPermission()) return@post
      val targetWm = resolveWindowManager()
      val targetType = resolveOverlayType()
      if (isAdded && addedWindowManager === targetWm && layoutParams.type == targetType) {
        return@post
      }
      if (isAdded) {
        hideInternal()
      }
      showInternal()
    }
  }

  fun destroy() {
    DynamicIslandAccessibilityService.onServiceStateChanged = null
    gradientAnimator?.cancel()
    gradientAnimator = null
    hide()
    scope.cancel()
  }

  class DynamicVisualizerView(context: Context) : View(context) {
    private val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D71921") // Nothing Red
        style = Paint.Style.FILL
      }
    private val heights = floatArrayOf(0.3f, 0.7f, 0.5f)
    private var animator: ValueAnimator? = null

    var isPlaying: Boolean = false
      set(value) {
        if (field != value) {
          field = value
          if (value) startAnim() else stopAnim()
        }
      }

    private fun startAnim() {
      if (animator?.isRunning == true) return
      animator =
        ValueAnimator.ofFloat(0f, 1f).apply {
          duration = 400
          repeatCount = ValueAnimator.INFINITE
          repeatMode = ValueAnimator.REVERSE
          interpolator = AccelerateDecelerateInterpolator()
          addUpdateListener {
            val f = it.animatedFraction
            heights[0] = 0.25f + 0.65f * f
            heights[1] = 0.9f - 0.6f * f
            heights[2] = 0.35f + 0.55f * ((f + 0.4f) % 1f)
            invalidate()
          }
          start()
        }
    }

    private fun stopAnim() {
      animator?.cancel()
      animator = null
      heights[0] = 0.25f
      heights[1] = 0.25f
      heights[2] = 0.25f
      invalidate()
    }

    override fun onDraw(canvas: Canvas) {
      super.onDraw(canvas)
      val w = width.toFloat()
      val h = height.toFloat()
      val barCount = 3
      val spacing = 2.5f * resources.displayMetrics.density
      val totalSpacing = spacing * (barCount - 1)
      val barWidth = maxOf(2f, (w - totalSpacing) / barCount)
      val radius = barWidth / 2f

      for (i in 0 until barCount) {
        val left = i * (barWidth + spacing)
        val barH = maxOf(barWidth, h * heights[i])
        val top = (h - barH) / 2f
        canvas.drawRoundRect(left, top, left + barWidth, top + barH, radius, radius, paint)
      }
    }

    override fun onDetachedFromWindow() {
      super.onDetachedFromWindow()
      stopAnim()
    }
  }
}
