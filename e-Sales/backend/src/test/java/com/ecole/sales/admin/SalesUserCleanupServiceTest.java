package com.ecole.sales.admin;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.customer.CustomerOwnershipRepository;
import com.ecole.sales.provider.ProductService;
import com.ecole.sales.provider.ProviderClient;
import com.ecole.sales.user.SalesUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalesUserCleanupServiceTest {
    private final ProductService products = mock(ProductService.class);
    private final ProviderClient provider = mock(ProviderClient.class);
    private final CustomerOwnershipRepository ownership = mock(CustomerOwnershipRepository.class);
    private final SalesUserCleanupService cleanup = new SalesUserCleanupService(products, provider, ownership);
    private final SalesUser user = new SalesUser("alex-sales", "Alex Sales", "alex@example.test",
        "hash", "SALES_USER", true);

    @Test
    void removesOnlyReadyDemosAfterCheckingAllEnabledProducts() {
        configureProducts();
        when(ownership.findAllByProductId(anyString())).thenReturn(List.of());
        when(provider.request(anyString(), eq("GET"), anyString(), isNull())).thenAnswer(invocation -> {
            String path = invocation.getArgument(2);
            return path.equals("/customers") ? JsonNodeFactory.instance.arrayNode()
                : demo(path.equals("/demos/alex-sales") && invocation.getArgument(0).equals("school"));
        });

        cleanup.cleanBeforeRemoval(user);

        verify(provider).request("school", "DELETE", "/demos/alex-sales", null);
        verify(provider, never()).request("cafe", "DELETE", "/demos/alex-sales", null);
    }

    @Test
    void reportsPartialProviderDeletionAndDoesNotHideProgress() {
        configureProducts();
        when(ownership.findAllByProductId(anyString())).thenReturn(List.of());
        when(provider.request(anyString(), eq("GET"), anyString(), isNull())).thenAnswer(invocation ->
            invocation.getArgument(2).equals("/customers") ? JsonNodeFactory.instance.arrayNode() : demo(true));
        when(provider.request(eq("school"), eq("DELETE"), anyString(), isNull()))
            .thenReturn(JsonNodeFactory.instance.objectNode());
        when(provider.request(eq("cafe"), eq("DELETE"), anyString(), isNull()))
            .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Cafe unavailable"));

        ApiException error = assertThrows(ApiException.class, () -> cleanup.cleanBeforeRemoval(user));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        assertTrue(error.getMessage().contains("Sales account was not deleted"));
        assertTrue(error.getMessage().contains("Demos deleted from School"));
        assertTrue(error.getMessage().contains("Cafe unavailable"));
    }

    @Test
    void blocksRemovalWhenAnUnmappedProviderCustomerIsOwnedByTheUser() {
        configureProducts();
        when(ownership.findAllByProductId(anyString())).thenReturn(List.of());
        when(provider.request(eq("school"), eq("GET"), eq("/customers"), isNull())).thenReturn(customerList());

        ApiException error = assertThrows(ApiException.class, () -> cleanup.cleanBeforeRemoval(user));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(provider, never()).request(anyString(), eq("DELETE"), anyString(), any());
    }

    private void configureProducts() {
        when(products.all()).thenReturn(List.of(
            new ProductService.Product("school", "School", true),
            new ProductService.Product("cafe", "Cafe", true)));
        when(products.productName("school")).thenReturn("School");
        when(products.productName("cafe")).thenReturn("Cafe");
    }

    private JsonNode demo(boolean ready) {
        return JsonNodeFactory.instance.objectNode().put("ready", ready);
    }

    private JsonNode customerList() {
        var customer = JsonNodeFactory.instance.objectNode()
            .put("tenantId", "school-one")
            .put("ownerRef", "alex-sales");
        return JsonNodeFactory.instance.arrayNode().add(customer);
    }
}
