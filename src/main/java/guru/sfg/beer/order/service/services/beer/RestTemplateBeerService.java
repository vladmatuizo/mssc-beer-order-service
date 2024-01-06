package guru.sfg.beer.order.service.services.beer;

import guru.sfg.beer.order.service.services.beer.config.BeerServiceConfiguration;
import com.example.common.model.BeerDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
public class RestTemplateBeerService implements BeerService {

    private static final String BEER_BY_ID_URI = "/api/v1/beer/";
    private static final String BEER_BY_UPC_URI = "/api/v1/beerUpc/";

    private final BeerServiceConfiguration beerServiceConfiguration;

    private final RestTemplate restTemplate;

    public RestTemplateBeerService(BeerServiceConfiguration beerServiceConfiguration,
                                   RestTemplateBuilder restTemplateBuilder) {
        this.beerServiceConfiguration = beerServiceConfiguration;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public Optional<BeerDto> getBeerById(UUID beerId) {
        return Optional.ofNullable(restTemplate.getForObject(
                beerServiceConfiguration.host()  + BEER_BY_ID_URI + beerId.toString(), BeerDto.class));
    }

    @Override
    public Optional<BeerDto> getBeerByUpc(String upc) {
        return Optional.ofNullable(restTemplate.getForObject(
                beerServiceConfiguration.host()  + BEER_BY_UPC_URI + upc, BeerDto.class));
    }
}
