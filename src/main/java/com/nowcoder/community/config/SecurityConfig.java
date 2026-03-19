package com.nowcoder.community.config;

import com.nowcoder.community.security.CaptchaDaoAuthenticationProvider;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@Configuration
public class SecurityConfig implements CommunityConst {

    @Autowired
    UserService userService;

    @Value("${spring.security.rememberme.key}")
    private String rememberMeKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public RememberMeServices rememberMeServices(){
        TokenBasedRememberMeServices rememberMeServices = new TokenBasedRememberMeServices(rememberMeKey, userService);
        rememberMeServices.setTokenValiditySeconds(604800);
        rememberMeServices.setParameter("remember");
        return rememberMeServices;
    }


    @Bean
    public CaptchaDaoAuthenticationProvider authenticationProvider(UserDetailsService userService, PasswordEncoder passwordEncoder) {
        // 1. 实例化自定义的子类（核心修改点）
        CaptchaDaoAuthenticationProvider provider = new CaptchaDaoAuthenticationProvider();

        // 2. 设置用户信息服务（必须，父类需要这个来查询用户）
        provider.setUserDetailsService(userService);

        // 3. 设置密码编码器（必须，父类需要这个验证密码）
        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        return (web) -> web.ignoring().requestMatchers("/resources/**");
    }




    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{


        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN,
                        AUTHORITY_USER,
                        AUTHORITY_MODERATOR
                ).requestMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(
                        AUTHORITY_MODERATOR
                ).requestMatchers(
                        "/discuss/delete",
                        "/data/**"
                ).hasAnyAuthority(
                        AUTHORITY_ADMIN
                ).anyRequest().permitAll()
        ).exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String xRequestWith = request.getHeader("x-requested-with");
                    if("XMLHttpRequest".equals(xRequestWith)){
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403,"你还没有登录"));
                    }else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }

                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    System.out.println("用户权限为:" + authentication);

                    String xRequestWith = request.getHeader("x-requested-with");
                    if("XMLHttpRequest".equals(xRequestWith)){
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403,"你没有权限访问"));
                    }else {
                        response.sendRedirect(request.getContextPath() + "/denied");
                    }
                })
        ).logout(
                logout -> logout
                    .logoutUrl("/logout")
        ).formLogin(
                form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/doLogin")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler((request, response, authentication) -> {
                        response.sendRedirect(request.getContextPath() + "/index"); // 登录成功重定向到首页
                    })
                    // 自定义失败跳转（核心修复：redirect 方式，发起新的 GET 请求）
                    .failureHandler((request, response, exception) -> {
                        // 携带错误信息重定向到登录页（GET 方法）
                        response.sendRedirect(request.getContextPath() + "/login?error=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
                    })
        ).rememberMe(remember -> remember.
                        rememberMeServices(rememberMeServices())
        ).sessionManagement(session -> session
                // 设置 Session 超时时间（测试用，默认30分钟）
                .invalidSessionUrl("/login")
                .maximumSessions(1) // 单用户单端登录（可选）
        ).csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
