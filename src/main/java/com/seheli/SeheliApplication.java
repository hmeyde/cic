package com.seheli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeheliApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeheliApplication.class, args);
    }
}
