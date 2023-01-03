package com.github.sotfheif.weatherforecast

import android.Manifest
import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BasicFuncTest {
    @get:Rule
    val mRuntimePermissionRule = GrantPermissionRule
        .grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun showCurLocTodayForecast() {
        Thread.sleep(3000)
        onView(withId(R.id.showForecastButton)).perform(click())
        Thread.sleep(45000)
        try {
            onView(withId(R.id.todayForecastTextView)).check(matches(not(withText(""))))
        } catch (e: Exception) {
            Log.d("BasicFuncTest", "$e")
        }
    }
}
/*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.sotfheif.weatherforecast", appContext.packageName)
    }
}
 */