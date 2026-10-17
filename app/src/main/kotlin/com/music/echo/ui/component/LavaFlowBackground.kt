package echo.music.iad1tya.ui.component

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush

private const val LAVA_SHADER =
  """
uniform float2 resolution;
uniform float time;

float hash(float2 p) { return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
float noise(float2 p) {
  float2 i = floor(p);
  float2 f = fract(p);
  f = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), f.x),
             mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), f.x), f.y);
}
float fbm(float2 p) {
  float v = 0.0;
  float a = 0.5;
  for (int i = 0; i < 4; i++) {
    v += a * noise(p);
    p = p * 2.03 + float2(17.3, 9.1);
    a *= 0.5;
  }
  return v;
}

half4 main(float2 fc) {
  float2 uv = fc / resolution;
  float2 p = float2(uv.x * resolution.x / resolution.y, uv.y) * 2.1;
  float t = time * 0.10;

  // thick, creamy molten flow: two warped layers drifting downward
  float2 q = float2(fbm(p + float2(0.0, -t)), fbm(p + float2(5.2, 1.3) - float2(0.0, t * 0.7)));
  float2 r = float2(fbm(p + 3.0 * q + float2(1.7, 9.2) + t * 0.5),
                    fbm(p + 3.0 * q + float2(8.3, 2.8) - t * 0.4));
  float f = fbm(p + 3.2 * r - float2(0.0, t * 1.4));

  // cooling crust vs hot cream
  float3 crust  = float3(0.06, 0.015, 0.01);
  float3 ember  = float3(0.62, 0.08, 0.02);
  float3 fire   = float3(1.00, 0.38, 0.04);
  float3 gold   = float3(1.00, 0.72, 0.18);
  float3 cream  = float3(1.00, 0.93, 0.70);

  float3 col = mix(crust, ember, smoothstep(0.18, 0.46, f));
  col = mix(col, fire, smoothstep(0.42, 0.66, f));
  col = mix(col, gold, smoothstep(0.62, 0.82, f));
  col = mix(col, cream, smoothstep(0.80, 0.98, f) * 0.85);

  // glowing seams where the flow folds over itself
  float seam = pow(1.0 - abs(2.0 * fract(f * 3.0) - 1.0), 6.0);
  col += float3(1.0, 0.35, 0.05) * seam * 0.18 * smoothstep(0.3, 0.7, f);

  // slow pulse + hotter toward the bottom, darker toward the top
  col *= 0.82 + 0.18 * sin(time * 0.8 + f * 6.0);
  col *= 0.55 + 0.65 * uv.y;
  return half4(half3(col), 1.0);
}
"""

/**
 * Player "Lava flow" background: thick molten lava with a creamy, slowly flowing surface and fire-lit
 * seams. On Android 13+ it is a real-time GPU shader; older phones get the cracked-lava canvas.
 */
@Composable
fun LavaFlowBackground(modifier: Modifier = Modifier.fillMaxSize()) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    LavaShaderLayer(modifier)
  } else {
    LavaBackground(modifier)
  }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LavaShaderLayer(modifier: Modifier) {
  // If a device rejects the shader for any reason, fall back to the canvas lava instead of crashing.
  val shader = remember { runCatching { RuntimeShader(LAVA_SHADER) }.getOrNull() }
  if (shader == null) {
    LavaBackground(modifier)
    return
  }
  val brush = remember(shader) { ShaderBrush(shader) }
  var time by remember { mutableFloatStateOf(0f) }
  LaunchedEffect(Unit) {
    var last = 0L
    while (true) {
      withFrameNanos { now ->
        // ~30 fps keeps the GPU cost low
        if (now - last >= 33_000_000L) {
          last = now
          time = ((now / 1_000_000L) % 600_000L) / 1000f
        }
      }
    }
  }
  Canvas(modifier) {
    shader.setFloatUniform("resolution", size.width, size.height)
    shader.setFloatUniform("time", time)
    drawRect(brush)
  }
}
