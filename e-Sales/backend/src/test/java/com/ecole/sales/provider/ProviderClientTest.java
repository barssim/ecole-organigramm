package com.ecole.sales.provider;

import com.ecole.sales.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ProviderClientTest {
    @Test void defaultsProviderReadTimeoutTo120SecondsAndRejectsInvalidValues(){
        AppProperties props=new AppProperties();
        assertEquals(120,props.getProviderReadTimeoutSeconds());
        props.setProviderReadTimeoutSeconds(0);
        assertThrows(IllegalStateException.class,()->new ProviderClient(new ProductService(props),new ObjectMapper(),props));
    }

    @Test void validatesContractAndSendsStableOwnerAndCredential() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var observed=new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/",exchange->{
            observed.set(exchange.getRequestMethod()+" "+exchange.getRequestURI()+" "+exchange.getRequestHeaders().getFirst("X-Provider-Key")+" "+new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            String payload=exchange.getRequestURI().getPath().equals("/api/provider/v1/capabilities")
                ? "{\"product\":\"school\",\"contractVersion\":1,\"features\":[\"demos\",\"customers\"]}"
                : "{\"tenantId\":\"demo-alex\",\"ready\":true,\"tenantUrl\":\"http://demo-alex.localhost:3080/\",\"password\":\"333333\",\"logins\":[]}";
            byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        try {
            AppProperties props=new AppProperties();var school=new AppProperties.Provider();
            school.setUrl("http://127.0.0.1:"+server.getAddress().getPort()+"/api/provider/v1");school.setKey("private-key");school.setEnabled(true);
            props.getProviders().setSchool(school);
            ProviderClient client=new ProviderClient(new ProductService(props),new ObjectMapper(),props);
            client.verifyContract("school");
            var demo=client.request("school","PUT","/demos/alex-sales",Map.of("ownerName","Alex"));
            assertEquals("demo-alex",demo.path("tenantId").asText());
            assertTrue(observed.get().contains("PUT /api/provider/v1/demos/alex-sales private-key"));
            assertTrue(observed.get().endsWith("{\"ownerName\":\"Alex\"}"));
        } finally {server.stop(0);}
    }

    @Test void refusesDisabledProviderWithoutCallingIt(){
        AppProperties props=new AppProperties();
        ProviderClient client=new ProviderClient(new ProductService(props),new ObjectMapper(),props);
        var error=assertThrows(com.ecole.sales.common.ApiException.class,()->client.request("cafe","GET","/capabilities",null));
        assertEquals(503,error.getStatus().value());
    }
}
