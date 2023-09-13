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
package io.pivot.android.ui.manage.device.manage.defaultuser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.pivot.android.R
import io.pivot.android.data.Database
import io.pivot.android.data.model.UserType
import io.pivot.android.databinding.BottomSheetSelectionListBinding
import io.pivot.android.extensions.showSafe
import io.pivot.android.livedata.ignoreUnchanged
import io.pivot.android.livedata.map
import io.pivot.android.livedata.switchMap
import io.pivot.android.logic.AppLogic
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.SetDeviceDefaultUserAction
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.ui.main.ActivityViewModelHolder

class SetDeviceDefaultUserDialogFragment: BottomSheetDialogFragment() {
    companion object {
        private const val EXTRA_DEVICE_ID = "deviceId"
        private const val DIALOG_TAG = "sddudf"

        fun newInstance(deviceId: String) = SetDeviceDefaultUserDialogFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_DEVICE_ID, deviceId)
            }
        }
    }

    val deviceId: String by lazy { arguments!!.getString(EXTRA_DEVICE_ID)!! }
    val logic: AppLogic by lazy { DefaultAppLogic.with(context!!) }
    val database: Database by lazy { logic.database }
    val auth: ActivityViewModel by lazy { (activity as ActivityViewModelHolder).getActivityViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth.authenticatedUser.observe(this, Observer {
            if (it?.type != UserType.Parent) {
                dismissAllowingStateLoss()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BottomSheetSelectionListBinding.inflate(inflater, container, false)

        binding.title = getString(R.string.manage_device_default_user_title)

        val list = binding.list
        val users = database.user().getAllUsersLive()
        val deviceEntry = database.device().getDeviceById(deviceId)
        val currentDefaultUserId = deviceEntry.map { it?.defaultUser }.ignoreUnchanged()

        currentDefaultUserId.switchMap { v1 ->
            users.map { v2 -> v1 to v2 }
        }.observe(this, Observer { (defaultUserId, userList) ->
            list.removeAllViews()

            fun buildRow(): CheckedTextView = LayoutInflater.from(context!!).inflate(
                    android.R.layout.simple_list_item_single_choice,
                    list,
                    false
            ) as CheckedTextView

            val hasDefaultUser = userList.find { it.id == defaultUserId } != null

            userList.forEach { user ->
                buildRow().let { row ->
                    row.text = user.name
                    row.isChecked = defaultUserId == user.id
                    row.setOnClickListener {
                        auth.tryDispatchParentAction(
                                SetDeviceDefaultUserAction(
                                        deviceId = deviceId,
                                        defaultUserId = user.id
                                )
                        )

                        dismiss()
                    }

                    list.addView(row)
                }
            }

            buildRow().let { row ->
                row.setText(R.string.manage_device_default_user_selection_none)
                row.isChecked = !hasDefaultUser
                row.setOnClickListener {
                    auth.tryDispatchParentAction(
                            SetDeviceDefaultUserAction(
                                    deviceId = deviceId,
                                    defaultUserId = ""
                            )
                    )

                    dismiss()
                }

                list.addView(row)
            }
        })

        return binding.root
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, DIALOG_TAG)
}