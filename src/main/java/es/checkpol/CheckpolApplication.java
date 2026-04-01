package es.checkpol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckpolApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckpolApplication.class, args);
    }
}
