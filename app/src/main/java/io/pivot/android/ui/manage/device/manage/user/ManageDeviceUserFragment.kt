/*
 * TimeLimit Copyright <C> 2019 - 2021 Jonas Lochmann
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
package io.pivot.android.ui.manage.device.manage.user


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import io.pivot.android.R
import io.pivot.android.data.model.Device
import io.pivot.android.databinding.ManageDeviceUserFragmentBinding
import io.pivot.android.livedata.ignoreUnchanged
import io.pivot.android.livedata.liveDataFromNonNullValue
import io.pivot.android.livedata.map
import io.pivot.android.livedata.mergeLiveData
import io.pivot.android.logic.AppLogic
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.SetDeviceUserAction
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.ui.main.ActivityViewModelHolder
import io.pivot.android.ui.main.AuthenticationFab
import io.pivot.android.ui.main.FragmentWithCustomTitle
import io.pivot.android.ui.manage.device.manage.defaultuser.ManageDeviceDefaultUser

class ManageDeviceUserFragment : Fragment(), FragmentWithCustomTitle {
    private val activity: ActivityViewModelHolder by lazy { getActivity() as ActivityViewModelHolder }
    private val logic: AppLogic by lazy { DefaultAppLogic.with(requireContext()) }
    private val auth: ActivityViewModel by lazy { activity.getActivityViewModel() }
    private val args: ManageDeviceUserFragmentArgs by lazy { ManageDeviceUserFragmentArgs.fromBundle(requireArguments()) }
    private val deviceEntry: LiveData<Device?> by lazy {
        logic.database.device().getDeviceById(args.deviceId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navigation = Navigation.findNavController(container!!)
        val binding = ManageDeviceUserFragmentBinding.inflate(inflater, container, false)
        val userEntries = logic.database.user().getAllUsersLive()
        var isBindingUserListSelection = false

        // auth
        AuthenticationFab.manageAuthenticationFab(
                fab = binding.fab,
                shouldHighlight = auth.shouldHighlightAuthenticationButton,
                authenticatedUser = auth.authenticatedUser,
                fragment = this,
                doesSupportAuth = liveDataFromNonNullValue(true)
        )

        // label, id
        val userListItems = ArrayList<Pair<String, String>>()

        fun bindUserListItems() {
            userListItems.forEachIndexed { index, listItem ->
                val oldRadio = binding.userList.getChildAt(index) as RadioButton?
                val radio = oldRadio ?: RadioButton(requireContext())

                radio.text = listItem.first

                if (oldRadio == null) {
                    radio.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    radio.id = index

                    binding.userList.addView(radio)
                }
            }

            while (binding.userList.childCount > userListItems.size) {
                binding.userList.removeViewAt(userListItems.size)
            }
        }

        fun bindUserListSelection() {
            isBindingUserListSelection = true

            val selectedUserId = deviceEntry.value?.currentUserId
            val selectedIndex = userListItems.indexOfFirst { it.second == selectedUserId }

            if (selectedIndex != -1) {
                binding.userList.check(selectedIndex)
            } else {
                val fallbackSelectedIndex = userListItems.indexOfFirst { it.second == "" }

                if (fallbackSelectedIndex != -1) {
                    binding.userList.check(fallbackSelectedIndex)
                }
            }

            isBindingUserListSelection = false
        }

        binding.handlers = object: ManageDeviceUserFragmentHandlers {
            override fun showAuthenticationScreen() {
                activity.showAuthenticationScreen()
            }
        }

        binding.userList.setOnCheckedChangeListener { _, checkedId ->
            if (isBindingUserListSelection) {
                return@setOnCheckedChangeListener
            }

            val userId = userListItems[checkedId].second
            val device = deviceEntry.value

            if (device != null && device.currentUserId != userId) {
                if (!auth.tryDispatchParentAction(
                                SetDeviceUserAction(
                                        deviceId = args.deviceId,
                                        userId = userId
                                )
                        )) {
                    bindUserListSelection()
                }
            }
        }

        deviceEntry.observe(this, Observer {
            device ->

            if (device == null) {
                navigation.popBackStack(R.id.overviewFragment, false)
            }
        })

        val isThisDevice = logic.deviceId.map { ownDeviceId -> ownDeviceId == args.deviceId }.ignoreUnchanged()

        mergeLiveData(deviceEntry, userEntries).observe(this, Observer {
            val (device, users) = it!!

            if (device != null && users != null) {
                userListItems.clear()
                userListItems.addAll(
                        users.map { user -> Pair(user.name, user.id) }
                )
                userListItems.add(Pair(getString(R.string.manage_device_current_user_none), ""))

                bindUserListItems()
                bindUserListSelection()
            }
        })

        ManageDeviceDefaultUser.bind(
                view = binding.defaultUser,
                device = deviceEntry,
                users = userEntries,
                lifecycleOwner = this,
                isThisDevice = isThisDevice,
                auth = auth,
                fragmentManager = parentFragmentManager
        )

        return binding.root
    }

    override fun getCustomTitle(): LiveData<String?> = deviceEntry.map { "${getString(R.string.manage_device_card_user_title)} < ${it?.name} < ${getString(R.string.main_tab_overview)}" }
}

interface ManageDeviceUserFragmentHandlers {
    fun showAuthenticationScreen()
}
