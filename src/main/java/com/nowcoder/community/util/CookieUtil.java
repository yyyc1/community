package com.nowcoder.community.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;


public class CookieUtil {
    public static String getValue(HttpServletRequest request, String name){
        if(request == null || name == null){
            throw new IllegalArgumentException("参数不能为空");
        }




        Cookie[] cookies = request.getCookies();



        if(cookies == null) return null;

        for(Cookie cookie : cookies){
            if(cookie.getName().equals(name)) return cookie.getValue();
        }

        return null;
    }
}
