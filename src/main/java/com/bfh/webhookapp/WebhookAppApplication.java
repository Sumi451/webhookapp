package com.bfh.webhookapp;
import com.bfh.webhookapp.service.WebhookService;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.bfh.webhookapp.service.WebhookService;

@SpringBootApplication
public class WebhookAppApplication implements CommandLineRunner {

    @Autowired
    private WebhookService webhookService;

    public static void main(String[] args) {
        SpringApplication.run(WebhookAppApplication.class, args);
    }

    @Override
    public void run(String... args) {
        webhookService.process();
    }
}
