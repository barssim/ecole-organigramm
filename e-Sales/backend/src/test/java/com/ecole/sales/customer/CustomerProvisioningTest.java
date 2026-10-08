package com.ecole.sales.customer;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.provider.ProductService;
import com.ecole.sales.provider.ProviderClient;
import com.ecole.sales.security.CurrentUser;
import com.ecole.sales.user.SalesUser;
import com.ecole.sales.user.SalesUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;

class CustomerProvisioningTest {
    private final CurrentUser current = mock(CurrentUser.class);
    private final ProductService products = mock(ProductService.class);
    private final ProviderClient provider = mock(ProviderClient.class);
    private final CustomerOwnershipRepository ownership = mock(CustomerOwnershipRepository.class);
    private final SalesUserRepository users = mock(SalesUserRepository.class);
    private final CustomerOwnershipService ownershipService = mock(CustomerOwnershipService.class);
    private final CustomerController controller = new CustomerController(
        current, products, provider, ownership, users, ownershipService);
    private final SalesUser actor = new SalesUser("alex-sales", "Alex Sales", "alex@example.test",
        "hash", "SALES_USER", true);
    private final CustomerController.CreateCustomer request = new CustomerController.CreateCustomer(
        "school", "school-one", "School One", "office@example.test",
        new CustomerController.AdminInfo("director", "Director", "director@example.test"));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsOwnershipOnlyAfterProviderConfirmsProvisioning() throws Exception {
        when(current.get()).thenReturn(actor);
        when(products.productName("school")).thenReturn("School");
        when(provider.request(eq("school"), eq("POST"), eq("/customers"), any()))
            .thenReturn(new ObjectMapper().readTree("""
                {"tenantId":"school-one","name":"School One","invitationLink":"https://school.test/invite","invitationEmailSent":true}
                """));

        ResponseEntity<CustomerController.Created> response = controller.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("school-one", response.getBody().tenantId());
        assertTrue(response.getBody().invitationEmailSent());
        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        InOrder order = inOrder(provider, ownership);
        order.verify(provider).verifyContract("school");
        order.verify(provider).request(eq("school"), eq("POST"), eq("/customers"), body.capture());
        order.verify(ownership).save(any(CustomerOwnership.class));
        assertInstanceOf(Map.class, body.getValue());
        assertEquals("alex-sales", ((Map<?, ?>) body.getValue()).get("ownerRef"));
    }

    @Test
    void doesNotRecordOwnershipWhenProviderProvisioningFails() {
        when(current.get()).thenReturn(actor);
        when(products.productName("school")).thenReturn("School");
        when(provider.request(eq("school"), eq("POST"), eq("/customers"), any()))
            .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "School provider is unreachable"));

        ApiException error = assertThrows(ApiException.class, () -> controller.create(request));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatus());
        verify(ownership, never()).save(any(CustomerOwnership.class));
    }

    @Test
    void rejectsIncompleteProviderResponseWithoutRecordingOwnership() throws Exception {
        when(current.get()).thenReturn(actor);
        when(products.productName("school")).thenReturn("School");
        when(provider.request(eq("school"), eq("POST"), eq("/customers"), any()))
            .thenReturn(new ObjectMapper().readTree("""
                {"tenantId":"school-one","name":"School One","invitationEmailSent":true}
                """));

        ApiException error = assertThrows(ApiException.class, () -> controller.create(request));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
        assertTrue(error.getMessage().contains("Invalid customer creation response"));
        verify(ownership, never()).save(any(CustomerOwnership.class));
    }
}
