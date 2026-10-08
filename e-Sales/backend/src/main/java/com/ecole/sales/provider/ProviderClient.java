package com.ecole.sales.provider;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.config.AppProperties;
import com.fasterxml.jackson.databind.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Duration;
import java.util.*;

@Service
public class ProviderClient {
    private final ProductService products; private final RestTemplate http; private final ObjectMapper mapper;
    public ProviderClient(ProductService products,ObjectMapper mapper,AppProperties properties){
        this.products=products;this.mapper=mapper;
        int timeoutSeconds=properties.getProviderReadTimeoutSeconds();
        if(timeoutSeconds<1)throw new IllegalStateException("PROVIDER_READ_TIMEOUT_SECONDS must be at least 1");
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http=new RestTemplate(factory);
    }
    public JsonNode request(String product,String method,String path,Object body){
        if(!products.enabled(product))throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,products.productName(product)+" provider is disabled or not configured");
        AppProperties.Provider cfg=products.config(product);
        try{
            URI uri=URI.create(cfg.getUrl().replaceAll("/+$","")+path);
            HttpHeaders headers=new HttpHeaders();headers.set("X-Provider-Key",cfg.getKey());headers.setContentType(MediaType.APPLICATION_JSON);headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<?> entity=new HttpEntity<>(body,headers);
            ResponseEntity<String> response=http.exchange(uri,HttpMethod.valueOf(method),entity,String.class);
            if(response.getStatusCode().is2xxSuccessful()&&method.equals("DELETE"))return mapper.createObjectNode();
            String raw=response.getBody();return raw==null||raw.isBlank()?mapper.createObjectNode():mapper.readTree(raw);
        }catch(HttpStatusCodeException ex){
            String message="Product provider request failed";
            try{JsonNode err=mapper.readTree(ex.getResponseBodyAsString());if(err.hasNonNull("message"))message=err.get("message").asText();}catch(JsonProcessingException ignored){}
            HttpStatus status=ex.getStatusCode().value()==409?HttpStatus.CONFLICT:
                ex.getStatusCode().value()==404?HttpStatus.NOT_FOUND:HttpStatus.BAD_GATEWAY;
            throw new ApiException(status,message);
        }catch(ResourceAccessException ex){
            if(method.equals("GET"))throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,products.productName(product)+" provider is unreachable");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,products.productName(product)+" provider did not confirm the "+method.toLowerCase(Locale.ROOT)+" operation; it may have completed. Check provider state before retrying");
        }
        catch(RestClientException ex){throw new ApiException(HttpStatus.BAD_GATEWAY,"Product provider request failed");}
        catch(JsonProcessingException ex){throw new ApiException(HttpStatus.BAD_GATEWAY,"Invalid JSON response from product provider");}
        catch(IllegalArgumentException ex){throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,products.productName(product)+" provider configuration is invalid");}
    }
    public void verifyContract(String product){
        JsonNode caps=request(product,"GET","/capabilities",null);
        if(!caps.path("contractVersion").canConvertToInt()||caps.path("contractVersion").asInt()!=1||
           !products.productName(product).equalsIgnoreCase(caps.path("product").asText())||
           !caps.path("features").isArray()||!contains(caps.path("features"),"demos")||!contains(caps.path("features"),"customers"))
            throw new ApiException(HttpStatus.BAD_GATEWAY,"Product provider does not satisfy contract version 1");
    }
    private boolean contains(JsonNode arr,String value){for(JsonNode n:arr)if(value.equals(n.asText()))return true;return false;}
}
