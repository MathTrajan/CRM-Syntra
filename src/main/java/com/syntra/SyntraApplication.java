package com.syntra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyntraApplication {

    public static void main(String[] args) {
        SpringApplication.run(SyntraApplication.class, args);
    }
}
