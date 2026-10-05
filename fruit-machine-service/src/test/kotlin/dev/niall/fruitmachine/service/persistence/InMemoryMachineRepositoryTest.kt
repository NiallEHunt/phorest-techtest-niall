package dev.niall.fruitmachine.service.persistence

class InMemoryMachineRepositoryTest : MachineRepositoryContractTest() {
    override fun createRepository() = InMemoryMachineRepository()
}
