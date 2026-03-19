package com.nowcoder.community.controller;


import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.wf.captcha.SpecCaptcha;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Controller
public class LoginController implements CommunityConst {


    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;


    @Value("${server.servlet.context-path}")

    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(Model model){
        return "/site/register";
    }

    @RequestMapping(path = "register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map =  userService.register(user);
        if(map == null || map.isEmpty()){
            model.addAttribute("msg","注册成功, 我们已向您的邮箱发送了一封激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path = "activation/{userId}/{activationCode}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("activationCode") String activationCode){
        int result = userService.activation(userId, activationCode);
        if(result == ACTIVATION_TRUE){
            model.addAttribute("msg","激活成功，您的账号可以正常使用了");
            model.addAttribute("target","/login");
        }else if(result == ACTIVATION_REPEAT){
            model.addAttribute("msg","账号已激活，无需重复激活");
            model.addAttribute("target","/login");
        }else{
            model.addAttribute("msg","激活失败，激活码不正确");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }

    @RequestMapping(path = "/captcha", method = RequestMethod.GET)
    @ResponseBody
    public String getCaptcha(HttpServletResponse response, HttpSession session){
        SpecCaptcha captcha = new SpecCaptcha(130, 48);
        String text = captcha.text();// 获取验证码的字符
//        session.setAttribute("captcha", text);
        // 输出验证码
        String redisKey = userService.loadCaptcha(text);
        Map<String, Object> result = new HashMap<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            captcha.out(baos); // 验证码图片写入字节数组流
        } catch (Exception e) {
            logger.error("生成验证码Base64失败：{}", e.getMessage(), e);
            return CommunityUtil.getJSONString(1, "生成验证码失败");
        }
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

        result.put("captchaKey",redisKey);
        result.put("base64Image",base64Image);
        return CommunityUtil.getJSONString(0, "生成验证码成功",result);
    }


    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage(){
        return "/site/forget";
    }

    @RequestMapping(path = "/doForget", method = RequestMethod.POST)
    @ResponseBody
    public String doForget(@RequestParam("email") String email,
                           @RequestParam("password") String password,
                           @RequestParam("inputCode") String inputCode){
        Map<String,Object> result = userService.doForget(email, password, inputCode);
        if(result == null || result.isEmpty())  return CommunityUtil.getJSONString(0,"修改密码成功");
        return CommunityUtil.getJSONString(1,result.get("errorMsg").toString());

    }

    @RequestMapping(path = "/sendForgetEmail", method = RequestMethod.POST)
    @ResponseBody
    public String sendForgetEmail(@RequestParam("email") String email){
        Map<String,Object> result = userService.sendForgetEmail(email);
        if(result == null || result.isEmpty())  return CommunityUtil.getJSONString(0,"邮件发送成功");
        return CommunityUtil.getJSONString(1,result.get("errorMsg").toString());
    }



}
