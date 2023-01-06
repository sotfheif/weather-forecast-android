package com.github.sotfheif.weatherforecast

import android.Manifest
import android.os.Build
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class BasicFuncTest {
    @get:Rule
    val mRuntimePermissionRule: GrantPermissionRule = GrantPermissionRule
        .grant(Manifest.permission.ACCESS_FINE_LOCATION)

    @get:Rule
    val activity = ActivityScenarioRule(MainActivity::class.java)


    @Test
    fun showCurLocTodayForecast() {
        //turn on location services
        if (Build.VERSION.SDK_INT >= 29)
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_mode 3").close()
        else {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_providers_allowed +network")
                .close()
            Thread.sleep(3_000)
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("settings put secure location_providers_allowed +gps")
                .close()
        }
        //
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global window_animation_scale 0")
            .close()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global transition_animation_scale 0")
            .close()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global animator_duration_scale 0")
            .close()
        Thread.sleep(500)
        onView(withId(R.id.showForecastButton)).perform(click())
        Thread.sleep(35000)
        onView(withId(R.id.todayForecastTextView)).check(matches(not(withText(""))))
        onView(withId(R.id.weekForecastButton)).perform(click())
        Thread.sleep(5000)
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    0,
                    hasDescendant(withSubstring("to"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    0,
                    hasDescendant(withSubstring("-"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).perform(swipeUp())
        Thread.sleep(2000)
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    6,
                    hasDescendant(withSubstring("to"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    6,
                    hasDescendant(withSubstring("-"))
                )
            )
        )
    }

    @Test
    fun showSelLocTodayForecast() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global window_animation_scale 0")
            .close()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global transition_animation_scale 0")
            .close()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("settings put global animator_duration_scale 0")
            .close()
        Thread.sleep(500)
        onView(withId(R.id.textFieldInput)).perform(typeText("Paris"), closeSoftKeyboard())
        onView(withId(R.id.select_city_button)).perform(click())
        Thread.sleep(15000)
        onView(withId(R.id.recycler_view)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                click()
            )
        )
        onView(withId(R.id.showForecastButton)).perform(click())
        Thread.sleep(10000)
        onView(withId(R.id.todayForecastTextView)).check(matches(not(withText(""))))
        onView(withId(R.id.weekForecastButton)).perform(click())
        Thread.sleep(5000)
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    0,
                    hasDescendant(withSubstring("to"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    0,
                    hasDescendant(withSubstring("-"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).perform(swipeUp())
        Thread.sleep(2000)
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    6,
                    hasDescendant(withSubstring("to"))
                )
            )
        )
        onView(withId(R.id.recycler_view)).check(
            matches(
                Utils.atPosition(
                    6,
                    hasDescendant(withSubstring("-"))
                )
            )
        )
    }
}

object Utils {
    fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> =
        object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("has item at position $position: ")
                itemMatcher.describeTo(description)
            }

            override fun matchesSafely(view: RecyclerView): Boolean {
                val viewHolder = view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
                return itemMatcher.matches(viewHolder.itemView)
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