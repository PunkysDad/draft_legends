package com.draftlegends.backend.controller

import com.draftlegends.backend.wallet.DailyAdLimitReachedException
import com.draftlegends.backend.wallet.InsufficientFundsException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DailyAdLimitReachedException::class)
    fun handleDailyAdLimitReached(ex: DailyAdLimitReachedException): ResponseEntity<Map<String, Any?>> {
        logger.error("DailyAdLimitReachedException: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
            mapOf(
                "status" to 429,
                "error" to "DailyAdLimitReachedException",
                "message" to ex.message
            )
        )
    }

    @ExceptionHandler(InsufficientFundsException::class)
    fun handleInsufficientFunds(ex: InsufficientFundsException): ResponseEntity<Map<String, Any?>> {
        logger.error("InsufficientFundsException: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            mapOf(
                "status" to 402,
                "error" to "InsufficientFundsException",
                "message" to ex.message
            )
        )
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, Any?>> {
        logger.error("ResponseStatusException: ${ex.reason}", ex)
        return ResponseEntity.status(ex.statusCode).body(
            mapOf(
                "status" to ex.statusCode.value(),
                "error" to ex.javaClass.simpleName,
                "message" to ex.reason
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, Any?>> {
        logger.error("Unhandled exception: ${ex.message}", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "status" to 500,
                "error" to ex.javaClass.simpleName,
                "message" to ex.message
            )
        )
    }
}
