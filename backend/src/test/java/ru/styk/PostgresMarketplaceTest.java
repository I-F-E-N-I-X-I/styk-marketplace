package ru.styk;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Runs the same contract against a real PostgreSQL service in CI. */
@EnabledIfEnvironmentVariable(named="TEST_POSTGRES_URL",matches=".+")
class PostgresMarketplaceTest extends MarketplaceTest {
 @DynamicPropertySource static void postgres(DynamicPropertyRegistry registry){
  registry.add("spring.datasource.url",()->System.getenv("TEST_POSTGRES_URL"));
  registry.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_POSTGRES_USER","styk"));
  registry.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_POSTGRES_PASSWORD","styk-test"));
 }
}
