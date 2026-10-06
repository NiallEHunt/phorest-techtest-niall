package dev.niall.fruitmachine.service.api

import dev.niall.fruitmachine.service.MachineNotFoundException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class ApiExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handleMachineNotFound(exception: MachineNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.message)

    @ExceptionHandler
    fun handleInvalidMachineConfig(exception: InvalidMachineConfigException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message)

    // Spring's default body for a Bean Validation failure doesn't say which fields failed,
    // so list each one, e.g. {"field": "colours[1]", "message": "must not be blank"}.
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        ex.body.setProperty("errors", ex.bindingResult.fieldErrors
            .sortedBy { it.field }
            .map { mapOf("field" to it.field, "message" to it.defaultMessage) })
        return super.handleMethodArgumentNotValid(ex, headers, status, request)
    }
}
