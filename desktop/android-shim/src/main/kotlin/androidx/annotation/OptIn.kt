package androidx.annotation

import kotlin.reflect.KClass

annotation class OptIn(vararg val markerClass: KClass<out Annotation>)
