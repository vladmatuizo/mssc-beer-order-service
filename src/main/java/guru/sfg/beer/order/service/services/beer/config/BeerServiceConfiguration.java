package guru.sfg.beer.order.service.services.beer.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "beer-service")
public record BeerServiceConfiguration(String host) {
}
