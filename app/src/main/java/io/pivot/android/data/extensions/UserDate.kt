/*
 * Open TimeLimit Copyright <C> 2019 Jonas Lochmann
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
package io.pivot.android.data.extensions

import androidx.lifecycle.LiveData
import io.pivot.android.data.model.User
import io.pivot.android.date.DateInTimezone
import io.pivot.android.integration.time.TimeApi
import io.pivot.android.livedata.ignoreUnchanged
import io.pivot.android.livedata.liveDataFromFunction
import io.pivot.android.livedata.switchMap

fun LiveData<User?>.getDateLive(timeApi: TimeApi) = this.mapToTimezone().switchMap {
    timeZone ->

    liveDataFromFunction {
        DateInTimezone.newInstance(timeApi.getCurrentTimeInMillis(), timeZone)
    }
}.ignoreUnchanged()
