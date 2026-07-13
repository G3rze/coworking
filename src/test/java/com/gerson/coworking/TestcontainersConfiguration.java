package com.gerson.coworking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));
	}

	@Bean
	DataSourceInitializer dataSourceInitializer(DataSource dataSource) {
		return new DataSourceInitializer(dataSource);
	}

	static class DataSourceInitializer {
		DataSourceInitializer(DataSource dataSource) {
			try (Connection conn = dataSource.getConnection();
				 Statement stmt = conn.createStatement()) {
				stmt.execute("CREATE SCHEMA IF NOT EXISTS coworking");
			} catch (Exception e) {
				throw new RuntimeException("Failed to create coworking schema", e);
			}
		}
	}

}
