package com.ecole.sales.provider;

import com.ecole.sales.config.AppProperties;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ProductService {
    private final AppProperties properties;
    public ProductService(AppProperties properties){this.properties=properties;}
    public record Product(String id,String name,boolean enabled){}
    public AppProperties.Provider config(String id){
        if("school".equals(id))return properties.getProviders().getSchool();
        if("cafe".equals(id))return properties.getProviders().getCafe();
        throw new com.ecole.sales.common.ApiException(org.springframework.http.HttpStatus.NOT_FOUND,"Unknown product");
    }
    public List<Product> all(){return List.of(product("school","School"),product("cafe","Cafe"));}
    public Product product(String id,String name){var p=config(id);return new Product(id,name,p.isEnabled()&&p.getUrl()!=null&&!p.getUrl().isBlank()&&p.getKey()!=null&&!p.getKey().isBlank());}
    public boolean enabled(String id){return all().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow().enabled();}
    public String productName(String id){return switch(id){case "school"->"School";case "cafe"->"Cafe";default->throw new com.ecole.sales.common.ApiException(org.springframework.http.HttpStatus.NOT_FOUND,"Unknown product");};}
}
