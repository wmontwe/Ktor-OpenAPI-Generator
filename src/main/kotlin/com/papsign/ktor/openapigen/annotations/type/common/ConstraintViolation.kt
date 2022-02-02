package com.papsign.ktor.openapigen.annotations.type.common

abstract class ConstraintViolation(defaultMessage: String, message: String = "", cause: Throwable? = null)
    : Exception(if (message.isEmpty()) defaultMessage else message, cause)
