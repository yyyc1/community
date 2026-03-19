package com.nowcoder.community.security;

import com.nowcoder.community.service.UserService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class CaptchaDaoAuthenticationProvider extends DaoAuthenticationProvider {

    @Autowired
    UserService userService;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        validateCaptcha(request);
        super.additionalAuthenticationChecks(userDetails, authentication);
    }

    private void validateCaptcha(HttpServletRequest request){
        String inputCaptcha = request.getParameter("code");
        String captcha = userService.getCaptcha(request.getParameter("captchaKey"));
        if(StringUtils.isBlank(inputCaptcha)){
            throw new AuthenticationServiceException("验证码不能为空");
        }
        if(StringUtils.isBlank(captcha)){
            throw new AuthenticationServiceException("验证码已经过期");
        }
        if(!inputCaptcha.equalsIgnoreCase(captcha)){
            throw new AuthenticationServiceException("验证码错误");
        }
    }



}
