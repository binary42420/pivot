/*
 * TimeLimit Copyright <C> 2019 - 2022 Jonas Lochmann
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

package io.pivot.android.data.model.derived

import androidx.room.ColumnInfo
import androidx.room.Embedded
import io.pivot.android.data.model.ChildTask

data class FullChildTask(
        @Embedded
        val childTask: ChildTask,
        @ColumnInfo(name = "category_title")
        val categoryTitle: String,
        @ColumnInfo(name = "child_name")
        val childName: String,
        @ColumnInfo(name = "child_timezone")
        val childTimezone: String
)