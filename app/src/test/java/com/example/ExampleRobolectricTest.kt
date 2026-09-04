package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.ChatThemeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SMS Connect", appName)
  }

  @Test
  fun `chat theme options are loaded properly`() {
    assertTrue(ChatThemeOption.ALL_THEMES.isNotEmpty())
    val defaultTheme = ChatThemeOption.getById(0)
    assertEquals("Azul Océano", defaultTheme.name)
    val whatsappTheme = ChatThemeOption.getById(1)
    assertEquals("Verde WhatsApp", whatsappTheme.name)
    assertNotNull(whatsappTheme.primaryColor)
  }
}
