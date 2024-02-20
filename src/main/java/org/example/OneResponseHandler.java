package org.example;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@AllArgsConstructor
public class OneResponseHandler implements Handler {

    private final Client client;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public ApplicationStatusResponse performOperation(String id) {
        CompletableFuture<ApplicationStatusResponse> response = new CompletableFuture<>();
        AtomicInteger cnt = new AtomicInteger();

        attemptServiceCall(id, response, cnt, true, true);
        try {
            ApplicationStatusResponse applicationStatusResponse = response.orTimeout(15, TimeUnit.SECONDS).get();
            scheduler.shutdownNow();
            return applicationStatusResponse;
        } catch (Exception e) {
            scheduler.shutdownNow();
            return new ApplicationStatusResponse.Failure(null, Math.max(cnt.get(), 2));
        }
    }

    private void attemptServiceCall(String id, CompletableFuture<ApplicationStatusResponse> responseFuture, AtomicInteger attemptCount, boolean attemptService1, boolean attemptService2) {
        if (attemptService1) {
            CompletableFuture.supplyAsync(() -> client.getApplicationStatus1(id), scheduler)
                    .thenAccept(result -> processServiceResponse(id, responseFuture, attemptCount, result, true));
        }
        if (attemptService2) {
            CompletableFuture.supplyAsync(() -> client.getApplicationStatus2(id), scheduler)
                    .thenAccept(result -> processServiceResponse(id, responseFuture, attemptCount, result, false));
        }
    }

    private void processServiceResponse(String id, CompletableFuture<ApplicationStatusResponse> responseFuture, AtomicInteger attemptCount, Response result, boolean isService1) {
        if (result instanceof Response.Success) {
            Response.Success success = (Response.Success) result;
            responseFuture.complete(new ApplicationStatusResponse.Success(success.applicationId(), success.applicationStatus()));

        } else if (result instanceof Response.RetryAfter) {
            Response.RetryAfter retryAfter = (Response.RetryAfter) result;
            attemptCount.incrementAndGet();
            scheduler.schedule(() -> attemptServiceCall(id, responseFuture, attemptCount, isService1, !isService1), retryAfter.delay().toMillis(), TimeUnit.MILLISECONDS);

        } else if (result instanceof Response.Failure) {
            // better to use exponential backoff instead of each second retry
            scheduler.schedule(() -> attemptServiceCall(id, responseFuture, attemptCount, isService1, !isService1), Duration.ofSeconds(1).toMillis(), TimeUnit.MILLISECONDS);

        }
    }

}