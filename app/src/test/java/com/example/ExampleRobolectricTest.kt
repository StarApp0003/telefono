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
  fun `default sms prompt strings match requirements`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val title = context.getString(R.string.not_default_title)
    val button = context.getString(R.string.activate_button)
    assertEquals("SMS Connect no es la app predeterminada", title)
    assertEquals("Activar", button)
  }

  @Test
  fun `viewmodel settings state is reactive`() {
    val context = ApplicationProvider.getApplicationContext<Context>() as android.app.Application
    val vm = com.example.ui.viewmodel.SmsViewModel(context)
    
    // Changing theme mode should update uiState immediately
    vm.setThemeMode(2)
    assertEquals(2, vm.uiState.value.themeMode)
    
    vm.setThemeMode(1)
    assertEquals(1, vm.uiState.value.themeMode)

    // Changing chat theme color should update uiState immediately
    vm.setChatThemeColor(3)
    assertEquals(3, vm.uiState.value.chatThemeColor)

    // Toggling sounds should update uiState immediately
    vm.setInChatSoundsEnabled(false)
    assertEquals(false, vm.uiState.value.inChatSoundsEnabled)
    vm.setInChatSoundsEnabled(true)
    assertEquals(true, vm.uiState.value.inChatSoundsEnabled)
  }
}
