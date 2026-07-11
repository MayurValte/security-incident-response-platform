package com.sirp.gateway.exception;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * @RestControllerAdvice does not apply under WebFlux. This is the
 * Gateway's equivalent, returning the same {status, message, path} JSON
 * shape used by the Auth Service's GlobalExceptionHandler.
 */
@Component
@Order(-2)
public class GatewayExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GatewayExceptionHandler(ErrorAttributes errorAttributes,
                                    WebProperties webProperties,
                                    ApplicationContext applicationContext,
                                    ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        var errorProperties = getErrorAttributes(request,
            ErrorAttributeOptions.of(ErrorAttributeOptions.Include.MESSAGE));

        HttpStatus status = HttpStatus.valueOf((int) errorProperties.getOrDefault("status", 500));

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(errorProperties), java.util.Map.class);
    }
}
