/*
 * Open TimeLimit Copyright <C> 2019 - 2021 Jonas Lochmann
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
package io.pivot.android.ui.diagnose

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import io.pivot.android.R
import io.pivot.android.databinding.DiagnoseClockFragmentBinding
import io.pivot.android.date.CalendarCache
import io.pivot.android.date.DateInTimezone
import io.pivot.android.date.getMinuteOfWeek
import io.pivot.android.livedata.liveDataFromFunction
import io.pivot.android.livedata.liveDataFromNullableValue
import io.pivot.android.livedata.map
import io.pivot.android.livedata.switchMap
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.ui.main.FragmentWithCustomTitle
import java.util.*

class DiagnoseClockFragment : Fragment(), FragmentWithCustomTitle {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DiagnoseClockFragmentBinding.inflate(inflater, container, false)
        val logic = DefaultAppLogic.with(requireContext())

        val timeZone = logic.deviceUserEntry.map { TimeZone.getTimeZone(it?.timeZone) ?: logic.timeApi.getSystemTimeZone() }
        val timestamp = liveDataFromFunction { logic.timeApi.getCurrentTimeInMillis() }
        val dateInTimezone = timeZone.switchMap { tz -> timestamp.map { ts -> DateInTimezone.newInstance(ts, tz) } }
        val minuteOfWeek = timeZone.switchMap { tz -> timestamp.map { ts -> getMinuteOfWeek(ts, tz) } }
        val timeOfDay = timeZone.switchMap { tz -> timestamp.map { ts ->
            val calendar = CalendarCache.getCalendar()

            calendar.firstDayOfWeek = Calendar.MONDAY

            calendar.timeZone = tz
            calendar.timeInMillis = ts

            String.format("%2d:%2d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        }}
        val dateString = timeZone.switchMap { tz -> timestamp.map { ts ->
            DateFormat.getDateFormat(context).apply {
                setTimeZone(tz)
            }.format(Date(ts))
        }}

        timestamp.observe(this, androidx.lifecycle.Observer { binding.epochalSeconds = it / 1000 })
        timeZone.observe(this, androidx.lifecycle.Observer { binding.timeZone = it.displayName })
        timeOfDay.observe(this, androidx.lifecycle.Observer { binding.timeOfDay = it })
        dateString.observe(this, androidx.lifecycle.Observer { binding.dateString = it })
        dateInTimezone.observe(this, androidx.lifecycle.Observer {
            binding.dayOfWeek = it.dayOfWeek
            binding.dayOfEpoch = it.dayOfEpoch.toLong()
        })
        minuteOfWeek.observe(this, androidx.lifecycle.Observer {
            binding.minuteOfWeek = it
        })

        return binding.root
    }

    override fun getCustomTitle(): LiveData<String?> = liveDataFromNullableValue("${getString(R.string.diagnose_clock_title)} < ${getString(R.string.about_diagnose_title)} < ${getString(R.string.main_tab_overview)}")
}
