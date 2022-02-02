package com.papsign.ktor.openapigen.content.type.ktor;

import io.ktor.http.ContentType;
import io.ktor.server.plugins.ContentNegotiation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ContentNegotiationHelper {

    @SuppressWarnings("KotlinInternalInJava")
    public static Set<ContentType> contentTypes(ContentNegotiation contentNegotiation) {
        List<ContentNegotiation.ConverterRegistration> registrations = contentNegotiation.getRegistrations$ktor_server_content_negotiation();

        return registrations.stream()
                .map(ContentNegotiation.ConverterRegistration::getContentType)
                .collect(Collectors.toSet());
    }
}
