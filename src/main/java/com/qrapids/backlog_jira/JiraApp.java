package com.qrapids.backlog_jira;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;



@SpringBootApplication
@Configuration
public class JiraApp extends SpringBootServletInitializer {

        @Override
        protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
            return application.sources(JiraApp.class);
        }

        public static void main(String[] args) {
            SpringApplication.run(JiraApp.class, args);
        }

    }

