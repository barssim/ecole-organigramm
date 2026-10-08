package com.ecole.sales.demo;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.provider.*;
import com.ecole.sales.security.*;
import com.ecole.sales.user.SalesUser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/me/demos")
public class DemoController {
    private final CurrentUser current;private final ProviderClient provider;private final ProductService products;
    public DemoController(CurrentUser current,ProviderClient provider,ProductService products){this.current=current;this.provider=provider;this.products=products;}
    public record Login(String username,String role){}
    public record Demo(String productId,String productName,boolean enabled,String status,String tenantId,String tenantUrl,String password,List<Login> logins,String message){}
    @GetMapping public List<Demo> list(){current.get();return products.all().stream().map(p->getDemo(p.id())).toList();}
    @PutMapping("/{productId}") public Demo create(@PathVariable String productId){
        SalesUser user=current.get();products.productName(productId);provider.verifyContract(productId);
        JsonNode result=provider.request(productId,"PUT","/demos/"+user.getUsername(),Map.of("ownerName",user.getName()));
        return map(productId,result,null);
    }
    @DeleteMapping("/{productId}") public ResponseEntity<Void> delete(@PathVariable String productId){
        SalesUser user=current.get();products.productName(productId);provider.verifyContract(productId);
        provider.request(productId,"DELETE","/demos/"+user.getUsername(),null);return ResponseEntity.noContent().build();
    }
    private Demo getDemo(String id){
        String name=products.productName(id);
        if(!products.enabled(id))return new Demo(id,name,false,"UNAVAILABLE",null,null,null,List.of(),name+" provider is disabled or not configured");
        try{
            provider.verifyContract(id);JsonNode data=provider.request(id,"GET","/demos/"+current.get().getUsername(),null);
            if(data.has("ready")&&!data.path("ready").asBoolean())return new Demo(id,name,true,"NOT_CREATED",text(data,"tenantId"),null,null,List.of(),null);
            return map(id,data,null);
        }catch(ApiException e){
            if(e.getStatus()==HttpStatus.NOT_FOUND)return new Demo(id,name,true,"NOT_CREATED",null,null,null,List.of(),null);
            return new Demo(id,name,true,"UNAVAILABLE",null,null,null,List.of(),e.getMessage());
        }
    }
    private Demo map(String id,JsonNode data,String msg){
        if(!data.has("ready")||!data.path("ready").isBoolean())throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid demo response from product provider");
        boolean ready=data.path("ready").asBoolean();
        if(ready&&(text(data,"tenantId")==null||text(data,"tenantUrl")==null||text(data,"password")==null||!data.path("logins").isArray()))
            throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid demo response from product provider");
        List<Login> logins=new ArrayList<>();JsonNode array=data.path("logins");
        if(array.isArray())for(JsonNode item:array)logins.add(new Login(text(item,"username"),text(item,"role")));
        return new Demo(id,products.productName(id),true,ready?"READY":"NOT_CREATED",text(data,"tenantId"),text(data,"tenantUrl"),text(data,"password"),List.copyOf(logins),msg);
    }
    private String text(JsonNode node,String field){return node.hasNonNull(field)?node.get(field).asText():null;}
}
