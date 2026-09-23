@file:OptIn(ExperimentalMaterial3Api::class)

package echo.music.iad1tya.ui.screens.settings

import android.app.Activity
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = {
      TopAppBar(
        title = { Text("App Icon", fontWeight = FontWeight.Bold) },
        navigationIcon = {
          IconButton(onClick = { navController.navigateUp() }) {
            Icon(
              painter = painterResource(R.drawable.arrow_back),
              contentDescription = "Back"
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
      )
    }
  ) { innerPadding ->
    val icons =
      listOf(
        AppIconOption(
          AppIconType.BILLIE_EILISH,
          "Billie Eilish",
          "billie eilish by Lalo salamanca",
          R.mipmap.billie_eilish_icon
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
          AppIconType.ECHO_CAT,
          "Echo Cat",
          "A playful cat by Alarp_Svc",
          R.mipmap.echo_cat_icon
        ),
        AppIconOption(
          AppIconType.LEGACY,
          "Legacy Icon",
          "The OG Monochrome Icon",
          R.mipmap.legacy_icon
        ),
        AppIconOption(
          AppIconType.DEFAULT,
          "New Icon",
          "The standard vibrant icon",
          R.mipmap.ic_launcher
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
