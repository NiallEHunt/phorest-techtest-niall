package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.service.MachineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/machines")
class MachineController(private val machineService: MachineService) {
    @PostMapping
    fun create(@RequestBody request: CreateMachineRequest): ResponseEntity<MachineResponse> {
        val machine = request.toFruitMachine()
        val id = machineService.create(machine)
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri()
        
        return ResponseEntity.created(location).body(MachineResponse.from(id, machine))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): MachineResponse =
        MachineResponse.from(id, machineService.get(id))
}
