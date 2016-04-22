package de.escalon.hypermedia.spring.siren;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.escalon.hypermedia.spring.sample.test.ReviewController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.core.DefaultRelProvider;
import org.springframework.hateoas.core.Relation;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static de.escalon.hypermedia.spring.AffordanceBuilder.linkTo;
import static de.escalon.hypermedia.spring.AffordanceBuilder.methodOn;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Created by Dietrich on 18.04.2016.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(loader = AnnotationConfigWebContextLoader.class)
public class SirenMessageConverterTest {

    public static final Logger LOG = LoggerFactory.getLogger(SirenMessageConverterTest.class);
    private ObjectMapper objectMapper = new ObjectMapper();



    @Relation("customer")
    class Customer {
        private final String customerId = "pj123";
        private final String name = "Peter Joseph";

        public String getCustomerId() {
            return customerId;
        }

        public String getName() {
            return name;
        }
    }

    @RequestMapping("/customers")
    static class DummyCustomersController {

        @RequestMapping("/{customerId}")
        public ResponseEntity<Resource<Customer>> getCustomer(@PathVariable String customerId) {
            return null;
        }
    }



    public static class OrderItem {
        private int orderNumber;
        private String productCode;
        private Integer quantity;

        @JsonCreator
        public OrderItem(@JsonProperty("orderNumber") int orderNumber,
                         @JsonProperty("productCode") String productCode,
                         @JsonProperty("quantity") Integer quantity) {
            this.orderNumber = orderNumber;
            this.productCode = productCode;
            this.quantity = quantity;
        }

        public int getOrderNumber() {
            return orderNumber;
        }

        public String getProductCode() {
            return productCode;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }

    class Order extends ResourceSupport {
        private final int orderNumber = 42;
        private final int itemCount = 3;
        private final String status = "pending";

        private final Resource<Customer> customer =
                new Resource<Customer>(new Customer());

        public Order() {
            customer.add(linkTo(methodOn(DummyCustomersController.class)
                    .getCustomer("pj123"))
                    .withSelfRel());
        }


        public int getOrderNumber() {
            return orderNumber;
        }

        public int getItemCount() {
            return itemCount;
        }

        public String getStatus() {
            return status;
        }

        public Resource<Customer> getCustomer() {
            return customer;
        }

    }


    @RequestMapping("/orders")
    static class DummyOrderController {

        @RequestMapping("/{orderNumber}")
        public ResponseEntity<Resource<Order>> getOrder(@PathVariable int orderNumber) {
            return null;
        }

        @RequestMapping("/{orderNumber}/items")
        public ResponseEntity<Resource<OrderItem>> getOrderItems(@PathVariable int orderNumber) {
            return null;
        }

        @RequestMapping(value = "/{orderNumber}/items", method = RequestMethod.POST)
        public ResponseEntity<Void> addOrderItems(@PathVariable int orderNumber, @RequestBody OrderItem orderItem) {
            return null;
        }

    }

    @Configuration
    @EnableWebMvc
    static class WebConfig extends WebMvcConfigurerAdapter {


        @Bean
        public DummyOrderController orderController() {
            return new DummyOrderController();
        }

        @Bean
        public DummyCustomersController customersController() {
            return new DummyCustomersController();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            super.configureMessageConverters(converters);
            converters.add(new SirenMessageConverter(new DefaultRelProvider()));
        }

        @Override
        public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
            final ExceptionHandlerExceptionResolver resolver = new ExceptionHandlerExceptionResolver();
            resolver.setWarnLogCategory(resolver.getClass()
                    .getName());
            exceptionResolvers.add(resolver);
        }

    }

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(this.wac).build();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testActions() throws JsonProcessingException {


        Order order = new Order();
        order.add(linkTo(methodOn(DummyOrderController.class)
                .addOrderItems(42, new OrderItem(42, null, null)))
                .withRel("order-items"));
        order.add(linkTo(methodOn(DummyOrderController.class)
                .getOrder(42))
                .withSelfRel());
        order.add(linkTo(methodOn(DummyOrderController.class)
                .getOrder(43))
                .withRel("next"));
        order.add(linkTo(methodOn(DummyOrderController.class)
                .getOrder(41))
                .withRel("previous"));

        SirenEntity entity = new SirenEntity();
        SirenUtils.toSirenEntity(entity, order, new DefaultRelProvider());

        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(entity));
    }

}
