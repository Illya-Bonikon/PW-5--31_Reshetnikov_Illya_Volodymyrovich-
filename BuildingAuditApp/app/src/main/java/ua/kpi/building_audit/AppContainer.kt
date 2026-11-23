package ua.kpi.building_audit

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import ua.kpi.building_audit.auth.JwtDecoder
import ua.kpi.building_audit.auth.PrefsTokenStore
import ua.kpi.building_audit.auth.SessionManager
import ua.kpi.building_audit.data.local.AuditDatabase
import ua.kpi.building_audit.data.local.LocalBuildingDataSource
import ua.kpi.building_audit.data.remote.RemoteBuildingDataSource
import ua.kpi.building_audit.data.repository.AuthRepository
import ua.kpi.building_audit.data.repository.BuildingRepository
import ua.kpi.building_audit.data.repository.DiagnosticsRepository
import ua.kpi.building_audit.error.ErrorCenter
import ua.kpi.building_audit.error.ErrorMapper
import ua.kpi.building_audit.error.FileErrorJournal
import ua.kpi.building_audit.location.LocationTracker
import ua.kpi.building_audit.network.AndroidNetworkMonitor
import ua.kpi.building_audit.network.RetrofitFactory
import ua.kpi.building_audit.notifications.AlertCenter
import ua.kpi.building_audit.notifications.AlertNotifier
import ua.kpi.building_audit.sensors.SensorReader
import java.io.File

class EnergyAuditApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Сервери тайлів OpenStreetMap відхиляють запити без User-Agent застосунку.
        Configuration.getInstance().userAgentValue = packageName
    }
}

/** Ручне впровадження залежностей: усі об'єкти створюються тут один раз на процес. */
class AppContainer(context: Context) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val networkMonitor = AndroidNetworkMonitor(context)
    val sessionManager = SessionManager(PrefsTokenStore(context), JwtDecoder(gson))

    private val journal = FileErrorJournal(File(context.filesDir, "error_journal.jsonl"), gson)
    val errorCenter = ErrorCenter(ErrorMapper(gson), journal, networkMonitor)

    private val api = RetrofitFactory.create(
        baseUrl = BuildConfig.API_BASE_URL,
        gson = gson,
        connectivity = networkMonitor,
        tokenProvider = { sessionManager.currentToken },
        logBodies = BuildConfig.DEBUG
    )
    private val database = AuditDatabase.create(context)

    val buildingRepository = BuildingRepository(
        LocalBuildingDataSource(database.dao(), gson),
        RemoteBuildingDataSource(api),
        errorCenter
    )
    val authRepository = AuthRepository(api, sessionManager, errorCenter)
    val diagnosticsRepository = DiagnosticsRepository(api, errorCenter, journal)
    val locationTracker = LocationTracker(context)
    val sensorReader = SensorReader(context)
    val alertCenter = AlertCenter(context, AlertNotifier(context).apply { createChannels() })

    init {
        scope.launch { errorCenter.authFailures.collect { sessionManager.end(it) } }
        scope.launch {
            while (isActive) {
                sessionManager.checkExpiry()
                delay(1_000)
            }
        }
    }
}
