package org.nep.nepsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NepsystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NepsystemApplication.class, args);
    }

}