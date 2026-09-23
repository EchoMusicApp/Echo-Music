@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
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

  fun handleIconChange(iconType: AppIconType) {
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
    topBar = {
      TopAppBar(
        title = { Text("App Icon", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
          )
      )
    }
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
          .background(
            androidx.compose.ui.graphics.Brush.verticalGradient(
              colors =
                listOf(
                  MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                  MaterialTheme.colorScheme.surface
                )
            )
          )
          .padding(innerPadding)
          .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      val icons =
        listOf(
          AppIconOption(
            AppIconType.DEFAULT,
            "New Icon",
            "The standard vibrant icon",
            R.mipmap.ic_launcher
          ),
          AppIconOption(
            AppIconType.LEGACY,
            "Legacy Icon",
            "The OG Monochrome Icon",
            R.mipmap.legacy_icon
          ),
          AppIconOption(
            AppIconType.CAT,
            "Cat Icon",
            "A Pinkish cat-themed icon",
            R.mipmap.cat_icon
          ),
          AppIconOption(
            AppIconType.CRAZY_BLUE,
            "Crazy Blue Icon",
            "A vibrant crazy blue icon",
            R.mipmap.crazy_blue_icon
          ),
          AppIconOption(
            AppIconType.POOKIE,
            "Pookie Icon",
            "A Cute Pink icon",
            R.mipmap.pookie_icon
          ),
          AppIconOption(
            AppIconType.SKY,
            "Sky Icon",
            "A beautiful sky-themed icon",
            R.mipmap.sky_icon
          ),
          AppIconOption(
            AppIconType.ECHO_CAT,
            "Echo Cat",
            "A playful cat by Alarp_Svc",
            R.mipmap.echo_cat_icon
          ),
          AppIconOption(
            AppIconType.EKO,
            "Eko",
            "Kinda a robotic face by Kouki",
            R.mipmap.eko_icon
          ),
          AppIconOption(
            AppIconType.WIERD_CAT,
            "Wierd Cat",
            "A quirky cat by ! Udransh...",
            R.mipmap.wierd_cat_icon
          ),
          AppIconOption(
            AppIconType.LEGACY_PIXEL,
            "Legacy Pixel",
            "Legacy icon pixel by Carcocciolo",
            R.mipmap.legacy_pixel_icon
          ),
          AppIconOption(
            AppIconType.NEW_PIXEL,
            "New Pixel",
            "New icon pixel by Carcocciolo",
            R.mipmap.new_pixel_icon
          ),
          AppIconOption(
            AppIconType.TAMIL_NAIDU,
            "Tamil Naidu",
            "Tamil Naidu by Hamish",
            R.mipmap.tamil_naidu_icon
          ),
          AppIconOption(
            AppIconType.BILLIE_EILISH,
            "Billie Eilish",
            "billie eilish by Lalo salamanca",
            R.mipmap.billie_eilish_icon
          )
        )

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(0.dp)
      ) {
        Column {
          icons.forEachIndexed { index, option ->
            AppIconRow(
              option = option,
              isSelected = (appIconType == option.type),
              onClick = { handleIconChange(option.type) }
            )
            if (index < icons.size - 1) {
              HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              )
            }
          }
        }
      }
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
  val backgroundColor =
    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    else Color.Transparent

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .background(backgroundColor)
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Icon Preview
    Box(
      modifier =
        Modifier.size(48.dp)
          .clip(androidx.compose.foundation.shape.CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center
    ) {
      val context = LocalContext.current
      val bitmap =
        remember(option.iconRes) {
          val drawable = ContextCompat.getDrawable(context, option.iconRes)
          drawable?.toBitmap(width = 192, height = 192)?.asImageBitmap()
        }
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = null,
          modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape)
        )
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = option.title,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = option.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    RadioButton(selected = isSelected, onClick = onClick)
  }
}
