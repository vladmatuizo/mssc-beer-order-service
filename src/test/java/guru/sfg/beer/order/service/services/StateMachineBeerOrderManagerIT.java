package guru.sfg.beer.order.service.services;

import com.example.common.model.BeerDto;
import com.example.common.model.event.AllocationFailureEvent;
import com.example.common.model.event.DeallocateOrderRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import java.util.HashSet;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static guru.sfg.beer.order.service.config.MessagingConfig.ALLOCATION_FAILURE_QUEUE_NAME;
import static guru.sfg.beer.order.service.config.MessagingConfig.DEALLOCATE_ORDER_QUEUE_NAME;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
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
    @Autowired
    JmsTemplate jmsTemplate;

    Customer testCustomer;
    BeerDto testBeerDto;
    UUID beerId;

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
    void setUp() throws JsonProcessingException {
        beerId = UUID.randomUUID();

        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
        testBeerDto = BeerDto.builder()
                .id(beerId)
                .upc("12345")
                .build();
        wireMockServer.stubFor(get("/api/v1/beerUpc/12345")
                .willReturn(okJson(objectMapper.writeValueAsString(testBeerDto))));
    }

    @Test
    void testNewToAllocated() {
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED, updatedBeerOrder.getOrderStatus());
        });
    }

    @Test
    void testValidationFailed() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-validation");

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.VALIDATION_ERROR, updatedBeerOrder.getOrderStatus());
        });
    }

    @Test
    void testAllocationFailed() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("fail-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATION_ERROR, updatedBeerOrder.getOrderStatus());
        });

        AllocationFailureEvent allocationFailureEvent = (AllocationFailureEvent) jmsTemplate.receiveAndConvert(ALLOCATION_FAILURE_QUEUE_NAME);

        assertNotNull(allocationFailureEvent);
        assertEquals(allocationFailureEvent.orderId(), savedBeerOrder.getId());
    }
    @Test
    void testPartialAllocation() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("partial-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PENDING_INVENTORY, updatedBeerOrder.getOrderStatus());

            updatedBeerOrder.getBeerOrderLines().forEach(beerOrderLine -> {
                assertEquals(beerOrderLine.getOrderQuantity(), 1);
                assertEquals(beerOrderLine.getQuantityAllocated(), 0);
            });
        });
    }

    @Test
    void testNewToPickedUp() {
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED, updatedBeerOrder.getOrderStatus());
        });
        beerOrderManager.processBeerOrderPickUp(savedBeerOrder.getId());

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.PICKED_UP, updatedBeerOrder.getOrderStatus());
        });

    }

    @Test
    void testValidationPendingToCancelled() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("cancel-order-before-validation");

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        beerOrderManager.processBeerOrderCancel(savedBeerOrder.getId());

        BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.CANCELLED, updatedBeerOrder.getOrderStatus());
    }
    @Test
    void testAllocationPendingToCancelled() {
        BeerOrder beerOrder = createBeerOrder();
        beerOrder.setCustomerRef("cancel-order-before-allocation");

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            assertEquals(BeerOrderStatusEnum.ALLOCATION_PENDING, updatedBeerOrder.getOrderStatus());
        });

        beerOrderManager.processBeerOrderCancel(savedBeerOrder.getId());

        BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertEquals(BeerOrderStatusEnum.CANCELLED, updatedBeerOrder.getOrderStatus());
    }

    @Test
    void testAllocatedToCancelled() {
        BeerOrder beerOrder = createBeerOrder();

        BeerOrder savedBeerOrder = beerOrderManager.create(beerOrder);
        assertNotNull(savedBeerOrder);

        await().untilAsserted(() -> {
            BeerOrder updatedBeerOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();

            assertEquals(BeerOrderStatusEnum.ALLOCATED, updatedBeerOrder.getOrderStatus());

        });
        beerOrderManager.processBeerOrderCancel(savedBeerOrder.getId());

        DeallocateOrderRequest deallocateOrderRequest = (DeallocateOrderRequest) jmsTemplate.receiveAndConvert(DEALLOCATE_ORDER_QUEUE_NAME);

        assertNotNull(deallocateOrderRequest);
        assertEquals(deallocateOrderRequest.beerOrder().getId(), savedBeerOrder.getId());
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
