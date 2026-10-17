@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import echo.music.iad1tya.LocalPlayerAwareWindowInsets
import echo.music.iad1tya.R
import echo.music.iad1tya.constants.AppIconTypeKey
import echo.music.iad1tya.utils.AppIconType
import echo.music.iad1tya.utils.IconUtils
import echo.music.iad1tya.utils.rememberEnumPreference
import kotlinx.coroutines.launch

@Composable
fun AppIconSettingsScreen(
  navController: NavController,
  activity: Activity,
  snackbarHostState: SnackbarHostState
) {
  val (appIconType, onAppIconTypeChange) =
    rememberEnumPreference(AppIconTypeKey, defaultValue = AppIconType.DEFAULT)
  val coroutineScope = rememberCoroutineScope()

  // Android cannot swap the launcher icon for an arbitrary picture, so a gallery photo becomes a
  // home-screen shortcut (with that picture as its icon) that opens Pattukal.
  val galleryPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      coroutineScope.launch {
        val icon: Bitmap? =
          withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                activity.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                val src =
                  activity.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                  } ?: return@runCatching null
                val side = minOf(src.width, src.height)
                val square =
                  Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
                Bitmap.createScaledBitmap(square, 256, 256, true)
              }
              .getOrNull()
          }
        if (icon == null) {
          snackbarHostState.showSnackbar("Couldn't read that picture")
        } else if (!ShortcutManagerCompat.isRequestPinShortcutSupported(activity)) {
          snackbarHostState.showSnackbar("This launcher can't pin shortcuts")
        } else {
          val launch =
            Intent(Intent.ACTION_MAIN)
              .setPackage(activity.packageName)
              .addCategory(Intent.CATEGORY_LAUNCHER)
              .setClassName(activity.packageName, "echo.music.iad1tya.MainActivity")
          val info =
            ShortcutInfoCompat.Builder(activity, "pattukal_custom_${System.currentTimeMillis()}")
              .setShortLabel("Pattukal")
              .setIcon(IconCompat.createWithBitmap(icon))
              .setIntent(launch)
              .build()
          ShortcutManagerCompat.requestPinShortcut(activity, info, null)
          // notifications / dynamic island keep the official Pattukal glyph for gallery pictures
          echo.music.iad1tya.utils.NotificationIcon.setGalleryIcon(activity, true)
          Toast.makeText(activity, "Confirm to add it to your home screen", Toast.LENGTH_LONG).show()
        }
      }
    }

  fun handleIconChange(iconType: AppIconType) {
    echo.music.iad1tya.utils.NotificationIcon.setGalleryIcon(activity, false)
    if (appIconType == iconType) return
    onAppIconTypeChange(iconType)
    IconUtils.setIcon(activity, iconType)
    coroutineScope.launch {
      val result =
        snackbarHostState.showSnackbar(
          message = "Icon updated, restart to apply",
          actionLabel = "Restart"
        )
      if (result == SnackbarResult.ActionPerformed) {
        val packageManager = activity.packageManager
        val intent = packageManager.getLaunchIntentForPackage(activity.packageName)
        if (intent != null) {
          val componentName = intent.component
          val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
          activity.startActivity(mainIntent)
          Runtime.getRuntime().exit(0)
        }
      }
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      TopAppBar(
        title = { Text("App Icon", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
      )
    }
  ) { innerPadding ->
    val icons =
      listOf(
        AppIconOption(
          AppIconType.DEFAULT,
          "Pattukal Official",
          "Default Pattukal launcher icon",
          R.mipmap.ic_launcher
        ),
        AppIconOption(
          AppIconType.HEADPHONES,
          "Headphones",
          "Classic headphones with a music note",
          R.mipmap.icon_headphones
        ),
        AppIconOption(
          AppIconType.BILLIE_EILISH,
          "Billie Eilish",
          "Billie Eilish portrait",
          R.mipmap.icon_billie
        ),
        AppIconOption(
          AppIconType.MICHAEL,
          "Pattukal Michael",
          "Michael Jackson with the Pattukal script",
          R.mipmap.icon_michael
        ),
        AppIconOption(
          AppIconType.WAVE,
          "Sound Wave",
          "Tribal headphones with a red sound wave",
          R.mipmap.icon_wave
        ),
        AppIconOption(
          AppIconType.ORNATE,
          "Pattukal Cosmos",
          "Ornate engraved headphones in a starry crest",
          R.mipmap.icon_ornate
        ),
        AppIconOption(
          AppIconType.NOTE_WAVE,
          "Note Wave",
          "Music note over a sound wave",
          R.mipmap.icon_notewave
        ),
        AppIconOption(
          AppIconType.SUNSET,
          "Sunset",
          "Sunset over water with a floating note",
          R.mipmap.icon_sunset
        ),
        AppIconOption(
          AppIconType.DISC,
          "Headphone Disc",
          "Bold headphones on a bright disc",
          R.mipmap.icon_disc
        ),
        AppIconOption(
          AppIconType.PLANET,
          "Orbit Note",
          "A music note circled by a planet ring",
          R.mipmap.icon_planet
        ),
        AppIconOption(
          AppIconType.SKETCH,
          "Sketchbook",
          "Hand-drawn headphones, notes and lilies",
          R.mipmap.icon_sketch
        )
      )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
          .verticalScroll(rememberScrollState())
          .windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
          )
          .padding(innerPadding)
          .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Choose your launcher icon",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
      )

      Column(modifier = Modifier.fillMaxWidth()) {
        icons.forEachIndexed { index, option ->
          val shape =
            when {
              icons.size == 1 -> RoundedCornerShape(24.dp)
              index == 0 ->
                RoundedCornerShape(
                  topStart = 24.dp,
                  topEnd = 24.dp,
                  bottomStart = 4.dp,
                  bottomEnd = 4.dp
                )
              index == icons.size - 1 ->
                RoundedCornerShape(
                  topStart = 4.dp,
                  topEnd = 4.dp,
                  bottomStart = 24.dp,
                  bottomEnd = 24.dp
                )
              else -> RoundedCornerShape(4.dp)
            }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (appIconType == option.type)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                  else MaterialTheme.colorScheme.surfaceContainerHigh
              ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
          ) {
            AppIconRow(
              option = option,
              isSelected = (appIconType == option.type),
              onClick = { handleIconChange(option.type) }
            )
          }

          if (index < icons.size - 1) {
            Spacer(modifier = Modifier.height(2.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable {
                galleryPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
              .padding(horizontal = 20.dp, vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier =
              Modifier.size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Rounded.AddPhotoAlternate,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          Spacer(modifier = Modifier.width(20.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Pick from gallery",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Adds a home-screen icon with your own picture that opens Pattukal",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(
        modifier =
          Modifier.windowInsetsPadding(
            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
          )
      )
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

data class AppIconOption(
  val type: AppIconType,
  val title: String,
  val description: String,
  val iconRes: Int
)

@Composable
fun AppIconRow(option: AppIconOption, isSelected: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val context = LocalContext.current
    val bitmap =
      remember(option.iconRes) {
        val drawable = ContextCompat.getDrawable(context, option.iconRes)
        drawable?.toBitmap(width = 192, height = 192)?.asImageBitmap()
      }

    Box(
      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = null,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
        )
      }
    }

    Spacer(modifier = Modifier.width(20.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = option.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = option.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.width(8.dp))
    RadioButton(selected = isSelected, onClick = onClick)
  }
}
