package com.tutr.backend.admin.config;

import com.tutr.backend.admin.repository.AdminUserRepository;
import com.tutr.backend.repository.UserRepository;
import com.tutr.backend.security.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }

    @Bean
    @Primary
    public AdminUserRepository adminUserRepository() {
        return Mockito.mock(AdminUserRepository.class);
    }

    @Bean
    @Primary
    public UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }

    /**
     * Replaces Spring MVC's argument resolver list so that our Authentication
     * resolver runs BEFORE Spring Security's built-in one.
     */
    @Autowired
    public void customizeArgumentResolvers(RequestMappingHandlerAdapter adapter) {
        List<HandlerMethodArgumentResolver> custom = new ArrayList<>();

        // Our resolver first — reads from SecurityContextHolder
        custom.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Authentication.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return SecurityContextHolder.getContext().getAuthentication();
            }
        });

        // Then the existing resolvers (skip any that already handle Authentication)
        List<HandlerMethodArgumentResolver> existing = adapter.getArgumentResolvers();
        if (existing != null) {
            for (HandlerMethodArgumentResolver r : existing) {
                boolean handlesAuth = false;
                try {
                    handlesAuth = r.supportsParameter(
                            new MethodParameter(
                                    TestSecurityConfig.class.getDeclaredMethod("dummy", Authentication.class),
                                    0));
                } catch (NoSuchMethodException ignored) {
                }
                if (!handlesAuth) {
                    custom.add(r);
                }
            }
        }

        adapter.setArgumentResolvers(custom);
    }

    @SuppressWarnings("unused")
    private void dummy(Authentication auth) { }
}