package com.ecole.sales.security;

import com.ecole.sales.user.SalesUser;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CurrentUserArgumentResolver implements WebMvcConfigurer {
    private final CurrentUser current;
    public CurrentUserArgumentResolver(CurrentUser current){this.current=current;}
    @Override public void addArgumentResolvers(java.util.List<HandlerMethodArgumentResolver> resolvers){
        resolvers.add(new HandlerMethodArgumentResolver(){
            public boolean supportsParameter(MethodParameter p){return p.hasParameterAnnotation(CurrentUserArg.class)&&p.getParameterType()==SalesUser.class;}
            public Object resolveArgument(MethodParameter p,ModelAndViewContainer c,NativeWebRequest r,WebDataBinderFactory b){return current.get();}
        });
    }
}
