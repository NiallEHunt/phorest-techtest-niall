package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.service.MachineService
import dev.niall.fruitmachine.service.persistence.InMemoryMachineRepository
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.matchesPattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertNotNull

@WebMvcTest(MachineController::class)
@Import(MachineService::class, InMemoryMachineRepository::class)
class MachineControllerTest(@Autowired private val mockMvc: MockMvc) {
    private val createRequest = """
        {
          "slotCount": 4,
          "colours": ["black", "white", "green", "yellow"],
          "adjacentSlotsForSmallPrize": 2,
          "playCost": { "minorUnits": 100, "currency": "EUR" },
          "startingFloat": { "minorUnits": 5000, "currency": "EUR" }
        }
    """.trimIndent()

    private fun createMachine() = mockMvc.post("/machines") {
        contentType = MediaType.APPLICATION_JSON
        content = createRequest
    }

    @Test
    fun `creating a machine returns 201 with its location and configuration`() {
        createMachine().andExpect {
            status { isCreated() }
            header { string("Location", matchesPattern("http://localhost/machines/[0-9a-f-]{36}")) }
            jsonPath("$.id") { isNotEmpty() }
            jsonPath("$.slotCount") { value(4) }
            jsonPath("$.colours") { value(contains("black", "white", "green", "yellow")) }
            jsonPath("$.adjacentSlotsForSmallPrize") { value(2) }
            jsonPath("$.playCost.minorUnits") { value(100) }
            jsonPath("$.playCost.currency") { value("EUR") }
            jsonPath("$.float.minorUnits") { value(5000) }
            jsonPath("$.float.currency") { value("EUR") }
        }
    }

    @Test
    fun `a created machine can be read from its location`() {
        val location = assertNotNull(createMachine().andReturn().response.getHeader("Location"))
        val id = location.substringAfterLast("/")

        mockMvc.get(location).andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
            jsonPath("$.slotCount") { value(4) }
            jsonPath("$.float.minorUnits") { value(5000) }
        }
    }

    @Test
    fun `reading an unknown machine returns 404 problem details`() {
        val id = "00000000-0000-0000-0000-000000000000"

        mockMvc.get("/machines/$id").andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(404) }
            jsonPath("$.detail") { value("No machine found with id $id") }
        }
    }

    @Test
    fun `reading a machine with a malformed id returns 400`() {
        mockMvc.get("/machines/not-a-uuid").andExpect {
            status { isBadRequest() }
        }
    }
}
