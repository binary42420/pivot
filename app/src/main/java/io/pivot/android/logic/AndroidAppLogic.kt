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
package io.pivot.android.logic

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.pivot.android.coroutines.runAsync
import io.pivot.android.data.RoomDatabase
import io.pivot.android.data.backup.DatabaseBackup
import io.pivot.android.integration.platform.android.AndroidIntegration
import io.pivot.android.integration.time.RealTimeApi
import java.util.concurrent.CountDownLatch

object AndroidAppLogic {
    private var instance: AppLogic? = null
    private val handler = Handler(Looper.getMainLooper())

    fun with(context: Context): AppLogic {
        val safeContext = context.applicationContext

        if (Looper.getMainLooper() == Looper.myLooper()) {
            // at the UI thread
            if (instance == null) {
                val isInitialized = MutableLiveData<Boolean>().apply { value = false }

                instance = AppLogic(
                        platformIntegration = AndroidIntegration(safeContext),
                        timeApi = RealTimeApi,
                        database = RoomDatabase.with(safeContext),
                        context = safeContext,
                        isInitialized = isInitialized
                )

                runAsync {
                    DatabaseBackup.with(safeContext).tryRestoreDatabaseBackupAsyncAndWait()
                    isInitialized.value = true
                }
            }
        } else {
            // at a background thread
            if (instance == null) {
                val latch = CountDownLatch(1)

                handler.post {
                    with(context)
                    latch.countDown()
                }

                latch.await()
            }
        }

        return instance!!
    }
}
