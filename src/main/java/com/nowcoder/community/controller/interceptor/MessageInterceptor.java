package com.nowcoder.community.controller.interceptor;


import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.util.HostHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class MessageInterceptor implements HandlerInterceptor {


    @Autowired
    MessageService messageService;

    @Autowired
    HostHolder hostHolder;




    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

        int UnreadLetterCount = messageService.findUnreadLetterCountByUserId(hostHolder.getUser().getId());
        int UnreadNoticeCount = messageService.findUnreadNoticeCountByUserId(hostHolder.getUser().getId());


        modelAndView.addObject("allUnreadCount", UnreadLetterCount + UnreadNoticeCount);
    }

}
