package com.zelcar.app

import android.app.Application
import com.zelcar.app.billing.BillingManager
import com.zelcar.app.data.LocalSyncService
import com.zelcar.app.data.LocalTransferService
import com.zelcar.app.data.Prefs
import com.zelcar.app.data.Repository
import com.zelcar.app.data.SyncService
import com.zelcar.app.data.TransferService
import com.zelcar.app.data.ZelcarDatabase
import com.zelcar.app.work.Notifications
import com.zelcar.app.work.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ZelcarApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: Repository
        private set
    lateinit var billing: BillingManager
        private set
    val sync: SyncService = LocalSyncService()
    val transfer: TransferService = LocalTransferService()

    override fun onCreate() {
        super.onCreate()
        val prefs = Prefs(this)
        repository = Repository(ZelcarDatabase.get(this), prefs)
        billing = BillingManager(this, appScope, prefs).also { it.start() }
        Notifications.createChannels(this)
        ReminderWorker.schedule(this)
    }
}
