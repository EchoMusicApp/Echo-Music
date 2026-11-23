package echo.music.dsp.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import echo.music.dsp.core.models.ParametricEQ
import echo.music.dsp.core.models.SavedEQProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@Singleton
class EQProfileRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("nanosonic_eq_profiles", Context.MODE_PRIVATE)

  private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
  }

  private val _profiles = MutableStateFlow<List<SavedEQProfile>>(emptyList())
  val profiles: StateFlow<List<SavedEQProfile>> = _profiles.asStateFlow()

  private val _activeProfile = MutableStateFlow<SavedEQProfile?>(null)
  val activeProfile: StateFlow<SavedEQProfile?> = _activeProfile.asStateFlow()

  companion object {
    private const val KEY_PROFILES = "eq_profiles"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    private const val TAG = "EQProfileRepository"
  }

  init {
    loadProfiles()
  }

  private fun loadProfiles() {
    try {
      val profilesJson = prefs.getString(KEY_PROFILES, null)
      if (profilesJson != null) {
        val loadedProfiles = json.decodeFromString<List<SavedEQProfile>>(profilesJson)
        _profiles.value = loadedProfiles

        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        _activeProfile.value = loadedProfiles.find { it.id == activeId }
      }
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, "Error loading EQ profiles")
      _profiles.value = emptyList()
      _activeProfile.value = null
    }
  }

  suspend fun saveProfile(profile: SavedEQProfile) =
    withContext(Dispatchers.IO) {
      val currentProfiles = _profiles.value.toMutableList()
      val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }

      if (existingIndex >= 0) {
        currentProfiles[existingIndex] = profile
      } else {
        currentProfiles.add(profile)
      }

      val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
      prefs.edit().putString(KEY_PROFILES, profilesJson).apply()
      _profiles.value = currentProfiles
    }

  suspend fun deleteProfile(profileId: String) =
    withContext(Dispatchers.IO) {
      val currentProfiles = _profiles.value.toMutableList()
      currentProfiles.removeAll { it.id == profileId }

      val profilesJson = json.encodeToString<List<SavedEQProfile>>(currentProfiles)
      prefs.edit().putString(KEY_PROFILES, profilesJson).apply()

      if (_activeProfile.value?.id == profileId) {
        _activeProfile.value = null
        prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
      }

      _profiles.value = currentProfiles
    }

  suspend fun setActiveProfile(profileId: String?) =
    withContext(Dispatchers.IO) {
      val currentProfiles = _profiles.value

      if (profileId == null) {
        _activeProfile.value = null
        prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
      } else {
        val profile = currentProfiles.find { it.id == profileId }
        _activeProfile.value = profile
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
      }
    }

  fun getAllProfiles(): List<SavedEQProfile> {
    return _profiles.value
  }

  fun getActiveProfile(): SavedEQProfile? {
    return _activeProfile.value
  }

  suspend fun importCustomProfile(name: String, parametricEQ: ParametricEQ) =
    withContext(Dispatchers.IO) {
      val id = "custom_${System.currentTimeMillis()}_${name.hashCode()}"

      val customProfile =
        SavedEQProfile(
          id = id,
          name = name,
          deviceModel = name,
          bands = parametricEQ.bands,
          preamp = parametricEQ.preamp,
          isActive = false,
          isCustom = true
        )

      saveProfile(customProfile)
    }

  fun getSortedProfiles(): List<SavedEQProfile> {
    return _profiles.value.filter { it.isCustom }.sortedByDescending { it.addedTimestamp }
  }
}
