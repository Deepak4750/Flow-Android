package com.deepak.flow.feature.home.presentation

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class GreetingTest {

    @Test
    fun morningGreeting() {
        assertEquals("Good morning", greetingForTime(LocalTime.of(8, 0)))
    }

    @Test
    fun afternoonGreeting() {
        assertEquals("Good afternoon", greetingForTime(LocalTime.of(14, 0)))
    }

    @Test
    fun eveningGreeting() {
        assertEquals("Good evening", greetingForTime(LocalTime.of(20, 0)))
    }

    @Test
    fun greetingWithNickname() {
        assertEquals("Good morning, D", greetingForTime(LocalTime.of(8, 0), "D"))
    }

    @Test
    fun greetingWithoutNickname() {
        assertEquals("Good morning", greetingForTime(LocalTime.of(8, 0), null))
        assertEquals("Good morning", greetingForTime(LocalTime.of(8, 0), ""))
    }
}
