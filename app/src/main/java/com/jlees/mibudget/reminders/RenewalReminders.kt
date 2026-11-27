package com.jlees.mibudget.reminders

import kotlinx.coroutines.launch
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.MainActivity
import com.jlees.mibudget.R
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.domain.Reminder
import com.jlees.mibudget.data.db.cycle
import com.jlees.mibudget.domain.ReminderKind
import com.jlees.mibudget.domain.Reminders
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Renewal reminders run fully on-device: a JobScheduler job wakes once a day (around 9 AM),
 * auto-logs any subscription payments that came due, and posts notifications for upcoming
 * renewals and free trials that are about to end. No server, no push.
 */
object RenewalReminders {
    const val CHANNEL_ID = "renewals"
    const val EXTRA_SUBSCRIPTION_ID = "open_subscription_id"
    /** A one-off job for the next 9 AM that books the following one when it's done. */
    private const val JOB_DAILY = 2001
    /** A check right now (after editing a subscription, or from Settings). */
    private const val JOB_NOW = 2002
    private val REMIND_AT: LocalTime = LocalTime.of(9, 0)

    private fun jobs(context: Context) = context.getSystemService(android.app.job.JobScheduler::class.java)
    private fun job(context: Context, id: Int) =
        android.app.job.JobInfo.Builder(id, android.content.ComponentName(context, RenewalReminderJob::class.java))

    /**
     * Call on every app start: makes sure the next 9 AM check is booked (an existing booking is kept).
     * Each run books the next one for 9 AM the following day, so the time never drifts — a 24-hour
     * repeating job slowly slid later (Doze, reboots) and never came back. Jobs left by the WorkManager
     * this used before (its own service, gone now) are cancelled.
     */
    fun schedule(context: Context) {
        val js = jobs(context) ?: return
        runCatching {
            js.allPendingJobs.filter { it.service.packageName == context.packageName && it.service.className.startsWith("androidx.work") }
                .forEach { js.cancel(it.id) }
        }
        if (js.getPendingJob(JOB_DAILY) == null) scheduleNextDaily(context)
    }

    /** Books the check for the next 9 AM (called by the daily run once it's finished, and at start). */
    internal fun scheduleNextDaily(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REMIND_AT)
        if (!next.isAfter(now.plusMinutes(5))) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        runCatching {
            jobs(context)?.schedule(
                job(context, JOB_DAILY).setMinimumLatency(delay).setOverrideDeadline(delay + TimeUnit.HOURS.toMillis(2)).build()
            )
        }
    }

    internal fun isDaily(jobId: Int) = jobId == JOB_DAILY

    /**
     * Sends any due reminders, then auto-logs payments that came due. Reminders go FIRST: auto-logging
     * moves "next payment" forward, and a "renews today" reminder checked after that would be lost.
     * Used by the daily job and at app start.
     */
    suspend fun runCheck(context: Context) {
        val c = (context.applicationContext as MiApp).container
        val settings = c.settings.current()
        val today = LocalDate.now()
        if (settings.renewalReminders && canNotify(context)) {
            val subs = c.repository.allSubscriptions()
            val due = Reminders.due(subs, today, settings.reminderDaysBefore, c.settings.sentReminders())
            due.forEach { post(context, it) }
            c.settings.markRemindersSent(due.map { it.key })
        }
        c.repository.processDueSubscriptions(today)
        // the same daily job keeps the This Month widget current (a new month, a renewal logged)
        runCatching { com.jlees.mibudget.widget.MonthWidget.update(context) }
    }

    /** Runs the check immediately (e.g. after editing a subscription or from Settings). */
    fun checkNow(context: Context) {
        runCatching { jobs(context)?.schedule(job(context, JOB_NOW).setOverrideDeadline(0).build()) }
    }

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, context.getString(R.string.channel_renewals_name), NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.channel_renewals_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** POST_NOTIFICATIONS is granted (always true before Android 13). */
    fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Reminders can actually be seen: permission, app notifications on, and the reminders channel not off. */
    fun canNotify(context: Context): Boolean {
        val nm = NotificationManagerCompat.from(context)
        if (!hasPermission(context) || !nm.areNotificationsEnabled()) return false
        val channel = nm.getNotificationChannel(CHANNEL_ID) ?: return true // created on first use
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun text(r: Reminder): Pair<String, String> {
        val s = r.subscription
        val price = Money.format(s.amountCents)
        val whenText = when (r.daysUntil) {
            0L -> "today"
            1L -> "tomorrow"
            else -> "in ${r.daysUntil} days"
        }
        return when (r.kind) {
            ReminderKind.RENEWAL -> "${s.name} renews $whenText" to "$price will be charged${if (s.paymentMethod.isNotBlank()) " to ${s.paymentMethod}" else ""}."
            ReminderKind.TRIAL_END -> "${s.name} free trial ends $whenText" to "After that it's $price. Cancel before then if you don't want it."
            ReminderKind.TRIAL_ENDED -> (if (r.daysUntil == 0L) "${s.name} free trial ends today" else "${s.name} free trial has ended") to
                "You're now being charged $price${s.cycle.shortSuffix}. Open mi. Budget to keep it or cancel it."
        }
    }

    fun post(context: Context, r: Reminder) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val (title, body) = text(r)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SUBSCRIPTION_ID, r.subscription.id)
        }
        val pi = PendingIntent.getActivity(
            context, r.key.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_renewal)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(r.key.hashCode(), n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post — nothing to do.
        }
    }
}

/**
 * Runs the reminders check (RenewalReminders.runCheck). onStartJob returns at once and the check runs on the app's
 * background scope; the daily run then books tomorrow's, after finishing (booking the same job id while it runs would
 * stop it).
 */
class RenewalReminderJob : android.app.job.JobService() {
    private var work: kotlinx.coroutines.Job? = null

    override fun onStartJob(params: android.app.job.JobParameters): Boolean {
        work = (application as MiApp).container.appScope.launch {
            // a failure today mustn't stop tomorrow's run from being booked
            runCatching { RenewalReminders.runCheck(applicationContext) }
            jobFinished(params, false)
            if (RenewalReminders.isDaily(params.jobId)) RenewalReminders.scheduleNextDaily(applicationContext)
        }
        return true
    }

    override fun onStopJob(params: android.app.job.JobParameters): Boolean {
        work?.cancel()
        // stopped early (Android needed the resources): try the day's check again
        return RenewalReminders.isDaily(params.jobId)
    }
}
