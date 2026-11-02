package com.tomcat927.bemfacontrol

import android.app.Application
import com.tomcat927.bemfacontrol.data.network.BemfaApiFactory
import com.tomcat927.bemfacontrol.data.network.GitHubApiFactory
import com.tomcat927.bemfacontrol.data.repository.BemfaOutletRepository
import com.tomcat927.bemfacontrol.data.repository.RoomOrderSyncRepository
import com.tomcat927.bemfacontrol.data.repository.UpdateRepository
import com.tomcat927.bemfacontrol.data.settings.AppSettingsStore
import com.tomcat927.bemfacontrol.data.settings.DiagCacheStore

class BemfaControlApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val settingsStore = AppSettingsStore(application)
    private val gitHubApi = GitHubApiFactory.create()
    val outletRepository = BemfaOutletRepository(BemfaApiFactory.create())
    val diagCacheStore = DiagCacheStore(application)
    val updateRepository = UpdateRepository(application, gitHubApi)
    val roomOrderSyncRepository = RoomOrderSyncRepository(gitHubApi, settingsStore)
}
