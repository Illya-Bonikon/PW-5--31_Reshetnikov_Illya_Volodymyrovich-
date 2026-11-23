package ua.kpi.building_audit

import android.Manifest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Проходить застосунок так, як це робить користувач, і знімає екран після кожного кроку.
 * Знімки робить системна утиліта screencap, тому в них потрапляють і мапа, і діалоги.
 * Потрібен тестовий сервер на комп'ютері-хості (10.0.2.2:5156).
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val shell = InstrumentationRegistry.getInstrumentation().uiAutomation

    /** Натискає кнопку, якщо вона є на екрані; інакше повертається системною кнопкою «Назад». */
    private fun tapIfShown(text: String) {
        val nodes = compose.onAllNodesWithText(text).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) compose.onAllNodesWithText(text).onFirst().performClick()
        else shell.executeShellCommand("input keyevent 4").close()
        pause(1_000)
    }

    private fun pause(ms: Long) {
        Thread.sleep(ms)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        pause(500)
        shell.executeShellCommand("mkdir -p /sdcard/Download/screens").close()
        shell.executeShellCommand("screencap -p /sdcard/Download/screens/$name.png").close()
        Thread.sleep(1_000)
    }

    private fun open(destination: Destination) {
        compose.onNodeWithContentDescription("Меню").performClick()
        pause(600)
        compose.onAllNodesWithText(destination.title).onLast().performClick()
        pause(1_500)
    }

    @Test
    fun tour() {
        compose.onNodeWithText("Аудитор").performClick()
        compose.onNodeWithText("Увійти").performClick()
        compose.waitUntil(20_000) { compose.onAllNodesWithText(Destination.PZ17.title).fetchSemanticsNodes().isNotEmpty() }

        // Реєстр завантажує будівлі з сервера в Room; решта екранів читає кеш.
        open(Destination.PZ9)
        pause(4_000)
        shot("pz09_registry")

        open(Destination.PZ13)
        pause(12_000)
        shot("pz13_telemetry")

        compose.onNodeWithContentDescription("Меню").performClick()
        pause(600)
        shot("drawer")
        compose.onAllNodesWithText(Destination.PZ14.title).onLast().performClick()
        pause(6_000)
        compose.onAllNodesWithText("Гуртожиток № 19").onLast().performClick()
        pause(4_000)
        shot("pz14_map_list")
        compose.onAllNodesWithText("Детальніше").onFirst()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        pause(1_500)
        shot("pz14_details")
        tapIfShown("До списку")
        pause(1_000)

        open(Destination.PZ15)
        compose.onAllNodesWithText("Житловий будинок").onFirst().performClick()
        compose.onAllNodesWithText("Потребує модернізації").onFirst().performClick()
        pause(1_000)
        shot("pz15_filters")

        open(Destination.PZ16)
        compose.onNodeWithText("Визначити моє місцезнаходження").performClick()
        pause(8_000)
        compose.onNodeWithText("≤ 1 км").performClick()
        pause(4_000)
        shot("pz16_gps")

        // Значення датчиків задає емулятор (adb emu sensor set), вологість 78 % дає ризик конденсату.
        open(Destination.PZ17)
        pause(5_000)
        shot("pz17_sensors")

        shell.executeShellCommand("cmd statusbar expand-notifications").close()
        pause(1_500)
        shot("pz18_shade")
        shell.executeShellCommand("cmd statusbar collapse").close()
        pause(800)

        open(Destination.PZ18)
        compose.onAllNodesWithText("Вихід за поріг").onFirst().performClick()
        pause(800)
        compose.onAllNodesWithText("Відкласти на 5 хв").onFirst().performClick()
        pause(800)
        shot("pz18_alerts")

        open(Destination.PZ19)
        compose.onNodeWithText("24 міс.").performClick()
        pause(1_500)
        shot("pz19_chart")

        open(Destination.PZ20)
        pause(3_000)
        shot("pz20_instruments")
    }
}
