/*
 * Open TimeLimit Copyright <C> 2019 - 2020 Jonas Lochmann
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

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import io.pivot.android.R
import io.pivot.android.data.Database
import io.pivot.android.databinding.ManageParentCategoryBinding
import io.pivot.android.ui.help.HelpDialogFragment
import io.pivot.android.ui.main.ActivityViewModel

object ParentCategoryView {
    fun bind(
            binding: ManageParentCategoryBinding,
            auth: ActivityViewModel,
            lifecycleOwner: LifecycleOwner,
            categoryId: String,
            childId: String,
            database: Database,
            fragmentManager: FragmentManager
    ) {
        binding.titleView.setOnClickListener {
            HelpDialogFragment.newInstance(
                    title = R.string.category_settings_parent_category_title,
                    text = R.string.category_settings_parent_category_intro
            ).show(fragmentManager)
        }

        database.category().getCategoriesByChildId(childId).observe(lifecycleOwner, Observer { categories ->
            val ownCategory = categories.find { it.id == categoryId }
            val parentCategory = categories.find { it.id == ownCategory?.parentCategoryId }

            binding.parentCategoryTitle = parentCategory?.title
        })

        binding.selectParentButton.setOnClickListener {
            if (auth.requestAuthenticationOrReturnTrueAllowChild(childId)) {
                SelectParentCategoryDialogFragment.newInstance(
                        childId = childId,
                        categoryId = categoryId
                ).show(fragmentManager)
            }
        }
    }
}