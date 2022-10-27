package com.programmersbox.nameinfocompose

import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun networkTest() = runBlocking {
        val f = ApiService()

        val d = f.getAgeInfo("Jacob").getOrNull()
        val d1 = f.getGenderInfo("Jacob").getOrNull()
        val d2 = f.getNationalInfo("Jacob").getOrNull()
        println(d)
        println(d1)
        println(d2)
    }
}