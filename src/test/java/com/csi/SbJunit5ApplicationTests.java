package com.csi;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SbJunit5ApplicationTests {

    private static Dotenv dotenv;

    @BeforeAll
    static void loadDotEnv() {
        // Load .env file from test resources
        dotenv = Dotenv.configure()
                .directory("./src/test/resources")
                .filename(".env.test")
                .load();
    }

    @Test
    void shouldLoadDotEnvFile() {
        assertThat(dotenv).isNotNull();
    }

    @Test
    void shouldContainRequiredEnvironmentVariables() {
        assertThat(dotenv.get("DATABASE_URL")).isNotNull();
        assertThat(dotenv.get("DATABASE_USERNAME")).isNotNull();
        assertThat(dotenv.get("DATABASE_PASSWORD")).isNotNull();
    }

    @Test
    void shouldHaveCorrectVariableValues() {
        String databaseUrl = dotenv.get("DATABASE_URL");
        assertThat(databaseUrl)
                .startsWith("jdbc:")
                .contains("localhost");

        String username = dotenv.get("DATABASE_USERNAME");
        assertThat(username).isNotBlank();
    }

    @Test
    void shouldHandleMissingVariablesGracefully() {
        String nonExistent = dotenv.get("NON_EXISTENT_VAR", "default_value");
        assertThat(nonExistent).isEqualTo("default_value");
    }
}