package com.example.visualvibefincal.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE, HAPPY, THINKING, ERROR, EXCITED, WAVING
}

enum class AssistantMessageType {
    SPEECH, THOUGHT
}

enum class AssistantColor(val hex: Long) {
    DEFAULT(0xFF00D1B2),
    BLUE(0xFF2196F3),
    PURPLE(0xFF9C27B0),
    ORANGE(0xFFFF9800),
    PINK(0xFFE91E63),
    CYAN(0xFF00BCD4),
    DARK(0xFF212121),
    NEON(0xFFCCFF00),
    RED(0xFFF44336),
    WHITE(0xFFFFFFFF)
}

enum class AssistantTheme(
    val headColor: AssistantColor,
    val bodyColor: AssistantColor,
    val accentColor: AssistantColor,
    val label: String
) {
    DEFAULT(AssistantColor.DEFAULT, AssistantColor.DEFAULT, AssistantColor.DEFAULT, "Teal"),
    OCEAN(AssistantColor.BLUE, AssistantColor.BLUE, AssistantColor.CYAN, "Ocean"),
    BERRY(AssistantColor.PURPLE, AssistantColor.PURPLE, AssistantColor.PINK, "Berry"),
    NEON_NIGHT(AssistantColor.DARK, AssistantColor.DARK, AssistantColor.NEON, "Neon"),
    SUNSET(AssistantColor.ORANGE, AssistantColor.ORANGE, AssistantColor.RED, "Sunset"),
    CUSTOM(AssistantColor.DEFAULT, AssistantColor.DEFAULT, AssistantColor.DEFAULT, "Custom")
}

enum class AssistantFrequency {
    LOW, MEDIUM, HIGH
}

data class AssistantPrefs(
    val isEnabled: Boolean = true,
    val isMuted: Boolean = false,
    val frequency: AssistantFrequency = AssistantFrequency.MEDIUM,
    val theme: AssistantTheme = AssistantTheme.DEFAULT,
    val customHeadColor: AssistantColor = AssistantColor.DEFAULT,
    val customBodyColor: AssistantColor = AssistantColor.DEFAULT,
    val customAccentColor: AssistantColor = AssistantColor.DEFAULT,
    val isCustomMode: Boolean = false,
    val lastPosX: Float = -1f,
    val lastPosY: Float = -1f
)

class AssistantViewModel : ViewModel() {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _messageType = MutableStateFlow(AssistantMessageType.SPEECH)
    val messageType: StateFlow<AssistantMessageType> = _messageType.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _robotState = MutableStateFlow(AssistantState.IDLE)
    val robotState: StateFlow<AssistantState> = _robotState.asStateFlow()

    private val _prefs = MutableStateFlow(AssistantPrefs())
    val prefs: StateFlow<AssistantPrefs> = _prefs.asStateFlow()

    private var messageJob: Job? = null

    private val financialTips = listOf(
        "Saving 10% monthly can grow significantly over time 📈",
        "Track your expenses to stay ahead 💰",
        "Compound interest is the eighth wonder of the world!",
        "Always keep an emergency fund of 3-6 months.",
        "Diversify your investments to manage risk.",
        "Small changes in interest rates can mean big savings on loans."
    )

    private val funFacts = listOf(
        "I'm 99% calculator, 1% comedian 🤖",
        "Math is fun… sometimes 😄",
        "I never sleep, I just compute.",
        "Beep boop! Processing awesomeness..."
    )

