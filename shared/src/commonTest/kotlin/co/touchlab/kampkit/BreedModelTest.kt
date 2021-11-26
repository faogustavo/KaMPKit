package co.touchlab.kampkit

import app.cash.turbine.test
import co.touchlab.kampkit.db.Breed
import co.touchlab.kampkit.ext.reset
import co.touchlab.kampkit.ext.resetAndClose
import co.touchlab.kampkit.mock.ClockMock
import co.touchlab.kampkit.mock.KtorApiMock
import co.touchlab.kampkit.models.BreedModel
import co.touchlab.kampkit.models.DataState
import co.touchlab.kampkit.models.ItemDataSummary
import co.touchlab.kermit.Logger
import com.russhwolf.settings.MockSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class BreedModelTest : BaseTest() {

    private var model: BreedModel = BreedModel()
    private var kermit = Logger
    private var testDbConnection = testDbConnection()
        .also { it.reset() }
    private var dbHelper = DatabaseHelper(
        testDbConnection,
        kermit,
        Dispatchers.Default
    )
    private val settings = MockSettings()
    private val ktorApi = KtorApiMock()

    // Need to start at non-zero time because the default value for db timestamp is 0
    private val clock = ClockMock(Clock.System.now())

    companion object {
        private val appenzeller = Breed(1, "appenzeller", 0L)
        private val australianNoLike = Breed(2, "australian", 0L)
        private val australianLike = Breed(2, "australian", 1L)
        val dataStateSuccessNoFavorite = DataState(
            data = ItemDataSummary(appenzeller, listOf(appenzeller, australianNoLike))
        )
        private val dataStateSuccessFavorite = DataState(
            data = ItemDataSummary(appenzeller, listOf(appenzeller, australianLike))
        )
    }

    @BeforeTest
    fun setup() {
        appStart(dbHelper, settings, ktorApi, kermit, clock)
    }

    @Test
    fun staleDataCheckTest() = runTest {
        val currentTimeMS = Clock.System.now().toEpochMilliseconds()
        settings.putLong(BreedModel.DB_TIMESTAMP_KEY, currentTimeMS)
        assertTrue(ktorApi.calledCount == 0)

        val expectedError = DataState<ItemDataSummary>(exception = "Unable to download breed list")
        val actualError = model.getBreedsFromNetwork(0L)

        assertEquals(
            expectedError,
            actualError
        )
        assertTrue(ktorApi.calledCount == 0)
    }

    @OptIn(FlowPreview::class)
    @Test
    fun updateFavoriteTest() = runTest {
        ktorApi.prepareResult(ktorApi.successResult())

        flowOf(model.refreshBreedsIfStale(), model.getBreedsFromCache())
            .flattenMerge().test {
                // Loading
                assertEquals(DataState(loading = true), awaitItem())
                // No Favorites
                assertEquals(dataStateSuccessNoFavorite, awaitItem())
                // Add 1 favorite breed
                model.updateBreedFavorite(australianNoLike)
                // For some unkown reason, the android is not updating the flow in the first item from flow
                delay(300)
                // Get the new result with 1 breed favorited
                assertEquals(dataStateSuccessFavorite, expectMostRecentItem())
            }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun fetchBreedsFromNetworkPreserveFavorites() {
        ktorApi.prepareResult(ktorApi.successResult())

        runTest {
            flowOf(model.refreshBreedsIfStale(), model.getBreedsFromCache())
                .flattenMerge().test {
                    // Loading
                    assertEquals(DataState(loading = true), awaitItem())
                    assertEquals(dataStateSuccessNoFavorite, awaitItem())
                    // "Like" the Australian breed
                    model.updateBreedFavorite(australianNoLike)
                    // For some unknown reason, the android is not updating the flow in the first item from flow
                    delay(300)
                    // Get the new result with the Australian breed liked
                    assertEquals(dataStateSuccessFavorite, expectMostRecentItem())
                    cancelAndIgnoreRemainingEvents()
                }
        }

        runTest {
            // Fetch breeds from the network (no breeds liked),
            // but preserved the liked breeds in the database.
            flowOf(model.refreshBreedsIfStale(true), model.getBreedsFromCache())
                .flattenMerge().test {
                    // Loading
                    assertEquals(DataState(loading = true), awaitItem())
                    // For some unknown reason, the android is not updating the flow in the first item from flow
                    delay(300)
                    // Get the new result with the Australian breed liked
                    assertEquals(dataStateSuccessFavorite, expectMostRecentItem())
                    cancelAndConsumeRemainingEvents()
                }
        }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun updateDatabaseTest() = runTest {
        val successResult = ktorApi.successResult()
        ktorApi.prepareResult(successResult)
        flowOf(model.refreshBreedsIfStale(), model.getBreedsFromCache()).flattenMerge()
            .test(timeout = Duration.seconds(30)) {
                assertEquals(DataState(loading = true), awaitItem())
                val oldBreeds = awaitItem()
                val data = oldBreeds.data
                assertTrue(data != null)
                assertEquals(
                    ktorApi.successResult().message.keys.size,
                    data.allItems.size
                )
                cancelAndConsumeRemainingEvents()
            }

        // Advance time by more than an hour to make cached data stale
        clock.currentInstant += Duration.hours(2)
        val resultWithExtraBreed = successResult.copy(message = successResult.message + ("extra" to emptyList()))

        ktorApi.prepareResult(resultWithExtraBreed)
        flowOf(model.refreshBreedsIfStale(), model.getBreedsFromCache()).flattenMerge()
            .test(timeout = Duration.seconds(30)) {
                assertEquals(DataState(loading = true), awaitItem())
                val updated = awaitItem()
                val data = updated.data
                assertTrue(data != null)
                assertEquals(resultWithExtraBreed.message.keys.size, data.allItems.size)
            }
    }

    @Test
    fun notifyErrorOnException() = runTest {
        ktorApi.throwOnCall(RuntimeException())
        assertNotNull(model.getBreedsFromNetwork(0L))
    }

    @AfterTest
    fun breakdown() = runTest {
        testDbConnection.resetAndClose()
        appEnd()
    }
}
