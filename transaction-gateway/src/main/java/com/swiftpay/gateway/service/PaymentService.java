package com.swiftpay.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftpay.common.events.PaymentInitiatedEvent;
import com.swiftpay.common.model.PaymentStatus;
import com.swiftpay.gateway.cache.BalanceCacheService;
import com.swiftpay.gateway.domain.PaymentEntity;
import com.swiftpay.gateway.domain.PaymentRepository;
import com.swiftpay.gateway.idempotency.IdempotencyService;
import com.swiftpay.gateway.kafka.PaymentEventPublisher;
import com.swiftpay.gateway.money.Money;
import com.swiftpay.gateway.web.dto.PaymentRequestDto;
import com.swiftpay.gateway.web.dto.PaymentResponseDto;
import com.swiftpay.gateway.web.error.BusinessRuleException;
import com.swiftpay.gateway.web.error.IdempotencyInProgressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final BalanceCacheService balanceCacheService;
    private final IdempotencyService idempotencyService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            BalanceCacheService balanceCacheService,
            IdempotencyService idempotencyService,
            PaymentEventPublisher paymentEventPublisher,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.balanceCacheService = balanceCacheService;
        this.idempotencyService = idempotencyService;
        this.paymentEventPublisher = paymentEventPublisher;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    public PaymentResponseDto acceptPayment(PaymentRequestDto request) {
        var transactionId = request.transactionId();

        var cached = idempotencyService.getRaw(transactionId);
        if (cached.isPresent()) {
            var raw = cached.get();
            if (IdempotencyService.IN_PROGRESS_MARKER.equals(raw)) {
                throw new IdempotencyInProgressException(
                        "This transaction_id is already being processed; retry shortly."
                );
            }
            try {
                return objectMapper.readValue(raw, PaymentResponseDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Corrupt idempotency payload for {}; re-processing", transactionId);
            }
        }

        var existing = paymentRepository.findByTransactionId(transactionId);
        if (existing.isPresent()) {
            return recoverExistingPayment(transactionId, existing.get());
        }

        if (!idempotencyService.markInProgress(transactionId)) {
            return handleConcurrentIdempotencyClaim(transactionId);
        }

        try {
            validateBusinessRules(request);

            var amountCents = Money.toCents(request.amount());
            var senderBalance = balanceCacheService.getBalanceCents(request.senderId())
                    .orElseThrow(() -> new BusinessRuleException(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "SENDER_NOT_FOUND",
                            "Sender account does not exist"
                    ));
            if (senderBalance < amountCents) {
                throw new BusinessRuleException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "INSUFFICIENT_FUNDS",
                        "Sender does not have enough available balance"
                );
            }

            var saved = transactionTemplate.execute(status -> {
                var entity = new PaymentEntity();
                entity.setTransactionId(transactionId);
                entity.setSenderId(request.senderId());
                entity.setReceiverId(request.receiverId());
                entity.setAmountCents(amountCents);
                entity.setCurrency(request.currency());
                entity.setStatus(PaymentStatus.PENDING);
                var persisted = paymentRepository.save(entity);
                var event = new PaymentInitiatedEvent(
                        transactionId,
                        request.senderId(),
                        request.receiverId(),
                        amountCents,
                        request.currency()
                );
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        paymentEventPublisher.publishPaymentInitiated(event);
                    }
                });
                return persisted;
            });

            var response = toDto(Objects.requireNonNull(saved, "payment persist"));
            idempotencyService.complete(transactionId, writeJson(response));
            return response;
        } catch (RuntimeException e) {
            idempotencyService.clear(transactionId);
            throw e;
        }
    }

    private PaymentResponseDto recoverExistingPayment(String transactionId, PaymentEntity entity) {
        if (entity.getStatus() == PaymentStatus.PENDING) {
            var event = new PaymentInitiatedEvent(
                    transactionId,
                    entity.getSenderId(),
                    entity.getReceiverId(),
                    entity.getAmountCents(),
                    entity.getCurrency()
            );
            try {
                paymentEventPublisher.publishPaymentInitiated(event);
            } catch (RuntimeException e) {
                idempotencyService.clear(transactionId);
                throw e;
            }
        }
        var response = toDto(entity);
        idempotencyService.complete(transactionId, writeJson(response));
        return response;
    }

    private PaymentResponseDto handleConcurrentIdempotencyClaim(String transactionId) {
        var again = idempotencyService.getRaw(transactionId);
        if (again.isPresent() && !IdempotencyService.IN_PROGRESS_MARKER.equals(again.get())) {
            try {
                return objectMapper.readValue(again.get(), PaymentResponseDto.class);
            } catch (JsonProcessingException ignored) {
            }
        }
        var existing = paymentRepository.findByTransactionId(transactionId);
        if (existing.isPresent()) {
            return recoverExistingPayment(transactionId, existing.get());
        }
        throw new IdempotencyInProgressException(
                "This transaction_id is already being processed; retry shortly."
        );
    }

    private void validateBusinessRules(PaymentRequestDto request) {
        if (request.senderId().equals(request.receiverId())) {
            throw new BusinessRuleException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TRANSFER",
                    "sender_id and receiver_id must differ"
            );
        }
    }

    private PaymentResponseDto toDto(PaymentEntity entity) {
        return new PaymentResponseDto(entity.getTransactionId(), entity.getStatus(), entity.getCreatedAt());
    }

    private String writeJson(PaymentResponseDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize payment response", e);
        }
    }
}