    fun showMessage(
        text: String,
        state: AssistantState = AssistantState.IDLE,
        type: AssistantMessageType = AssistantMessageType.SPEECH,
        durationMs: Long = 4000
    ) {
        if (!_prefs.value.isEnabled) return

        messageJob?.cancel()
        messageJob = viewModelScope.launch {
            _messageType.value = type
            // Respect WAVING state if it's currently active
            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = state
            }
            _isTyping.value = true
            delay(1000) // Simulate typing
            _isTyping.value = false
            _message.value = text
            // After typing, switch to the intended expression if not waving anymore
            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = state
            }
            delay(durationMs)
            _message.value = null
            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = AssistantState.IDLE
            }
        }
    }

    fun triggerWave() {
        if (!_prefs.value.isEnabled) return
        viewModelScope.launch {
            _robotState.value = AssistantState.WAVING
            delay(1500) // Duration of the wave animation
            if (_message.value == null) {
                _robotState.value = AssistantState.IDLE
            } else {
                _robotState.value = AssistantState.HAPPY
            }
        }
    }

    fun triggerRandomTip() {
        val tip = financialTips.random()
        showMessage(tip, AssistantState.HAPPY)
    }

    fun triggerRandomJoke() {
        val joke = funFacts.random()
        showMessage(joke, AssistantState.EXCITED)
    }

    fun updatePosition(x: Float, y: Float, context: Context) {
        _prefs.value = _prefs.value.copy(lastPosX = x, lastPosY = y)
        savePrefs(context)
    }

    fun setEnabled(enabled: Boolean, context: Context) {
        _prefs.value = _prefs.value.copy(isEnabled = enabled)
        savePrefs(context)
    }

    fun setMuted(muted: Boolean, context: Context) {
        _prefs.value = _prefs.value.copy(isMuted = muted)
        savePrefs(context)
    }

    fun setTheme(theme: AssistantTheme, context: Context) {
        _prefs.value = _prefs.value.copy(theme = theme, isCustomMode = theme == AssistantTheme.CUSTOM)
        savePrefs(context)
        if (theme != AssistantTheme.CUSTOM) {
            showMessage("Nice look 😎", AssistantState.HAPPY)
        }
    }

    fun setCustomColors(head: AssistantColor, body: AssistantColor, accent: AssistantColor, context: Context) {
        _prefs.value = _prefs.value.copy(
            customHeadColor = head,
            customBodyColor = body,
            customAccentColor = accent,
            isCustomMode = true,
            theme = AssistantTheme.CUSTOM
        )
        savePrefs(context)
        showMessage("I like this color!", AssistantState.EXCITED)
    }

    fun setCustomMode(enabled: Boolean, context: Context) {
        _prefs.value = _prefs.value.copy(
            isCustomMode = enabled,
            theme = if (enabled) AssistantTheme.CUSTOM else AssistantTheme.DEFAULT
        )
        savePrefs(context)
    }

    fun setFrequency(frequency: AssistantFrequency, context: Context) {
        _prefs.value = _prefs.value.copy(frequency = frequency)
        savePrefs(context)
    }

    fun resetPosition(context: Context) {
        _prefs.value = _prefs.value.copy(lastPosX = -1f, lastPosY = -1f)
        savePrefs(context)
    }

    fun loadPrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("AssistantPrefs_v2", Context.MODE_PRIVATE)
        _prefs.value = AssistantPrefs(
            isEnabled = sharedPref.getBoolean("enabled", true),
            isMuted = sharedPref.getBoolean("muted", false),
            frequency = AssistantFrequency.valueOf(sharedPref.getString("frequency", AssistantFrequency.MEDIUM.name) ?: AssistantFrequency.MEDIUM.name),
            theme = AssistantTheme.valueOf(sharedPref.getString("theme", AssistantTheme.DEFAULT.name) ?: AssistantTheme.DEFAULT.name),
            customHeadColor = AssistantColor.valueOf(sharedPref.getString("customHeadColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name),
            customBodyColor = AssistantColor.valueOf(sharedPref.getString("customBodyColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name),
            customAccentColor = AssistantColor.valueOf(sharedPref.getString("customAccentColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name),
            isCustomMode = sharedPref.getBoolean("isCustomMode", false),
            lastPosX = sharedPref.getFloat("posX", -1f),
            lastPosY = sharedPref.getFloat("posY", -1f)
        )
    }

    private fun savePrefs(context: Context) {
        val sharedPref = context.getSharedPreferences("AssistantPrefs_v2", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean("enabled", _prefs.value.isEnabled)
            putBoolean("muted", _prefs.value.isMuted)
            putString("frequency", _prefs.value.frequency.name)
            putString("theme", _prefs.value.theme.name)
            putString("customHeadColor", _prefs.value.customHeadColor.name)
            putString("customBodyColor", _prefs.value.customBodyColor.name)
            putString("customAccentColor", _prefs.value.customAccentColor.name)
            putBoolean("isCustomMode", _prefs.value.isCustomMode)
            putFloat("posX", _prefs.value.lastPosX)
            putFloat("posY", _prefs.value.lastPosY)
            apply()
        }
    }
}
