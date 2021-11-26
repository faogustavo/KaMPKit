package co.touchlab.kampkit.ext

import com.squareup.sqldelight.db.SqlDriver

internal fun SqlDriver.reset() {
    val tableList = listOf("Breed")

    tableList.forEach { table ->
        execute(null, "delete from $table", 0) {}
        execute(null, "delete from sqlite_sequence where name='$table'", 0) {}
    }
}

internal fun SqlDriver.resetAndClose() {
    reset()
    close()
}
