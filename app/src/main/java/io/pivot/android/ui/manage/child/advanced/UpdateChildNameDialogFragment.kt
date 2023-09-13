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
package io.pivot.android.ui.manage.child.advanced

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.coroutines.runAsync
import io.pivot.android.data.model.User
import io.pivot.android.data.model.UserType
import io.pivot.android.extensions.showSafe
import io.pivot.android.livedata.waitForNullableValue
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.RenameChildAction
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.ui.main.ActivityViewModelHolder
import io.pivot.android.ui.util.EditTextBottomSheetDialog

class UpdateChildNameDialogFragment: EditTextBottomSheetDialog() {
    companion object {
        private const val TAG = "UpdateChildNameDialogFragment"
        private const val EXTRA_USER_ID = "userId"

        fun newInstance(userId: String) = UpdateChildNameDialogFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_USER_ID, userId)
            }
        }
    }

    val userId: String by lazy { arguments!!.getString(EXTRA_USER_ID)!! }
    val auth: ActivityViewModel by lazy {
        (activity as ActivityViewModelHolder).getActivityViewModel()
    }
    val userEntry: LiveData<User?> by lazy {
        DefaultAppLogic.with(context!!).database.user().getChildUserByIdLive(userId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth.authenticatedUser.observe(this, Observer {
            if (it?.type != UserType.Parent) {
                dismissAllowingStateLoss()
            }
        })

        userEntry.observe(this, Observer {
            if (it == null) {
                dismissAllowingStateLoss()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            runAsync {
                userEntry.waitForNullableValue()?.let { userEntry ->
                    binding.editText.setText(userEntry.name)
                    didInitField()
                }
            }
        }

        binding.title = getString(R.string.rename_child_title)
    }

    override fun go() {
        val newDeviceTitle = binding.editText.text.toString()

        if (newDeviceTitle.isBlank()) {
            dismiss()
        } else {
            if (
                    auth.tryDispatchParentAction(
                            RenameChildAction(
                                    childId = userId,
                                    newName = newDeviceTitle
                            )
                    )
            ) {
                dismiss()
            }
        }
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, TAG)
}
