package com.ecole.sales.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecole.sales.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:sales;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true","spring.mail.host=","app.jwt-secret=Y7vN5cQ2bK9mR4xH8pL3sW6dF1jA0uZq",
    "app.admin-username=admin","app.admin-email=admin@example.test","app.admin-initial-password=InitialStrongPassword1"
})
@AutoConfigureMockMvc
class AuthLifecycleTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppProperties appProperties;

    @Test void invitationIsSingleUseAndEnablesLogin() throws Exception {
        MvcResult loginResult=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"InitialStrongPassword1\"}"))
            .andExpect(status().isOk()).andReturn();
        String adminToken=mapper.readTree(loginResult.getResponse().getContentAsString()).path("token").asText();
        MvcResult create=mvc.perform(post("/api/admin/sales-users").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"alex-sales\",\"name\":\"Alex Sales\",\"email\":\"alex@example.test\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.invitationEmailSent").value(false)).andReturn();
        JsonNode created=mapper.readTree(create.getResponse().getContentAsString());
        long userId=created.path("id").asLong();
        String oldToken=token(created.path("invitationLink").asText());
        MvcResult resent=mvc.perform(post("/api/admin/sales-users/"+userId+"/invitation").header("Authorization","Bearer "+adminToken))
            .andExpect(status().isOk()).andReturn();
        String token=token(mapper.readTree(resent.getResponse().getContentAsString()).path("invitationLink").asText());
        mvc.perform(post("/api/auth/invitations/accept").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\""+oldToken+"\",\"password\":\"StrongSalesPassword2\"}")).andExpect(status().isBadRequest());
        String accepted="{\"token\":\""+token+"\",\"password\":\"StrongSalesPassword2\"}";
        mvc.perform(post("/api/auth/invitations/accept").contentType(MediaType.APPLICATION_JSON).content(accepted)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/invitations/accept").contentType(MediaType.APPLICATION_JSON).content(accepted)).andExpect(status().isBadRequest());
        MvcResult salesLogin=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alex-sales\",\"password\":\"StrongSalesPassword2\"}"))
            .andExpect(status().isOk()).andReturn();
        String salesToken=mapper.readTree(salesLogin.getResponse().getContentAsString()).path("token").asText();
        mvc.perform(get("/api/me").header("Authorization","Bearer "+salesToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("SALES_USER"));
        mvc.perform(get("/api/admin/sales-users").header("Authorization","Bearer "+salesToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/sales-users").header("Authorization","Bearer "+salesToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"second-sales\",\"name\":\"Second Sales\",\"email\":\"second@example.test\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/sales-users/"+userId+"/invitation")
                .header("Authorization","Bearer "+salesToken)).andExpect(status().isForbidden());
        mvc.perform(put("/api/customers/school/school-one/owner")
                .header("Authorization","Bearer "+salesToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"salesUserId\":null}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/customers/school/school-one?confirm=school-one")
                .header("Authorization","Bearer "+salesToken)).andExpect(status().isForbidden());
        assertTrue(appProperties.getProviderReadTimeoutSeconds() >= 120);
    }
    private String token(String link){
        return java.net.URLDecoder.decode(link.substring(link.indexOf("token=")+6),java.nio.charset.StandardCharsets.UTF_8);
    }
}
