package com.choiceparalysis.turntable

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests covering the core user flow:
 * Hub → Select method → Execute → View result
 *
 * Uses string resources to avoid locale-dependent failures.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // --- Helper ---

    private fun getString(resId: Int): String = context.getString(resId)
    private fun getString(resId: Int, vararg args: Any): String = context.getString(resId, *args)

    // --- Hub Screen ---

    @Test
    fun hubScreen_showsAllDecisionMethods() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_spin_wheel)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_coin)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_dice)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_answer_book)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_finger_roulette)).assertIsDisplayed()
    }

    @Test
    fun hubScreen_navigatesToSpinWheel() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_spin_wheel)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).assertIsDisplayed()
    }

    @Test
    fun hubScreen_navigatesToCoin() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_coin)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).assertIsDisplayed()
    }

    @Test
    fun hubScreen_navigatesToDice() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_dice)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).assertIsDisplayed()
    }

    @Test
    fun hubScreen_navigatesToAnswerBook() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_answer_book)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).assertIsDisplayed()
    }

    @Test
    fun hubScreen_navigatesToFingerRoulette() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_finger_roulette)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).assertIsDisplayed()
    }

    // --- Navigation Back ---

    @Test
    fun spinWheel_canNavigateBack() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_spin_wheel)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_spin_wheel)).assertIsDisplayed()
    }

    @Test
    fun coin_canNavigateBack() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_coin)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_coin)).assertIsDisplayed()
    }

    @Test
    fun dice_canNavigateBack() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_dice)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_dice)).assertIsDisplayed()
    }

    @Test
    fun answerBook_canNavigateBack() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_answer_book)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_answer_book)).assertIsDisplayed()
    }

    @Test
    fun fingerRoulette_canNavigateBack() {
        composeTestRule.onNodeWithText(getString(R.string.hub_card_finger_roulette)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(getString(R.string.cd_back)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_finger_roulette)).assertIsDisplayed()
    }

    // --- Bottom Navigation ---

    @Test
    fun bottomNav_canNavigateToHistory() {
        composeTestRule.onNodeWithText(getString(R.string.nav_history)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.nav_history)).assertIsDisplayed()
    }

    @Test
    fun bottomNav_canNavigateToSettings() {
        composeTestRule.onNodeWithText(getString(R.string.nav_settings)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.nav_settings)).assertIsDisplayed()
    }

    @Test
    fun bottomNav_canNavigateBackToHub() {
        composeTestRule.onNodeWithText(getString(R.string.nav_settings)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.nav_home)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.hub_card_spin_wheel)).assertIsDisplayed()
    }

    // --- Settings Screen ---

    @Test
    fun settingsScreen_showsVersion() {
        composeTestRule.onNodeWithText(getString(R.string.nav_settings)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.settings_version)).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_showsLanguageOption() {
        composeTestRule.onNodeWithText(getString(R.string.nav_settings)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.settings_language)).assertIsDisplayed()
    }

    // --- History Screen ---

    @Test
    fun historyScreen_showsTitle() {
        composeTestRule.onNodeWithText(getString(R.string.nav_history)).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(getString(R.string.nav_history)).assertIsDisplayed()
    }
}
