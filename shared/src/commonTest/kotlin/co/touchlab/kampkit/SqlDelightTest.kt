package co.touchlab.kampkit

import co.touchlab.kampkit.db.Breed
import co.touchlab.kermit.Logger
import com.squareup.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SqlDelightTest : BaseTest() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var testDbConnection: SqlDriver

    private suspend fun DatabaseHelper.insertBreed(name: String) {
        insertBreeds(listOf(Breed(id = 1, name = name, favorite = 0L)))
    }

    @BeforeTest
    fun setup() = runTest {
        dbHelper = DatabaseHelper(
            testDbConnection(),
            Logger,
            Dispatchers.Default
        )
        dbHelper.deleteAll()
        dbHelper.insertBreed("Beagle")
    }

    @Test
    fun selectAllItemsSuccess() = runTest {
        val breeds = dbHelper.selectAllItems().first()
        assertNotNull(
            breeds.find { it.name == "Beagle" },
            "Could not retrieve Breed"
        )
    }

    @Test
    fun selectItemByIdSuccess() = runTest {
        val breeds = dbHelper.selectAllItems().first()
        val firstBreed = breeds.first()
        assertNotNull(
            dbHelper.selectById(firstBreed.id),
            "Could not retrieve Breed by Id"
        )
    }

    @Test
    fun updateFavoriteSuccess() = runTest {
        val breeds = dbHelper.selectAllItems().first()
        val firstBreed = breeds.first()
        dbHelper.updateFavorite(firstBreed.id, true)
        val newBreed = dbHelper.selectById(firstBreed.id).first().first()
        assertNotNull(
            newBreed,
            "Could not retrieve Breed by Id"
        )
        assertTrue(
            newBreed.isFavorited(),
            "Favorite Did Not Save"
        )
    }

    @Test
    fun deleteAllSuccess() = runTest {
        dbHelper.insertBreed("Poodle")
        dbHelper.insertBreed("Schnauzer")
        assertTrue(dbHelper.selectAllItems().first().isNotEmpty())
        dbHelper.deleteAll()

        assertTrue(
            dbHelper.selectAllItems().first().count() == 0,
            "Delete All did not work"
        )
    }
}
