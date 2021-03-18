package com.yeafel.priceget;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceGetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PriceGetApplication.class, args);
        System.out.println();
        System.out.println("爬虫程序已经开启,请勿关闭窗口");
    }

}
