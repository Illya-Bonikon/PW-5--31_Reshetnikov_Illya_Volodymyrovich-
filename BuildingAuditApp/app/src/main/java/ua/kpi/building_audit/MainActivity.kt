package ua.kpi.building_audit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import ua.kpi.building_audit.auth.SessionState
import ua.kpi.building_audit.error.ErrorCenter
import ua.kpi.building_audit.practical10.ApiExplorerScreen
import ua.kpi.building_audit.practical11.LoginScreen
import ua.kpi.building_audit.practical11.SessionScreen
import ua.kpi.building_audit.practical12.DiagnosticsScreen
import ua.kpi.building_audit.practical13.TelemetryScreen
import ua.kpi.building_audit.practical14.BuildingDetailScreen
import ua.kpi.building_audit.practical14.MapListScreen
import ua.kpi.building_audit.practical15.FilterScreen
import ua.kpi.building_audit.practical16.GpsMapScreen
import ua.kpi.building_audit.practical17.SensorsScreen
import ua.kpi.building_audit.practical18.AlertsScreen
import ua.kpi.building_audit.practical19.ConsumptionScreen
import ua.kpi.building_audit.practical20.InstrumentsScreen
import ua.kpi.building_audit.practical9.RegistryScreen
import ua.kpi.building_audit.ui.components.DeveloperInfoDialog
import ua.kpi.building_audit.ui.theme.BuildingAuditTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EnergyAuditApp).container
        // Сповіщення відкриває застосунок одразу на потрібному екрані.
        val startRoute = intent.getStringExtra(EXTRA_ROUTE) ?: Destination.PZ17.route
        setContent {
            BuildingAuditTheme {
                AppRoot(container, startRoute)
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

/** Без активного сеансу показується вхід; завершення сеансу будь-де в застосунку повертає сюди. */
@Composable
fun AppRoot(container: AppContainer, startRoute: String) {
    val sessionState by container.sessionManager.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val state = sessionState) {
            is SessionState.Active -> MainHub(container.errorCenter, startRoute)
            is SessionState.Ended -> LoginScreen(ended = state)
            SessionState.SignedOut -> LoginScreen(ended = null)
        }
    }
}

/** Пункти бічного меню, згруповані за практичними роботами. */
enum class Destination(val route: String, val work: String, val title: String) {
    PZ9("pz9", "ПР3", "ПЗ 9: Repository"),
    PZ10("pz10", "ПР3", "ПЗ 10: REST API"),
    PZ11("pz11", "ПР3", "ПЗ 11: Сеанс"),
    PZ12("pz12", "ПР3", "ПЗ 12: Помилки"),
    PZ13("pz13", "ПР4", "ПЗ 13: Flow і корутини"),
    PZ14("pz14", "ПР4", "ПЗ 14: Список і мапа"),
    PZ15("pz15", "ПР4", "ПЗ 15: Фільтри"),
    PZ16("pz16", "ПР4", "ПЗ 16: GPS"),
    PZ17("pz17", "ПР5", "ПЗ 17: Сенсори"),
    PZ18("pz18", "ПР5", "ПЗ 18: Сповіщення"),
    PZ19("pz19", "ПР5", "ПЗ 19: Графіки"),
    PZ20("pz20", "ПР5", "ПЗ 20: Прилади")
}

private const val DETAIL_ROUTE = "building/{id}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHub(errorCenter: ErrorCenter, startRoute: String) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var criticalMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val title = Destination.entries.firstOrNull { it.route == currentRoute }?.title ?: "Енергоаудит будівель"

    // Єдине місце, де помилки з усіх екранів потрапляють до користувача.
    LaunchedEffect(errorCenter) {
        errorCenter.messages.collect { message ->
            if (message.critical) criticalMessage = message.text else snackbarHostState.showSnackbar(message.text)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Енергоаудит будівель", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                Destination.entries.groupBy { it.work }.forEach { (work, destinations) ->
                    HorizontalDivider()
                    Text(work, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
                    destinations.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.title) },
                            selected = destination.route == currentRoute,
                            onClick = {
                                navController.navigateTop(destination.route)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("screen_title")) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Filled.Menu, contentDescription = "Меню") }
                    },
                    actions = {
                        TextButton(onClick = { showDeveloperDialog = true }) { Text("Про розробника", fontWeight = FontWeight.SemiBold) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            NavHost(navController, startDestination = startRoute, modifier = Modifier.padding(innerPadding)) {
                composable(Destination.PZ9.route) { RegistryScreen() }
                composable(Destination.PZ10.route) { ApiExplorerScreen() }
                composable(Destination.PZ11.route) { SessionScreen() }
                composable(Destination.PZ12.route) { DiagnosticsScreen() }
                composable(Destination.PZ13.route) { TelemetryScreen() }
                composable(Destination.PZ14.route) { MapListScreen(onOpenDetails = { id -> navController.navigate("building/$id") }) }
                composable(Destination.PZ15.route) { FilterScreen() }
                composable(Destination.PZ16.route) { GpsMapScreen() }
                composable(Destination.PZ17.route) { SensorsScreen() }
                composable(Destination.PZ18.route) { AlertsScreen() }
                composable(Destination.PZ19.route) { ConsumptionScreen() }
                composable(Destination.PZ20.route) { InstrumentsScreen() }
                composable(DETAIL_ROUTE, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    BuildingDetailScreen(buildingId = entry.arguments?.getLong("id") ?: 0L, onBack = { navController.popBackStack() })
                }
            }
        }
    }

    if (showDeveloperDialog) DeveloperInfoDialog(onDismissRequest = { showDeveloperDialog = false })

    criticalMessage?.let { text ->
        AlertDialog(
            onDismissRequest = { criticalMessage = null },
            title = { Text("Помилка") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { criticalMessage = null }) { Text("Зрозуміло") } }
        )
    }
}

/** Перехід між пунктами меню без накопичення стеку: «Назад» завжди виходить із застосунку. */
private fun NavHostController.navigateTop(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
