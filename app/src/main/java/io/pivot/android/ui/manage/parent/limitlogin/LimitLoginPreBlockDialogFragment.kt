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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.pivot.android.coroutines.runAsync
import io.pivot.android.databinding.LimitLoginPreBlockDialogFragmentBinding
import io.pivot.android.extensions.showSafe
import io.pivot.android.livedata.waitForNullableValue
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.UpdateUserLimitLoginPreBlockDuration
import io.pivot.android.ui.main.getActivityViewModel
import io.pivot.android.ui.util.bind

class LimitLoginPreBlockDialogFragment: BottomSheetDialogFragment() {
    companion object {
        private const val DIALOG_TAG = "LimitLoginPreBlockDialogFragment"
        private const val USER_ID = "userId"

        fun newInstance(userId: String) = LimitLoginPreBlockDialogFragment().apply {
            arguments = Bundle().apply {
                putString(USER_ID, userId)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LimitLoginPreBlockDialogFragmentBinding.inflate(inflater, container, false)
        val logic = DefaultAppLogic.with(requireContext())
        val activity = getActivityViewModel(requireActivity())
        val userId = requireArguments().getString(USER_ID)!!

        fun updateConfirmButton() {
            val user = activity.authenticatedUser.value ?: return
            val duration = binding.timeSpanView.timeInMillis

            val userItself = user.id == userId
            val zeroDuration = duration == 0L

            binding.confirmButton.isEnabled = userItself || zeroDuration
            binding.otherUserInfo = !userItself
        }

        activity.authenticatedUser.observe(viewLifecycleOwner) {
            if (it == null) dismissAllowingStateLoss()

            updateConfirmButton()
        }

        binding.timeSpanView.bind(logic.database, viewLifecycleOwner) { updateConfirmButton() }

        binding.confirmButton.setOnClickListener {
            activity.tryDispatchParentAction(
                    UpdateUserLimitLoginPreBlockDuration(
                            userId = userId,
                            preBlockDuration = binding.timeSpanView.timeInMillis
                    )
            )

            dismissAllowingStateLoss()
        }

        if (savedInstanceState == null) {
            runAsync {
                logic.database.userLimitLoginCategoryDao().getByParentUserIdLive(userId).waitForNullableValue()?.let { item ->
                    binding.timeSpanView.timeInMillis = item.preBlockDuration
                }
            }
        }

        return binding.root
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, DIALOG_TAG)
}