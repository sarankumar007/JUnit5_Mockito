package org.easyops.service;
import org.easyops.bean.OrderStatusEnum;
import org.easyops.exception.EntityException;
import org.easyops.shopclues.ShopcluesPortalConnection;
import org.easyops.jpa.domain.SalesChannel;
import org.easyops.jpa.domain.shopclues.ShopcluesConfig;
import org.easyops.jpa.repository.shopclues.ShopcluesConfigRepository;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = ShopcluesService.class)
public class ShopcluesServiceTest {
    @Autowired
    private ShopcluesService shopcluesService;
    @MockBean
    private CacheService cacheService;
    @MockBean
    private ShopcluesPortalConnection connector;
    @MockBean
    private ShopcluesConfigRepository shopcluesConfigRepository;
    @Captor
    ArgumentCaptor<ShopcluesConfig> configCaptor;
    @Captor
    ArgumentCaptor<String> keyCaptor;
    @Captor
    ArgumentCaptor <List<String>> jsonCaptor;
    @Captor
    ArgumentCaptor<Integer> timeCaptor;
    @Captor
    ArgumentCaptor<TimeUnit> timeUnitCaptor;

    @Test
    void givenSalesChannelAndConfigOptionalPresent_whenGetConfig_thanAssertSuccess() {
        SalesChannel salesChannel = new SalesChannel();
        ShopcluesConfig shopcluesConfig = new ShopcluesConfig();
        doReturn(Optional.of(shopcluesConfig)).when(shopcluesConfigRepository).findBySalesChannel(salesChannel);
        ShopcluesConfig config = shopcluesService.getConfig(salesChannel);

        assertSame(shopcluesConfig, config);
    }

    @Test
    void givenSalesChannelAndConfigOptionalNotPresent_whenGetConfig_thanAssertFailure() {
        SalesChannel salesChannel = new SalesChannel();
        Exception exception = assertThrows(EntityException.class, () -> {
            shopcluesService.getConfig(salesChannel);
        });

        assertNotNull(exception, "Entity Exception was not thrown");
        assertTrue(exception.getMessage().contains("Failed to locate channel config"));
    }

    @Test
    void givenNoCookiesAndLoggedIn_whenConnect_thanAssertSuccess() throws IOException {
        ShopcluesService service = Mockito.spy(shopcluesService);
        doReturn(connector).when(service).getConnection();
        ShopcluesConfig config = new ShopcluesConfig();
        doReturn(true).when(connector).login(config);
        service.connect(config);

        verify(connector, times(1)).login(configCaptor.capture());
        verify(cacheService).cacheList(keyCaptor.capture(), jsonCaptor.capture(), timeCaptor.capture(), timeUnitCaptor.capture());
        assertEquals(Arrays.asList(),jsonCaptor.getValue());
        assertSame(config, configCaptor.getValue());
    }

    @Test
    void givenCookies_whenConnect_thanAssertSuccess() throws IOException {
        ShopcluesService service = Mockito.spy(shopcluesService);
        doReturn(connector).when(service).getConnection();
        ShopcluesConfig config = new ShopcluesConfig();
        doReturn(Arrays.asList("1","2")).when(cacheService).retrieveList(anyString());
        service.connect(config);

        verify(connector).setPortalCookies(jsonCaptor.capture());
        verify(connector).setLoginSuccessful(true);
        assertEquals(Arrays.asList("1","2"),jsonCaptor.getValue());
    }

    @Test
    void givenNoCookiesAndNotLoggedIn_whenConnect_thanAssertSuccess() throws IOException {
        ShopcluesService service = Mockito.spy(shopcluesService);
        doReturn(connector).when(service).getConnection();
        ShopcluesConfig config = new ShopcluesConfig();
        doReturn(false).when(connector).login(config);
        service.connect(config);

        verify(connector, times(1)).login(configCaptor.capture());
        assertSame(config, configCaptor.getValue());
    }

    @Test
    void givenCookiesAndNull_whenConnect_thanAssertSuccess() throws IOException {
        ShopcluesService service = Mockito.spy(shopcluesService);
        doReturn(connector).when(service).getConnection();
        ShopcluesConfig config = new ShopcluesConfig();
        doReturn(null).when(cacheService).retrieveList(anyString());
        service.connect(config);

        verify(connector, times(1)).login(configCaptor.capture());
        assertSame(config, configCaptor.getValue());
    }

    @Test
    void dataFoundAndStatusNotCancelled_whenValidateOrder_thanAssertSuccess() throws IOException {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("status", "41");
        when(connector.getOrderStatus("123")).thenReturn(orderData);
        ShopcluesService service = spy(new ShopcluesService());
        OrderStatusEnum expectedStatus = OrderStatusEnum.PENDING;
        OrderStatusEnum actualStatus = service.validateOrder(connector, "123");

        assertEquals(expectedStatus,actualStatus);
    }

    @Test
    void dataNotFound_whenValidateOrder_thanAssertSuccess() throws IOException {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("status", "41");
        when(connector.getOrderStatus("123")).thenReturn(orderData);
        ShopcluesService service = spy(new ShopcluesService());
        when(connector.getOrderStatus("123")).thenReturn(null);
        OrderStatusEnum expectedStatus = OrderStatusEnum.NOT_FOUND;
        OrderStatusEnum actualStatus = service.validateOrder(connector, "123");

        assertEquals(expectedStatus,actualStatus);
    }

    @Test
    void dataFoundAndStatusCancelled_whenValidateOrder_thanAssertSuccess() throws IOException {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("status", "41");
        when(connector.getOrderStatus("123")).thenReturn(orderData);
        ShopcluesService service = spy(new ShopcluesService());
        orderData.put("status", "123");
        OrderStatusEnum expectedStatus = OrderStatusEnum.CANCELLED;
        OrderStatusEnum actualStatus = service.validateOrder(connector, "123");

        assertEquals(expectedStatus,actualStatus);
    }

    @Test
    void checkOrderStatusTest() throws IOException {
        Map<String, Object> expectedResult = new HashMap<String, Object>();
        expectedResult.put("status", "delivered");
        when(connector.getOrderStatus("12345")).thenReturn(expectedResult);
        Map<String, Object> actualResult = shopcluesService.checkOrderStatus(connector, "12345");

        verify(connector).getOrderStatus(keyCaptor.capture());
        assertEquals("12345",keyCaptor.getValue());
        assertEquals(expectedResult, actualResult);
    }

    @Test
    void checkCancelledOrderStatusTest() throws IOException {
        Map<String, Object> expectedResult = new HashMap<String, Object>();
        expectedResult.put("status", "cancelled");
        when(connector.getCancelledOrder("12345")).thenReturn(expectedResult);
        Map<String, Object> actualResult = shopcluesService.checkCancelledOrderStatus(connector, "12345");

        verify(connector).getCancelledOrder(keyCaptor.capture());
        assertEquals("12345",keyCaptor.getValue());
        assertEquals(expectedResult, actualResult);
    }

}
