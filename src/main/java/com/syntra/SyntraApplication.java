package com.syntra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class SyntraApplication {

    public static void main(String[] args) {
        // Container Fly.io roda em UTC; forcamos BRT para que LocalDateTime.now()
        // e o que e' exibido pelo Thymeleaf usem o fuso de Sao Paulo.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        System.setProperty("user.timezone", "America/Sao_Paulo");
        SpringApplication.run(SyntraApplication.class, args);
    }
}
