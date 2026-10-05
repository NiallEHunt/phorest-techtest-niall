package dev.niall.fruitmachine.service

import dev.niall.fruitmachine.core.FruitMachineEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.SecureRandom
import kotlin.random.asKotlinRandom

@Configuration
class EngineConfiguration {
    @Bean
    fun fruitMachineEngine() = FruitMachineEngine(SecureRandom().asKotlinRandom())
}
