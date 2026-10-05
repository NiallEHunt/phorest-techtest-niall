package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.service.MachineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

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
}