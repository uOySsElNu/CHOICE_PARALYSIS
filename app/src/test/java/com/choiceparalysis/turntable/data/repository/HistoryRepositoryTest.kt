package com.choiceparalysis.turntable.data.repository

import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HistoryRepositoryTest {

    private lateinit var dao: HistoryDao
    private lateinit var repository: HistoryRepository

    private val sampleEntity = HistoryEntity(
        id = 1,
        legacyId = "legacy-1",
        method = "SPIN_WHEEL",
        result = "Pizza",
        optionsSnapshot = "[\"Pizza\",\"Sushi\",\"Burger\"]",
        listName = null,
        timestamp = 1700000000000L
    )

    private val sampleEntry = HistoryEntry(
        id = "legacy-1",
        method = DecisionMethod.SPIN_WHEEL,
        result = "Pizza",
        options = listOf("Pizza", "Sushi", "Burger"),
        timestamp = 1700000000000L
    )

    @Before
    fun setup() {
        dao = mockk()
        every { dao.getRecent() } returns flowOf(listOf(sampleEntity))
        repository = HistoryRepository(dao)
    }

    @Test
    fun `history maps entities to entries correctly`() = runTest {
        val history = repository.history.first()
        assertEquals(1, history.size)
        assertEquals("Pizza", history[0].result)
        assertEquals(DecisionMethod.SPIN_WHEEL, history[0].method)
        assertEquals(listOf("Pizza", "Sushi", "Burger"), history[0].options)
    }

    @Test
    fun `addEntry inserts and trims when count exceeds 100`() = runTest {
        coEvery { dao.insert(any()) } just Runs
        coEvery { dao.count() } returns 101
        coEvery { dao.trimOld() } just Runs

        repository.addEntry(sampleEntry)

        coVerify { dao.insert(any()) }
        coVerify { dao.trimOld() }
    }

    @Test
    fun `addEntry inserts without trimming when count is 100`() = runTest {
        coEvery { dao.insert(any()) } just Runs
        coEvery { dao.count() } returns 100

        repository.addEntry(sampleEntry)

        coVerify { dao.insert(any()) }
        coVerify(exactly = 0) { dao.trimOld() }
    }

    @Test
    fun `clearHistory calls deleteAll`() = runTest {
        coEvery { dao.deleteAll() } just Runs
        repository.clearHistory()
        coVerify { dao.deleteAll() }
    }

    @Test
    fun `deleteEntry with numeric id calls deleteById`() = runTest {
        coEvery { dao.deleteById(123L) } just Runs
        repository.deleteEntry("123")
        coVerify { dao.deleteById(123L) }
    }

    @Test
    fun `deleteEntry with non-numeric id calls deleteByLegacyId`() = runTest {
        coEvery { dao.deleteByLegacyId("abc-def") } just Runs
        repository.deleteEntry("abc-def")
        coVerify { dao.deleteByLegacyId("abc-def") }
    }

    @Test
    fun `entity with unknown method defaults to SPIN_WHEEL`() = runTest {
        val entityWithBadMethod = sampleEntity.copy(method = "UNKNOWN_METHOD")
        every { dao.getRecent() } returns flowOf(listOf(entityWithBadMethod))
        val repo = HistoryRepository(dao)

        val history = repo.history.first()
        assertEquals(DecisionMethod.SPIN_WHEEL, history[0].method)
    }

    @Test
    fun `entity with malformed options json returns empty list`() = runTest {
        val entityWithBadJson = sampleEntity.copy(optionsSnapshot = "not-json")
        every { dao.getRecent() } returns flowOf(listOf(entityWithBadJson))
        val repo = HistoryRepository(dao)

        val history = repo.history.first()
        assertEquals(emptyList<String>(), history[0].options)
    }
}
