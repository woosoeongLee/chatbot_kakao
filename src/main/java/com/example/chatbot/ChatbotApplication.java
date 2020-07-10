package com.example.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@Configuration : 스프링 IOC container 에게 해당 클래스는 Bean 구성 클래스임을 알려준다.
//@EnableAutoConfiguration
public class ChatbotApplication {
	public static void main(String[] args) {
		SpringApplication.run(ChatbotApplication.class, args);
	}
}
