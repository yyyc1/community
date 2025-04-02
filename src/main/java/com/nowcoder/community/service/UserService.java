package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements CommunityConst {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        if(user == null){
            throw new IllegalArgumentException("参数不能为空");
        }

        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","用户名不能为空");
            return map;
        }

        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        if(StringUtils.isBlank(user.getUsername())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        if(userMapper.selectByUsername(user.getUsername()) != null){
            map.put("usernameMsg","用户名已被使用");
            return map;
        }

        if(userMapper.selectByEmail(user.getEmail()) != null){
            map.put("emailMsg","邮箱已被注册");
            return map;
        }

        user.setType(0);
        user.setStatus(0);
        user.setSalt(CommunityUtil.generateRandomUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+ user.getSalt()));
        user.setCreateTime(new Date());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setActivationCode(CommunityUtil.generateRandomUUID());
        userMapper.insertUser(user);

//        url =  http://localhost:8080/community/activation/101/code
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + contextPath + "/activation/" + user.getId() +"/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);


        return map;
    }

    public int activation(int userId, String code){
         User user = userMapper.selectById(userId);
         if(user.getStatus() == 1){
             return ACTIVATION_REPEAT;
         }else if(code.equals(user.getActivationCode())){
             userMapper.updateStatus(userId, 1);
             return ACTIVATION_TRUE;
         }else{
             return ACTIVATION_FAILURE;
         }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds){
        Map<String, Object> map = new HashMap<>();
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","用户名不能为空");
            return map;
        }

        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        User user = userMapper.selectByUsername(username);
        if(user == null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }

        if(user.getStatus() == 0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }

        if(!user.getPassword().equals(CommunityUtil.md5(password + user.getSalt()))){
            map.put("passwordMsg","密码错误");
            return map;
        }

        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * expiredSeconds));
        loginTicket.setTicket(CommunityUtil.generateRandomUUID());
        loginTicketMapper.insertLoginTicket(loginTicket);
        map.put("ticket", loginTicket.getTicket());

        return map;
    }


}
