package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.core.FruitMachineEngine
import dev.niall.fruitmachine.service.MachineService
import dev.niall.fruitmachine.service.sequenceRandom
import dev.niall.fruitmachine.service.persistence.InMemoryMachineRepository
import org.hamcrest.Matcher
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesPattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertNotNull

@WebMvcTest(MachineController::class)
@Import(MachineService::class, InMemoryMachineRepository::class, MachineControllerTest.JackpotEngine::class)
class MachineControllerTest(@Autowired private val mockMvc: MockMvc) {
    // Every spin lands on the first colour, so every play is a jackpot.
    @TestConfiguration
    class JackpotEngine {
        @Bean
        fun fruitMachineEngine() = FruitMachineEngine(sequenceRandom(0))
    }

    private val createRequest = """
        {
          "slotCount": 4,
          "colours": ["black", "white", "green", "yellow"],
          "adjacentSlotsForSmallPrize": 2,
          "playCost": { "minorUnits": 100, "currency": "EUR" },
          "startingFloat": { "minorUnits": 5000, "currency": "EUR" }
        }
    """.trimIndent()

    private fun createMachine(request: String = createRequest) = mockMvc.post("/machines") {
        contentType = MediaType.APPLICATION_JSON
        content = request
    }

    private fun createMachineExpectingBadRequest(request: String, expectedDetail: Matcher<String>) =
        createMachine(request).andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(400) }
            jsonPath("$.detail") { value(expectedDetail) }
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
    fun `reading a machine with a malformed id returns 400 problem details`() {
        mockMvc.get("/machines/not-a-uuid").andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `creating a machine the core rejects returns 400 with the reason`() {
        createMachineExpectingBadRequest(
            createRequest.replace("\"slotCount\": 4", "\"slotCount\": 1"),
            equalTo("A slot count of 2 or less will result in a machine where every spin wins a prize")
        )
    }

    @Test
    fun `creating a machine with an unknown currency returns 400`() {
        createMachineExpectingBadRequest(
            createRequest.replace("\"EUR\"", "\"XYZ\""),
            containsString("XYZ")
        )
    }

    @Test
    fun `creating a machine with different currencies for its float and play cost returns 400`() {
        createMachineExpectingBadRequest(
            createRequest.replace("{ \"minorUnits\": 5000, \"currency\": \"EUR\" }", "{ \"minorUnits\": 5000, \"currency\": \"USD\" }"),
            equalTo("A machine must use the same currency for its float and for its play cost")
        )
    }

    @Test
    fun `creating a machine beyond the service limits returns 400 listing the invalid fields`() {
        val request = createRequest
            .replace("\"slotCount\": 4", "\"slotCount\": ${MAX_SLOT_COUNT + 1}")
            .replace("\"minorUnits\": 100,", "\"minorUnits\": ${MAX_AMOUNT_MINOR_UNITS + 1},")

        createMachine(request).andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(400) }
            jsonPath("$.errors[*].field") { value(contains("playCost.minorUnits", "slotCount")) }
            jsonPath("$.errors[*].message") { value(contains("must be less than or equal to $MAX_AMOUNT_MINOR_UNITS", "must be less than or equal to $MAX_SLOT_COUNT")) }
        }
    }

    @Test
    fun `creating a machine with malformed JSON returns 400 problem details`() {
        createMachine("{ not json").andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `creating a machine with a missing field returns 400 problem details`() {
        createMachine(createRequest.replace("\"slotCount\": 4,", "")).andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `playing a machine returns the outcome and the new float`() {
        val location = assertNotNull(createMachine().andReturn().response.getHeader("Location"))

        mockMvc.post("$location/play").andExpect {
            status { isOk() }
            jsonPath("$.slots") { value(contains("black", "black", "black", "black")) }
            jsonPath("$.outcome") { value("JACKPOT") }
            jsonPath("$.payout.minorUnits") { value(5100) }
            jsonPath("$.payout.currency") { value("EUR") }
            jsonPath("$.freePlaysCredited") { value(0) }
            jsonPath("$.float.minorUnits") { value(0) }
            jsonPath("$.float.currency") { value("EUR") }
        }
    }

    @Test
    fun `a play's new float is stored`() {
        val location = assertNotNull(createMachine().andReturn().response.getHeader("Location"))

        mockMvc.post("$location/play")

        mockMvc.get(location).andExpect {
            jsonPath("$.float.minorUnits") { value(0) }
        }
    }

    @Test
    fun `playing an unknown machine returns 404`() {
        mockMvc.post("/machines/00000000-0000-0000-0000-000000000000/play").andExpect {
            status { isNotFound() }
        }
    }
}
