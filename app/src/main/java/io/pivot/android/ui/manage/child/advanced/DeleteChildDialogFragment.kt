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
package io.pivot.android.ui.manage.child.advanced

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.data.model.User
import io.pivot.android.data.model.UserType
import io.pivot.android.extensions.showSafe
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.RemoveUserAction
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.ui.main.ActivityViewModelHolder
import io.pivot.android.ui.util.ConfirmDeleteDialogFragment

class DeleteChildDialogFragment: ConfirmDeleteDialogFragment() {
    companion object {
        private const val DIALOG_TAG = "DeleteChildDialogFragment"
        private const val CHILD_ID = "childId"

        fun newInstance(childId: String) = DeleteChildDialogFragment().apply {
            arguments = Bundle().apply {
                putString(CHILD_ID, childId)
            }
        }
    }

    val auth: ActivityViewModel by lazy {
        (activity as ActivityViewModelHolder).getActivityViewModel()
    }
    val childId: String by lazy { arguments!!.getString(CHILD_ID)!! }
    val userEntry: LiveData<User?> by lazy {
        DefaultAppLogic.with(context!!).database.user().getChildUserByIdLive(childId)
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

        userEntry.observe(this, Observer {
            binding.text = getString(R.string.delete_child_text, it?.name)
        })
    }

    override fun onConfirmDeletion() {
        auth.tryDispatchParentAction(
                RemoveUserAction(
                        userId = childId
                )
        )

        dismiss()
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, DIALOG_TAG)
}
