package com.ecole.sales.customer;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.provider.*;
import com.ecole.sales.security.*;
import com.ecole.sales.user.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/customers")
public class CustomerController {
    private final CurrentUser current;private final ProductService products;private final ProviderClient provider;
    private final CustomerOwnershipRepository ownership;private final SalesUserRepository users;private final CustomerOwnershipService ownershipService;
    public CustomerController(CurrentUser current,ProductService products,ProviderClient provider,CustomerOwnershipRepository ownership,SalesUserRepository users,CustomerOwnershipService ownershipService){
        this.current=current;this.products=products;this.provider=provider;this.ownership=ownership;this.users=users;this.ownershipService=ownershipService;
    }
    public record Customer(String productId,String tenantId,String name,Instant createdAt,int userCount,boolean deletable,Long ownerId,String ownerName){}
    public record AdminInfo(@NotBlank @Size(max=48) String username,@NotBlank @Size(max=160) String name,@NotBlank @Email @Size(max=254) String email){}
    public record CreateCustomer(@NotBlank String productId,@NotBlank @Size(max=160) String tenantId,@NotBlank @Size(max=200) String name,@NotBlank @Email @Size(max=254) String email,@NotNull @Valid AdminInfo admin){}
    public record Created(String productId,String tenantId,String name,String invitationLink,boolean invitationEmailSent){}
    public record ChangeOwner(Long salesUserId){}
    @GetMapping public List<Customer> list(@RequestParam String productId){
        SalesUser actor=current.get();products.productName(productId);provider.verifyContract(productId);
        JsonNode remote=provider.request(productId,"GET","/customers",null);
        if(!remote.isArray())throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid customer list from product provider");
        Map<String,CustomerOwnership> local=new HashMap<>();
        for(CustomerOwnership row:ownership.findAllByProductId(productId))local.put(row.getTenantId(),row);
        List<Customer> result=new ArrayList<>();
        for(JsonNode item:remote){
            String tenant=text(item,"tenantId"),name=text(item,"name");
            if(tenant==null||name==null)throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid customer record from product provider");
            CustomerOwnership row=local.get(tenant);
            SalesUser owner=row==null?null:row.getOwner();
            if(row==null){String ref=text(item,"ownerRef");if(ref!=null)owner=users.findByUsername(ref).orElse(null);}
            if(!current.isAdmin(actor)&&(owner==null||!owner.getId().equals(actor.getId())))continue;
            Instant created=parseInstant(text(item,"createdAt"));
            result.add(new Customer(productId,tenant,name,created,item.path("userCount").asInt(0),item.path("deletable").asBoolean(false),owner==null?null:owner.getId(),owner==null?null:owner.getName()));
        }
        return result;
    }
    @PostMapping public ResponseEntity<Created> create(@Valid @RequestBody CreateCustomer request){
        String productId=request.productId();
        SalesUser actor=current.get();products.productName(productId);provider.verifyContract(productId);
        Map<String,Object> admin=Map.of("username",request.admin().username(),"name",request.admin().name(),"email",request.admin().email());
        Map<String,Object> body=Map.of("tenantId",request.tenantId(),"name",request.name(),"email",request.email(),"admin",admin,"ownerRef",actor.getUsername());
        JsonNode result=provider.request(productId,"POST","/customers",body);
        String tenant=text(result,"tenantId"), createdName=text(result,"name"), invitationLink=text(result,"invitationLink");
        if(tenant==null||createdName==null||invitationLink==null)
            throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid customer creation response from product provider");
        ownership.save(new CustomerOwnership(productId,tenant,actor,request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new Created(productId,tenant,createdName,invitationLink,result.path("invitationEmailSent").asBoolean(false)));
    }
    @PutMapping("/{productId}/{tenantId}/owner") public ResponseEntity<Void> changeOwner(@PathVariable String productId,@PathVariable String tenantId,@RequestBody ChangeOwner request){
        current.requireAdmin(current.get());products.productName(productId);
        if(request==null)throw new ApiException(HttpStatus.BAD_REQUEST,"Invalid request");
        CustomerOwnership row=ownership.findByProductIdAndTenantId(productId,tenantId).orElse(null);
        String customerName=row==null?null:row.getCustomerName();
        if(row==null){
            provider.verifyContract(productId);JsonNode remote=provider.request(productId,"GET","/customers",null);
            if(!remote.isArray())throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid customer list from product provider");
            for(JsonNode item:remote)if(tenantId.equals(text(item,"tenantId"))){customerName=text(item,"name");break;}
            if(customerName==null)throw new ApiException(HttpStatus.NOT_FOUND,"Customer not found");
        }
        ownershipService.assign(productId,tenantId,customerName,request.salesUserId());return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{productId}/{tenantId}") public ResponseEntity<Void> delete(@PathVariable String productId,@PathVariable String tenantId,@RequestParam String confirm){
        current.requireAdmin(current.get());products.productName(productId);
        if(!tenantId.equals(confirm))throw new ApiException(HttpStatus.BAD_REQUEST,"Confirmation must exactly match the tenant ID");
        provider.verifyContract(productId);provider.request(productId,"DELETE","/customers/"+encodePath(tenantId)+"?confirm="+encodePath(confirm),null);
        ownership.deleteByProductIdAndTenantId(productId,tenantId);
        return ResponseEntity.noContent().build();
    }
    private String encodePath(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8).replace("+","%20");}
    private String text(JsonNode n,String k){return n.hasNonNull(k)?n.get(k).asText():null;}
    private Instant parseInstant(String s){
        try{return s==null?null:Instant.parse(s);}
        catch(java.time.format.DateTimeParseException e){throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid customer date from product provider");}
    }
}
