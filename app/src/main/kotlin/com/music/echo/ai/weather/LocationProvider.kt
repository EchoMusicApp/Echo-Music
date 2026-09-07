package echo.music.iad1tya.ai.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

object LocationProvider {

    @SuppressLint("MissingPermission")
    suspend fun getLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            // Try LocationManager providers (GPS, Network, Passive)
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )

            var bestLocation: Location? = null

            for (provider in providers) {
                val isEnabled = try {
                    locationManager.isProviderEnabled(provider)
                } catch (e: Exception) {
                    false
                }
                if (isEnabled) {
                    val loc = try {
                        locationManager.getLastKnownLocation(provider)
                    } catch (e: Exception) {
                        null
                    }
                    if (loc != null) {
                        if (bestLocation == null || loc.time > bestLocation.time) {
                            bestLocation = loc
                        }
                    }
                }
            }

            if (bestLocation != null) {
                return@withContext bestLocation
            }

            // Reflection fallback for Google Play Services FusedLocationProviderClient if present
            try {
                val fusedClientClass = Class.forName("com.google.android.gms.location.LocationServices")
                val getFusedMethod = fusedClientClass.getMethod("getFusedLocationProviderClient", Context::class.java)
                val fusedClient = getFusedMethod.invoke(null, context)
                val getLastLocationMethod = fusedClient.javaClass.getMethod("getLastLocation")
                val task = getLastLocationMethod.invoke(fusedClient)

                val location = suspendTaskResult<Location>(task)
                if (location != null) return@withContext location
            } catch (e: Throwable) {
                // Ignore if Play Services location is not present
            }

            bestLocation
        } catch (e: Exception) {
            Timber.e(e, "Error retrieving location")
            null
        }
    }

    private suspend fun <T> suspendTaskResult(task: Any?): T? = withContext(Dispatchers.IO) {
        if (task == null) return@withContext null
        try {
            val isCompleteMethod = task.javaClass.getMethod("isComplete")
            val isSuccessfulMethod = task.javaClass.getMethod("isSuccessful")
            val getResultMethod = task.javaClass.getMethod("getResult")

            var attempts = 0
            while (attempts < 20) {
                val isComplete = isCompleteMethod.invoke(task) as? Boolean ?: false
                if (isComplete) {
                    val isSuccessful = isSuccessfulMethod.invoke(task) as? Boolean ?: false
                    return@withContext if (isSuccessful) {
                        @Suppress("UNCHECKED_CAST")
                        getResultMethod.invoke(task) as? T
                    } else null
                }
                delay(100)
                attempts++
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
