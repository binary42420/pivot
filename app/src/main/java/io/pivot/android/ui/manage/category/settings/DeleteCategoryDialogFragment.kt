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
package io.pivot.android.ui.manage.category.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.data.model.Category
import io.pivot.android.data.model.UserType
import io.pivot.android.extensions.showSafe
import io.pivot.android.logic.AppLogic
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.DeleteCategoryAction
import io.pivot.android.ui.main.ActivityViewModel
import io.pivot.android.ui.main.getActivityViewModel
import io.pivot.android.ui.util.ConfirmDeleteDialogFragment

class DeleteCategoryDialogFragment: ConfirmDeleteDialogFragment() {
    companion object {
        private const val TAG = "DeleteCategoryDialogFragment"
        private const val CHILD_ID = "childId"
        private const val CATEGORY_ID = "categoryId"

        fun newInstance(childId: String, categoryId: String) = DeleteCategoryDialogFragment().apply {
            arguments = Bundle().apply {
                putString(CHILD_ID, childId)
                putString(CATEGORY_ID, categoryId)
            }
        }
    }

    private val appLogic: AppLogic by lazy { DefaultAppLogic.with(requireContext()) }
    private val auth: ActivityViewModel by lazy { getActivityViewModel(requireActivity()) }
    private val childId: String get() = requireArguments().getString(CHILD_ID)!!
    private val categoryId: String get() = requireArguments().getString(CATEGORY_ID)!!

    val categoryEntry: LiveData<Category?> by lazy {
        appLogic.database.category().getCategoryByChildIdAndId(
                categoryId = categoryId,
                childId = childId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth.authenticatedUser.observe(this, Observer {
            if (it?.type != UserType.Parent) {
                dismissAllowingStateLoss()
            }
        })

        categoryEntry.observe(this, Observer {
            if (it == null) {
                dismissAllowingStateLoss()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryEntry.observe(this, Observer {
            binding.text = getString(R.string.category_settings_delete_dialog, it?.title)
        })
    }

    override fun onConfirmDeletion() {
        auth.tryDispatchParentAction(DeleteCategoryAction(
                categoryId = categoryId
        ))

        dismiss()
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, TAG)
}
