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
package io.pivot.android.ui.manage.parent.delete

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.pivot.android.R
import io.pivot.android.async.Threads
import io.pivot.android.coroutines.executeAndWait
import io.pivot.android.coroutines.runAsync
import io.pivot.android.crypto.PasswordHashing
import io.pivot.android.data.model.UserType
import io.pivot.android.livedata.*
import io.pivot.android.logic.DefaultAppLogic
import io.pivot.android.sync.actions.RemoveUserAction
import io.pivot.android.ui.main.ActivityViewModel

class DeleteParentModel(application: Application): AndroidViewModel(application) {
    private val logic = DefaultAppLogic.with(application)
    private val database = logic.database

    private val isWorkingInternal = MutableLiveData<Boolean>().apply { value = false }
    private val parentUserIdLive = MutableLiveData<String>()
    private val activityViewModelLive = MutableLiveData<ActivityViewModel>()

    private val authenticatedUserLive = activityViewModelLive.switchMap { it.authenticatedUser }

    private val isLastWithoutLoginLimit = parentUserIdLive.switchMap { userId ->
        database.userLimitLoginCategoryDao().countOtherUsersWithoutLimitLoginCategoryLive(userId).map { it == 0L }
    }

    val statusLive = parentUserIdLive.switchMap { parentUserId ->
        authenticatedUserLive.switchMap { authenticatedUser ->
            isLastWithoutLoginLimit.map { lastWithoutLoginLimit ->
                if (authenticatedUser?.type != UserType.Parent) {
                    Status.NotAuthenticated
                } else {
                    if (authenticatedUser.id == parentUserId) {
                        Status.WrongAccount
                    } else if (lastWithoutLoginLimit) {
                        Status.LastWihtoutLoginLimit
                    } else {
                        Status.Ready
                    }
                }
            }
        }
    }

    val parentUserLive = parentUserIdLive.switchMap { parentUserId ->
        database.user().getParentUserByIdLive(parentUserId)
    }

    val isWorking = isWorkingInternal.castDown()

    fun init(activityViewModel: ActivityViewModel, parentUserId: String) {
        activityViewModelLive.value = activityViewModel
        parentUserIdLive.value = parentUserId
    }

    fun deleteUser(password: String) {
        if (isWorkingInternal.value == true) {
            return
        }

        isWorkingInternal.value = true

        runAsync {
            try {
                if (statusLive.waitForNonNullValue() != Status.Ready) {
                    throw IllegalStateException()
                }

                val userEntry = parentUserLive.waitForNullableValue()

                if (userEntry == null) {
                    Toast.makeText(getApplication(), R.string.error_general, Toast.LENGTH_SHORT).show()

                    return@runAsync
                }

                val isPasswordValid = Threads.crypto.executeAndWait {
                    PasswordHashing.validateSync(password, userEntry.password)
                }

                if (!isPasswordValid) {
                    Toast.makeText(getApplication(), R.string.login_snackbar_wrong, Toast.LENGTH_SHORT).show()

                    return@runAsync
                }

                activityViewModelLive.value!!.tryDispatchParentAction(
                        RemoveUserAction(
                            userId = userEntry.id
                        )
                )
            } finally {
                isWorkingInternal.value = false
            }
        }
    }
}

enum class Status {
    NotAuthenticated,
    WrongAccount,
    LastWihtoutLoginLimit,
    Ready
}
