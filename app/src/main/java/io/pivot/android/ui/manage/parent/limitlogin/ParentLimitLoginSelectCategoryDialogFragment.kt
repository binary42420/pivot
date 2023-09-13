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

package io.pivot.android.ui.manage.parent.limitlogin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.data.model.UserType
import io.pivot.android.extensions.showSafe
import io.pivot.android.livedata.map
import io.pivot.android.livedata.switchMap
import io.pivot.android.sync.actions.UpdateUserLimitLoginCategory
import io.pivot.android.ui.fragment.BottomSheetSelectionListDialog
import io.pivot.android.ui.main.ActivityViewModelHolder

class ParentLimitLoginSelectCategoryDialogFragment: BottomSheetSelectionListDialog() {
    companion object {
        private const val DIALOG_TAG = "ParentLimitLoginSelectCategoryDialogFragment"
        private const val USER_ID = "userId"

        fun newInstance(userId: String) = ParentLimitLoginSelectCategoryDialogFragment().apply {
            arguments = Bundle().apply {
                putString(USER_ID, userId)
            }
        }
    }

    override val title: String get() = getString(R.string.parent_limit_login_title)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments!!.getString(USER_ID)!!
        val auth = (activity as ActivityViewModelHolder).getActivityViewModel()
        val logic = auth.logic
        val options = logic.database.userLimitLoginCategoryDao().getLimitLoginCategoryOptions(userId)

        options.switchMap { a ->
            auth.authenticatedUser.map { b ->
                a to b
            }
        }.observe(viewLifecycleOwner, Observer { (categoryList, user) ->
            if (user?.type != UserType.Parent) {
                dismissAllowingStateLoss(); return@Observer
            }

            val isUserItself = user.id == userId

            val hasSelection = categoryList.find { it.selected } != null

            clearList()

            categoryList.forEach { category ->
                addListItem(
                        label = getString(R.string.parent_limit_login_dialog_item, category.childTitle, category.categoryTitle),
                        checked = category.selected,
                        click = {
                            if (!category.selected) {
                                if (isUserItself) {
                                    auth.tryDispatchParentAction(
                                            UpdateUserLimitLoginCategory(
                                                    userId = userId,
                                                    categoryId = category.categoryId
                                            )
                                    )

                                    dismiss()
                                } else {
                                    LimitLoginRestrictedToUserItselfDialogFragment().show(parentFragmentManager)
                                }
                            } else {
                                dismiss()
                            }
                        }
                )
            }

            addListItem(
                    labelRes = R.string.parent_limit_login_dialog_no_selection,
                    checked = !hasSelection,
                    click = {
                        if (hasSelection) {
                            auth.tryDispatchParentAction(
                                    UpdateUserLimitLoginCategory(
                                            userId = userId,
                                            categoryId = null
                                    )
                            )
                        }

                        dismiss()
                    }
            )
        })
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, DIALOG_TAG)
}