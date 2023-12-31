/*
* TimeLimit Copyright <C> 2019 Jonas Lochmann
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
package io.pivot.android.ui.manage.device.manage

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.data.model.Device
import io.pivot.android.data.model.User
import io.pivot.android.data.model.UserType
import io.pivot.android.databinding.MissingPermissionViewBinding
import io.pivot.android.livedata.mergeLiveData

object ActivityLaunchPermissionRequiredAndMissing {
    fun bind(
            view: MissingPermissionViewBinding,
            user: LiveData<User?>,
            device: LiveData<Device?>,
            lifecycleOwner: LifecycleOwner
    ) {
        view.title = view.root.context.getString(R.string.activity_launch_permission_required_and_missing_title)

        mergeLiveData(user, device).observe(lifecycleOwner, Observer { (user, device) ->
            view.showMessage = user?.type == UserType.Child && device?.missingPermissionAtQOrLater ?: false
        })
    }
}
