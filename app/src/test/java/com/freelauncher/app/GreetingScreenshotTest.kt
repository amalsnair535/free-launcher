package com.freelauncher.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.freelauncher.app.ui.screens.HomeScreen
import com.freelauncher.app.ui.theme.FreeLauncherTheme
import com.freelauncher.app.ui.theme.LauncherFont
import com.freelauncher.app.ui.theme.LauncherThemeMode
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun home_screen_screenshot() {
    composeTestRule.setContent {
      FreeLauncherTheme(
        themeMode = LauncherThemeMode.OLED_BLACK,
        launcherFont = LauncherFont.MINIMAL_SANS
      ) {
        HomeScreen(
          state = LauncherUiState(),
          onNavigate = {},
          onOpenSettings = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/home_screen.png")
  }
}
