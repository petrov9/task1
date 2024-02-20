package org.example;

import java.time.Duration;

public class Main {
    public static void main(String[] args) {
        Client client = new Client() {
            @Override
            public Response getApplicationStatus1(String id) {
                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                double random = Math.random();

                if (random < 0.1) {
                    return new Response.Success("success status 1", id);
                } else if (random < 0.66) {
                    return new Response.RetryAfter(Duration.ofSeconds(3));
                } else {
                    return new Response.Failure(new RuntimeException("some ex"));
                }
            }

            @Override
            public Response getApplicationStatus2(String id) {

                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                double random = Math.random();

                if (random < 0.83) {
                    return new Response.Success("success status 2", id);
                } else if (random < 0.66) {
                    return new Response.RetryAfter(Duration.ofSeconds(3));
                } else {
                    return new Response.Failure(new RuntimeException("some ex"));
                }
            }
        };

        OneResponseHandler oneResponseHandler = new OneResponseHandler(client);
        ApplicationStatusResponse myId = oneResponseHandler.performOperation("my_id");
        System.out.println(myId);
    }
}