package `in`.bitotsav

import `in`.bitotsav.events.data.EventRepository
import `in`.bitotsav.feed.data.FeedRepository
import `in`.bitotsav.koin.repositoriesModule
import `in`.bitotsav.koin.retrofitModule
import `in`.bitotsav.koin.sharedPrefsModule
import `in`.bitotsav.koin.viewModelsModule
import `in`.bitotsav.notification.utils.createNotificationChannels
import `in`.bitotsav.shared.utils.getWork
import `in`.bitotsav.shared.utils.getWorkNameForTeamWorker
import `in`.bitotsav.shared.utils.scheduleUniqueWork
import `in`.bitotsav.shared.utils.startReminderWork
import `in`.bitotsav.shared.workers.*
import `in`.bitotsav.teams.championship.data.ChampionshipTeamRepository
import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.*

class Bitotsav19 : Application() {
    companion object {
        private const val IS_FIRST_RUN = "isFirstRun"
        private const val TAG = "Bitotsav19"
    }

    override fun onCreate() {
        super.onCreate()

        // Subscribe this device to "global" topic
        FirebaseMessaging.getInstance().subscribeToTopic("global")
            .addOnCompleteListener { task ->
                var msg = "Subscription to global successful"
                if (!task.isSuccessful) {
                    msg = "Subscription to global not successful"
                }
                Log.d(TAG, msg)
            }

        startKoin {
            androidContext(this@Bitotsav19)
            // Enable logging, log.INFO by default
            androidLogger()
            modules(repositoriesModule, retrofitModule, viewModelsModule, sharedPrefsModule)
        }

        // Initialise Reminder worker
        checkAndScheduleReminderWork()

        if (get<SharedPreferences>().getBoolean(IS_FIRST_RUN, true)) {
            init()
            get<SharedPreferences>().edit().putBoolean(IS_FIRST_RUN, false).apply()
        }

        // Refresh events and fetch new feeds on each run
        val eventWork =
            getWork<EventWorker>(workDataOf("type" to EventWorkType.FETCH_ALL_EVENTS.name))
        val feedWork = getWork<FeedWorker>(
            workDataOf("type" to FeedWorkType.FETCH_FEEDS.name)
        )
        WorkManager.getInstance().beginWith(eventWork).then(feedWork).enqueue()
    }

    // Place code which needs to run on first run only here
    private fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(this)
        }

        get<EventRepository>().getEventsFromLocalJsonAsync().invokeOnCompletion {
            get<FeedRepository>().getFeedsFromLocalJson()
        }
        get<ChampionshipTeamRepository>().getTeamsFromLocalJson()
        scheduleUniqueWork<TeamWorker>(
            workDataOf("type" to TeamWorkType.FETCH_ALL_TEAMS.name),
            getWorkNameForTeamWorker(TeamWorkType.FETCH_ALL_TEAMS)
        )
    }

    /**
     * Start the reminder work if Bitotsav is not yet over.
     * This method is called on each run as insurance that the Reminder worker does run on all devices. Reminder on
     * some Chinese OEMs may fail without this strategy.
     */
    private fun checkAndScheduleReminderWork() {
        val currentTimestamp = System.currentTimeMillis()
        val calendar = GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"))
        calendar.set(2019, 1, 17, 20, 0)
        val endOfBitotsav = calendar.timeInMillis
        if (currentTimestamp < endOfBitotsav)
            startReminderWork()
    }
}

// Global TODOs
// TODO [WARN]: Figure out if lifecycleOwner passed to data binding should be
//  viewLifecycleOwner or fragment.this
// TODO [WARN]: Figure out if app:layout_behavior="@string/appbar_scrolling_view_behavior" can
//  break in various layout files.
// TODO [Refactor]: Inject all services as AuthenticationService is right now
// TODO [Refactor]: Try removing as many observers from fragments as possible, observing should
//  preferably be done by data binding view only.
// TODO [WARN]: Figure out which fragment lifecycle method should contain what code, instead of putting
//  everything in onCreateView. Might lead to increased performance.
