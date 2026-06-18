package com.enosh.fincalc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE, HAPPY, THINKING, ERROR, EXCITED, WAVING, SHUSH
}

enum class AssistantMessageType {
    SPEECH, THOUGHT
}

enum class AssistantGender {
    MALE, FEMALE
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
    WHITE(0xFFFFFFFF),
    SOLID_BLUE(0xFF1A237E),
    SOLID_GREEN(0xFF1B5E20)
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
    READABLE_DARK(AssistantColor.SOLID_BLUE, AssistantColor.SOLID_BLUE, AssistantColor.CYAN, "Navy"),
    READABLE_LIGHT(AssistantColor.SOLID_GREEN, AssistantColor.SOLID_GREEN, AssistantColor.NEON, "Forest"),
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
    val lastPosY: Float = -1f,
    val gender: AssistantGender = AssistantGender.FEMALE,
    val isAnimated: Boolean = true
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
        "Try saving 10% of your money each month! 📈",
        "Keep track of your spending so you know where it goes. 💰",
        "The earlier you save, the more it earns! 🪙",
        "It's a good idea to have some emergency cash ready. 🏦",
        "Don't put all your money in one place. 🥚",
        "Even a small drop in interest rates can save you a lot! 📉",
        "Planning a trip? Use Smart Travel to split costs! ✈️",
        "Don't forget to finalize your trip to see the total! 🗺️",
        "Add friends to collaborate on travel expenses! 👥",
        "You can invite friends using your unique FinCalc ID! 🆔",
        "Scan a friend's QR code to connect instantly! 📱",
        "You can add extra returned money in Budget Planner using 'Add Extra Amount'. 💰",
        "Smart Business helps track income, targets, and payment sources. 📈",
        "You can chat only with accepted friends. 💬"
    )

    private val funFacts = listOf(
        "I'm mostly a calculator, but I try to be funny too 🤖",
        "Math is actually pretty cool! 😄",
        "I'm always ready to help. No sleep for me!",
        "Beep boop! Just thinking..."
    )

    private fun getPrefName(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "AssistantPrefs_$uid"
    }

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
            
            val finalState = if (_prefs.value.isMuted) AssistantState.SHUSH else state

            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = finalState
            }
            
            _isTyping.value = true
            delay(if (_prefs.value.isAnimated) 1000 else 0) 
            _isTyping.value = false
            _message.value = text
            
            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = finalState
            }
            
            delay(durationMs)
            _message.value = null
            if (_robotState.value != AssistantState.WAVING) {
                _robotState.value = AssistantState.IDLE
            }
        }
    }

    fun triggerWave() {
        if (!_prefs.value.isEnabled || !_prefs.value.isAnimated) return
        viewModelScope.launch {
            _robotState.value = AssistantState.WAVING
            delay(1500) 
            if (_message.value == null) {
                _robotState.value = AssistantState.IDLE
            } else {
                _robotState.value = if (_prefs.value.isMuted) AssistantState.SHUSH else AssistantState.HAPPY
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
        if (muted) {
            _robotState.value = AssistantState.SHUSH
        } else {
            _robotState.value = AssistantState.IDLE
        }
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

    fun setGender(gender: AssistantGender, context: Context) {
        _prefs.value = _prefs.value.copy(gender = gender)
        savePrefs(context)
        showMessage(if (gender == AssistantGender.MALE) "I'm your male assistant now! 👔" else "I'm your female assistant now! 👗", AssistantState.HAPPY)
    }

    fun setAnimated(animated: Boolean, context: Context) {
        _prefs.value = _prefs.value.copy(isAnimated = animated)
        savePrefs(context)
    }

    fun resetPosition(context: Context) {
        _prefs.value = _prefs.value.copy(lastPosX = -1f, lastPosY = -1f)
        savePrefs(context)
    }

    fun loadPrefs(context: Context) {
        try {
            val sharedPref = context.getSharedPreferences(getPrefName(), Context.MODE_PRIVATE)
            _prefs.value = AssistantPrefs(
                isEnabled = sharedPref.getBoolean("enabled", true),
                isMuted = sharedPref.getBoolean("muted", false),
                frequency = try { AssistantFrequency.valueOf(sharedPref.getString("frequency", AssistantFrequency.MEDIUM.name) ?: AssistantFrequency.MEDIUM.name) } catch (e: Exception) { AssistantFrequency.MEDIUM },
                theme = try { AssistantTheme.valueOf(sharedPref.getString("theme", AssistantTheme.DEFAULT.name) ?: AssistantTheme.DEFAULT.name) } catch (e: Exception) { AssistantTheme.DEFAULT },
                customHeadColor = try { AssistantColor.valueOf(sharedPref.getString("customHeadColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name) } catch (e: Exception) { AssistantColor.DEFAULT },
                customBodyColor = try { AssistantColor.valueOf(sharedPref.getString("customBodyColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name) } catch (e: Exception) { AssistantColor.DEFAULT },
                customAccentColor = try { AssistantColor.valueOf(sharedPref.getString("customAccentColor", AssistantColor.DEFAULT.name) ?: AssistantColor.DEFAULT.name) } catch (e: Exception) { AssistantColor.DEFAULT },
                isCustomMode = sharedPref.getBoolean("isCustomMode", false),
                lastPosX = sharedPref.getFloat("posX", -1f),
                lastPosY = sharedPref.getFloat("posY", -1f),
                gender = try { AssistantGender.valueOf(sharedPref.getString("gender", AssistantGender.FEMALE.name) ?: AssistantGender.FEMALE.name) } catch (e: Exception) { AssistantGender.FEMALE },
                isAnimated = sharedPref.getBoolean("isAnimated", true)
            )
        } catch (e: Exception) {
            android.util.Log.e("AssistantVM", "Error loading prefs", e)
        }
    }

    private fun savePrefs(context: Context) {
        val sharedPref = context.getSharedPreferences(getPrefName(), Context.MODE_PRIVATE)
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
            putString("gender", _prefs.value.gender.name)
            putBoolean("isAnimated", _prefs.value.isAnimated)
            apply()
        }
    }
}
