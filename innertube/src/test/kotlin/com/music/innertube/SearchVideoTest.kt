package com.music.innertube

import com.music.innertube.YouTube.SearchFilter
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SearchVideoTest {
    @Test
    fun testVideoSearch() = runBlocking {
        val result = YouTube.search("fakira", SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"))
        println("Result: $result")
        if (result.isSuccess) {
            println("Items size: ${result.getOrNull()?.items?.size}")
            result.getOrNull()?.items?.forEach {
                println("Item: $it")
            }
        } else {
            println("Error: ${result.exceptionOrNull()}")
        }
    }
}
