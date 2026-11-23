package echo.music.dsp.controller

import androidx.media3.common.audio.AudioProcessor
import echo.music.dsp.audio.AutomixDuckAudioProcessor
import echo.music.dsp.audio.CustomEqualizerAudioProcessor
import echo.music.dsp.audio.StereoWidenerAudioProcessor
import echo.music.dsp.core.models.SavedEQProfile
import kotlinx.coroutines.flow.StateFlow

data class PlayerDspProcessors(
  val equalizer: CustomEqualizerAudioProcessor,
  val duckProcessor: AutomixDuckAudioProcessor,
  val stereoWidener: StereoWidenerAudioProcessor
) {
  fun asList(): List<AudioProcessor> = listOf(equalizer, duckProcessor, stereoWidener)
}

interface DspController {
  val activeProfile: StateFlow<SavedEQProfile?>
  val isEnabled: StateFlow<Boolean>
  val stereoWidth: StateFlow<Float>

  /** Returns list of all configured DSP processors for ExoPlayer audio chain */
  fun getAudioProcessors(): List<AudioProcessor>

  /** Factory for creating and registering a synchronized trio of processors for an ExoPlayer pipeline */
  fun createProcessors(): PlayerDspProcessors

  /** Unregisters processors when an ExoPlayer instance is released */
  fun releaseProcessors(processors: PlayerDspProcessors)

  fun addAudioProcessor(processor: CustomEqualizerAudioProcessor)
  fun removeAudioProcessor(processor: CustomEqualizerAudioProcessor)

  fun addStereoWidener(processor: StereoWidenerAudioProcessor)
  fun removeStereoWidener(processor: StereoWidenerAudioProcessor)

  fun addDuckProcessor(processor: AutomixDuckAudioProcessor)
  fun removeDuckProcessor(processor: AutomixDuckAudioProcessor)

  fun applyProfile(profile: SavedEQProfile): Result<Unit>
  fun disable()
  fun disableEqualizer()
  fun setStereoWidth(width: Float)
  fun setAutoMixDuck(duckFactor: Float)
  fun isInitialized(): Boolean
  fun release()
}
