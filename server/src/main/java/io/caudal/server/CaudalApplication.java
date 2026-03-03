package io.caudal.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaudalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaudalApplication.class, args);
    }
}
