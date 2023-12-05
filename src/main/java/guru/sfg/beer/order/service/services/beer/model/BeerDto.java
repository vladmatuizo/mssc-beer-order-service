package guru.sfg.beer.order.service.services.beer.model;

import java.math.BigDecimal;
import java.util.UUID;

public record BeerDto(UUID id, String beerName, String beerStyle, BigDecimal price) {
}
