/*
 * Open TimeLimit Copyright <C> 2019 - 2020 Jonas Lochmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.pivot.android.ui.manage.child.advanced.managedisabletimelimits

import android.content.Context
import android.text.format.DateUtils
import androidx.fragment.app.FragmentActivity
import io.pivot.android.R
import io.pivot.android.data.model.User
import io.pivot.android.data.model.UserType
import io.pivot.android.date.DateInTimezone
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.SetUserDisableLimitsUntilAction
import io.pivot.android.ui.help.HelpDialogFragment
import io.pivot.android.ui.main.getActivityViewModel
import io.pivot.android.ui.view.ManageDisableTimelimitsViewHandlers
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import java.util.*

object ManageDisableTimelimitsViewHelper {
    fun createHandlers(childId: String, childTimezone: String, activity: FragmentActivity): ManageDisableTimelimitsViewHandlers {
        val auth = getActivityViewModel(activity)
        val logic = DefaultAppLogic.with(activity)

        fun getCurrentTime() = logic.timeApi.getCurrentTimeInMillis()

        return object : ManageDisableTimelimitsViewHandlers {
            override fun disableTimeLimitsForDuration(duration: Long) {
                auth.tryDispatchParentAction(
                        SetUserDisableLimitsUntilAction(
                                childId = childId,
                                timestamp = getCurrentTime() + duration
                        )
                )
            }

            override fun disableTimeLimitsForToday() {
                val dayOfEpoch = DateInTimezone.newInstance(getCurrentTime(), TimeZone.getTimeZone(childTimezone)).dayOfEpoch.toLong()

                val nextDayStart = LocalDate.ofEpochDay(dayOfEpoch)
                        .plusDays(1)
                        .atStartOfDay(ZoneId.of(childTimezone))
                        .toEpochSecond() * 1000

                auth.tryDispatchParentAction(
                        SetUserDisableLimitsUntilAction(
                                childId = childId,
                                timestamp = nextDayStart
                        )
                )
            }

            override fun disableTimeLimitsUntilSelectedDate() {
                if (auth.requestAuthenticationOrReturnTrue()) {
                    DisableTimelimitsUntilDateDialogFragment.newInstance(childId).show(activity.supportFragmentManager)
                }
            }

            override fun disableTimeLimitsUntilSelectedTimeOfToday() {
                if (auth.requestAuthenticationOrReturnTrue()) {
                    DisableTimelimitsUntilTimeDialogFragment.newInstance(childId).show(activity.supportFragmentManager)
                }
            }

            override fun enableTimeLimits() {
                auth.tryDispatchParentAction(
                        SetUserDisableLimitsUntilAction(
                                childId = childId,
                                timestamp = 0
                        )
                )
            }

            override fun showDisableTimeLimitsHelp() {
                HelpDialogFragment.newInstance(
                        title = R.string.manage_disable_time_limits_title,
                        text = R.string.manage_disable_time_limits_text
                ).show(activity.supportFragmentManager)
            }
        }
    }

    fun getDisabledUntilString(child: User?, currentTime: Long, context: Context): String? {
        if (child == null || child.type != UserType.Child || child.disableLimitsUntil == 0L || child.disableLimitsUntil < currentTime) {
            return null
        } else {
            return DateUtils.formatDateTime(
                    context,
                    child.disableLimitsUntil,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or
                            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_WEEKDAY
            )
        }
    }
}
