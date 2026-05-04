package com.swiftpay.gateway.web;

import com.swiftpay.gateway.service.PaymentService;
import com.swiftpay.gateway.web.dto.PaymentRequestDto;
import com.swiftpay.gateway.web.dto.PaymentResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Payments", description = "P2P payment initiation")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Create a payment", description = "Persists PENDING payment and emits PaymentInitiated (idempotent by transaction_id).")
    public PaymentResponseDto createPayment(@Valid @RequestBody PaymentRequestDto request) {
        return paymentService.acceptPayment(request);
    }
}
