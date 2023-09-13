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
package io.pivot.android.ui.manage.device.manage.permission

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import io.pivot.android.R
import io.pivot.android.extensions.showSafe
import io.pivot.android.integration.platform.SystemPermission
import io.pivot.android.integration.platform.SystemPermissionConfirmationLevel
import io.pivot.android.logic.DefaultAppLogic

class PermissionInfoConfirmDialog: DialogFragment() {
    companion object {
        private const val DIALOG_TAG = "PermissionInfoConfirmDialog"
        private const val EXTRA_PERMISSION = "permission"

        fun newInstance(permission: SystemPermission) = PermissionInfoConfirmDialog().apply {
            arguments = Bundle().apply {
                putSerializable(EXTRA_PERMISSION, permission)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val permission = requireArguments().getSerializable(EXTRA_PERMISSION) as SystemPermission
        val strings = PermissionInfoStrings.getFor(permission)

        return AlertDialog.Builder(requireContext(), theme)
            .setTitle(strings.title)
            .setMessage(strings.text)
            .setNegativeButton(R.string.generic_cancel, null)
            .setPositiveButton(R.string.wiazrd_next) { _, _ ->
                DefaultAppLogic.with(requireContext()).platformIntegration.openSystemPermissionScren(
                    requireActivity(), permission, SystemPermissionConfirmationLevel.PermissionInfo
                )
            }
            .create()
    }

    fun show(fragmentManager: FragmentManager) = showSafe(fragmentManager, DIALOG_TAG)
}