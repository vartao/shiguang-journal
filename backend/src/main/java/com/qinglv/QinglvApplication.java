package com.qinglv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 情侣笔记网站 - 主启动类
 */
@SpringBootApplication
@MapperScan("com.qinglv.mapper")
public class QinglvApplication {

    public static void main(String[] args) {
        SpringApplication.run(QinglvApplication.class, args);
    }
}
