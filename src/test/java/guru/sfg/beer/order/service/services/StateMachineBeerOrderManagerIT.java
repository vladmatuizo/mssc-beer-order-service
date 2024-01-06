package guru.sfg.beer.order.service.services;

import com.example.common.model.BeerPagedList;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import com.example.common.model.BeerDto;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
public class StateMachineBeerOrderManagerIT {
    @MockBean
    TastingRoomService tastingRoomService;

    @Autowired
    BeerOrderManager beerOrderManager;
    @Autowired
    BeerOrderRepository beerOrderRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    WireMockServer wireMockServer;

    Customer testCustomer;
    UUID beerId = UUID.randomUUID();

    @TestConfiguration
    static class RestTemplateBuilderProvider {

        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = new WireMockServer(wireMockConfig().port(8080));
            server.start();
            return server;
        }
    }

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException {
        BeerDto beerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();

        wireMockServer.stubFor(get("/api/v1/beerUpc/12345")
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED, updatedBeerOrder.getOrderStatus());
        });
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        HashSet<BeerOrderLine> beerOrderLines = new HashSet<>();
        beerOrderLines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .orderQuantity(1)
                .upc("12345")
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(beerOrderLines);
        return beerOrder;
    }
}
