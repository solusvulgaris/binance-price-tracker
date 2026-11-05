package com.ak.binancepricetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BinancepricetrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BinancepricetrackerApplication.class, args);
	}

}
