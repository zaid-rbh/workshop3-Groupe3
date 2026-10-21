package com.devsecops.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of VulnerableShop.
 * WARNING: this application is INTENTIONALLY VULNERABLE. Run it locally only.
 */
@SpringBootApplication
public class DevSecOpsProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevSecOpsProjectApplication.class, args);
    }
}
