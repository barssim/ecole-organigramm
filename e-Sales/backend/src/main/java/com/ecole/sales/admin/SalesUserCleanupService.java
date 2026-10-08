package com.ecole.sales.admin;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.customer.CustomerOwnership;
import com.ecole.sales.customer.CustomerOwnershipRepository;
import com.ecole.sales.provider.ProductService;
import com.ecole.sales.provider.ProviderClient;
import com.ecole.sales.user.SalesUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesUserCleanupService {
    private final ProductService products;
    private final ProviderClient provider;
    private final CustomerOwnershipRepository ownership;

    public SalesUserCleanupService(ProductService products, ProviderClient provider,
                                   CustomerOwnershipRepository ownership) {
        this.products = products;
        this.provider = provider;
        this.ownership = ownership;
    }

    public void cleanBeforeRemoval(SalesUser user) {
        List<String> enabledProducts = products.all().stream()
            .filter(ProductService.Product::enabled)
            .map(ProductService.Product::id)
            .toList();
        Map<String, List<CustomerOwnership>> localByProduct = new HashMap<>();
        for (String productId : enabledProducts) {
            List<CustomerOwnership> rows = ownership.findAllByProductId(productId);
            localByProduct.put(productId, rows);
            if (rows.stream().anyMatch(row -> row.getOwner() != null
                && row.getOwner().getId().equals(user.getId()))) {
                throw ownedCustomers();
            }
        }

        List<String> demosToDelete = new ArrayList<>();
        for (String productId : enabledProducts) {
            provider.verifyContract(productId);
            JsonNode customers = provider.request(productId, "GET", "/customers", null);
            if (!customers.isArray()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Invalid customer list from " + products.productName(productId) + " provider");
            }
            Map<String, CustomerOwnership> local = new HashMap<>();
            localByProduct.get(productId).forEach(row -> local.put(row.getTenantId(), row));
            for (JsonNode customer : customers) {
                String tenantId = text(customer, "tenantId");
                if (tenantId == null) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "Invalid customer record from " + products.productName(productId) + " provider");
                }
                if (!local.containsKey(tenantId) && user.getUsername().equals(text(customer, "ownerRef"))) {
                    throw ownedCustomers();
                }
            }

            JsonNode demo;
            try {
                demo = provider.request(productId, "GET", "/demos/" + user.getUsername(), null);
            } catch (ApiException ex) {
                if (ex.getStatus() == HttpStatus.NOT_FOUND) continue;
                throw ex;
            }
            if (!demo.path("ready").isBoolean()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Invalid demo response from " + products.productName(productId) + " provider");
            }
            if (demo.path("ready").asBoolean()) demosToDelete.add(productId);
        }

        List<String> deleted = new ArrayList<>();
        for (String productId : demosToDelete) {
            try {
                provider.request(productId, "DELETE", "/demos/" + user.getUsername(), null);
                deleted.add(products.productName(productId));
            } catch (ApiException ex) {
                String progress = deleted.isEmpty() ? "No demos were deleted."
                    : "Demos deleted from " + String.join(", ", deleted) + ".";
                throw new ApiException(ex.getStatus(), "Sales account was not deleted. " + progress
                    + " Demo deletion failed for " + products.productName(productId)
                    + "; check provider state before retrying. " + ex.getMessage());
            }
        }
    }

    private ApiException ownedCustomers() {
        return new ApiException(HttpStatus.CONFLICT,
            "Sales user owns customers; reassign them before deletion");
    }

    private String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }
}
