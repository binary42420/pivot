/*
 * TimeLimit Copyright <C> 2019 - 2020 Jonas Lochmann
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
package io.pivot.android.ui.manage.device.manage.defaultuser

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.coroutines.runAsync
import io.pivot.android.data.model.Device
import io.pivot.android.data.model.User
import io.pivot.android.databinding.ManageDeviceDefaultUserBinding
import io.pivot.android.livedata.map
import io.pivot.android.livedata.switchMap
import io.pivot.android.sync.actions.SignOutAtDeviceAction
import io.pivot.android.sync.actions.apply.ApplyActionUtil
import io.pivot.android.ui.help.HelpDialogFragment
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.util.TimeTextUtil

object ManageDeviceDefaultUser {
    fun bind(
            view: ManageDeviceDefaultUserBinding,
            users: LiveData<List<User>>,
            lifecycleOwner: LifecycleOwner,
            device: LiveData<Device?>,
            isThisDevice: LiveData<Boolean>,
            auth: ActivityViewModel,
            fragmentManager: FragmentManager
    ) {
        val context = view.root.context

        view.titleView.setOnClickListener {
            HelpDialogFragment.newInstance(
                    title = R.string.manage_device_default_user_title,
                    text = R.string.manage_device_default_user_info
            ).show(fragmentManager)
        }

        device.switchMap { deviceEntry ->
            users.map { users ->
                deviceEntry to users.find { it.id == deviceEntry?.defaultUser }
            }
        }.observe(lifecycleOwner, Observer { (deviceEntry, defaultUser) ->
            view.hasDefaultUser = defaultUser != null
            view.isAlreadyUsingDefaultUser = defaultUser != null && deviceEntry?.currentUserId == defaultUser.id
            view.defaultUserTitle = defaultUser?.name
        })

        isThisDevice.observe(lifecycleOwner, Observer {
            view.isCurrentDevice = it
        })

        device.observe(lifecycleOwner, Observer { deviceEntry ->
            view.setDefaultUserButton.setOnClickListener {
                if (deviceEntry != null && auth.requestAuthenticationOrReturnTrue()) {
                    SetDeviceDefaultUserDialogFragment.newInstance(
                            deviceId = deviceEntry.id
                    ).show(fragmentManager)
                }
            }

            view.configureAutoLogoutButton.setOnClickListener {
                if (deviceEntry != null && auth.requestAuthenticationOrReturnTrue()) {
                    SetDeviceDefaultUserTimeoutDialogFragment
                            .newInstance(deviceId = deviceEntry.id)
                            .show(fragmentManager)
                }
            }

            val defaultUserTimeout = deviceEntry?.defaultUserTimeout ?: 0

            view.isAutomaticallySwitchingToDefaultUserEnabled = defaultUserTimeout != 0
            view.defaultUserSwitchText = if (defaultUserTimeout == 0)
                context.getString(R.string.manage_device_default_user_timeout_off)
            else
                context.getString(
                        R.string.manage_device_default_user_timeout_on,
                        if (defaultUserTimeout < 1000 * 60)
                            TimeTextUtil.seconds(defaultUserTimeout / 1000, context)
                        else
                            TimeTextUtil.time(defaultUserTimeout, context)
                )
        })

        view.switchToDefaultUserButton.setOnClickListener {
            runAsync {
                ApplyActionUtil.applyAppLogicAction(
                        action = SignOutAtDeviceAction,
                        appLogic = auth.logic,
                        ignoreIfDeviceIsNotConfigured = true
                )
            }
        }
    }
}