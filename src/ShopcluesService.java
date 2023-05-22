package org.easyops.service;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.easyops.bean.ExcludeFromJacocoGeneratedReport;
import org.easyops.bean.OrderStatusEnum;
import org.easyops.bean.SystemIdentity;
import org.easyops.exception.EntityException;
import org.easyops.exception.ServiceUnavailableException;
import org.easyops.jpa.domain.SalesChannel;
import org.easyops.jpa.domain.shopclues.ShopcluesConfig;
import org.easyops.jpa.repository.shopclues.ShopcluesConfigRepository;
import org.easyops.shopclues.ShopcluesPortalConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShopcluesService {
    @Autowired
    private ShopcluesConfigRepository shopcluesConfigRepository;
    @Autowired
    private CacheService cacheService;

    public ShopcluesConfig getConfig(SalesChannel channel) {
        Optional<ShopcluesConfig> configOptional = shopcluesConfigRepository.findBySalesChannel(channel);
        if (configOptional.isPresent()) {
            return configOptional.get();
        } else {
            throw new EntityException("Failed to locate channel config");
        }
    }

    @CircuitBreaker(name = "shopcluesApi", fallbackMethod = "connectFallback")
    @RateLimiter(name = "shopcluesApi")
    @Bulkhead(name = "shopcluesApi")
    @Retry(name = "shopcluesApi")
    public ShopcluesPortalConnection connect(ShopcluesConfig config) {
        ShopcluesPortalConnection connection = getConnection();

        String key = SystemIdentity.CHANNEL_SHOPCLUES.getId() + "." + config.getId() + ".api.token";
        List<String> cookies = cacheService.retrieveList(key);
        if (cookies != null && !cookies.isEmpty()) {
            connection.setPortalCookies(cookies);
            connection.setLoginSuccessful(true);
        } else {
            boolean isLoggedIn = connection.login(config);
            if (isLoggedIn) {
                cacheService.cacheList(key, connection.getPortalCookies(), 30, TimeUnit.MINUTES);
            }
            log.info("Shopclues portal connector status :" + isLoggedIn);
        }
        return connection;
    }

    public OrderStatusEnum validateOrder(ShopcluesPortalConnection connector, String orderTrackingNumber) throws IOException {
        OrderStatusEnum activeStatus = OrderStatusEnum.PENDING;
        Map<String, Object> data = checkOrderStatus(connector, orderTrackingNumber);
        if (ObjectUtils.isEmpty(data)) {
            data = checkCancelledOrderStatus(connector, orderTrackingNumber);
        }
        if (ObjectUtils.isNotEmpty(data)) {
            List<String> statusCode = Arrays.asList("41", "73", "118", "23", "24", "22", "38", "19", "51", "84", "40", "12",
                    "Z", "14", "11", "75", "44", "10", "37", "36", "I");
            if (!statusCode.contains(data.get("status").toString())) {
                activeStatus = OrderStatusEnum.CANCELLED;
            }
        } else {
            activeStatus = OrderStatusEnum.NOT_FOUND;
        }

        return activeStatus;
    }

    @CircuitBreaker(name = "shopcluesApi", fallbackMethod = "checkOrderStatusFallback")
    @RateLimiter(name = "shopcluesApi")
    @Bulkhead(name = "shopcluesApi")
    @Retry(name = "shopcluesApi")
    public Map<String, Object> checkOrderStatus(ShopcluesPortalConnection connector, String orderTrackingNumber) throws IOException {
        return connector.getOrderStatus(orderTrackingNumber);
    }

    @CircuitBreaker(name = "shopcluesApi", fallbackMethod = "checkOrderStatusFallback")
    @RateLimiter(name = "shopcluesApi")
    @Bulkhead(name = "shopcluesApi")
    @Retry(name = "shopcluesApi")
    public Map<String, Object> checkCancelledOrderStatus(ShopcluesPortalConnection connector, String orderTrackingNumber) throws IOException {
        return connector.getCancelledOrder(orderTrackingNumber);
    }

    @ExcludeFromJacocoGeneratedReport
    protected ShopcluesPortalConnection getConnection() {
        return new ShopcluesPortalConnection();
    }

    //Handled the exception when the CircuitBreaker is open
    @ExcludeFromJacocoGeneratedReport
    private ShopcluesPortalConnection connectFallback(ShopcluesConfig config, Exception e) {
        log.error("Connect fallback called for exception e:" + e.getClass() + " message: " + e.getLocalizedMessage());
        if (e instanceof CallNotPermittedException) {
            throw new ServiceUnavailableException("Shopclues portal API service is not available");
        } else if (e instanceof BulkheadFullException) {
            throw new ServiceUnavailableException("Shopclues portal API limit has been reached. Please retry after sometime.");
        } else {
            return getConnection();
        }
    }

    //Handled the exception when the CircuitBreaker is open
    @ExcludeFromJacocoGeneratedReport
    private OrderStatusEnum checkOrderStatusFallback(ShopcluesPortalConnection connector, String orderTrackingNumber, Exception e) {
        log.error("Validate order fallback called for exception e:" + e.getClass() + " message: " + e.getLocalizedMessage());
        if (e instanceof CallNotPermittedException) {
            throw new ServiceUnavailableException("Shopclues portal API service is not available");
        } else if (e instanceof BulkheadFullException) {
            throw new ServiceUnavailableException("Shopclues portal API limit has been reached. Please retry after sometime.");
        } else {
            throw new ServiceUnavailableException("Shopclues portal API service error");
        }
    }

}
