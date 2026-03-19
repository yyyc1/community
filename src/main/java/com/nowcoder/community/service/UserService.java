package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConst, UserDetailsService {
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

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map = new HashMap<>();
        try{
            if (user == null) {
                throw new IllegalArgumentException("参数不能为空");
            }

            if (StringUtils.isBlank(user.getUsername())) {
                map.put("usernameMsg", "用户名不能为空");
                // 入参校验失败，统计为失败
                countUserOperate("register", RESULT_FAIL);
                return map;
            }

            if (StringUtils.isBlank(user.getPassword())) {
                map.put("passwordMsg", "密码不能为空");
                countUserOperate("register", RESULT_FAIL);
                return map;
            }

            if (StringUtils.isBlank(user.getEmail())) { // 修复原代码重复校验username的bug
                map.put("emailMsg", "邮箱不能为空");
                countUserOperate("register", RESULT_FAIL);
                return map;
            }

            if (userMapper.selectByUsername(user.getUsername()) != null) {
                map.put("usernameMsg", "用户名已被使用");
                countUserOperate("register", RESULT_FAIL);
                return map;
            }

            if (userMapper.selectByEmail(user.getEmail()) != null) {
                map.put("emailMsg", "邮箱已被注册");
                countUserOperate("register", RESULT_FAIL);
                return map;
            }
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            user.setType(0);
            user.setStatus(0);
            user.setPassword(encoder.encode(user.getPassword()));
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
            countUserOperate("register",RESULT_SUCCESS);
            return map;
        }catch (Exception e){
            countUserOperate("register",RESULT_FAIL);
            map.put("systemMsg", "注册失败：" + e.getMessage());
            return map;
        }




    }

    public int activation(int userId, String code){
        try {
            User user = userMapper.selectById(userId);
            int result;
            if (user.getStatus() == 1) {
                result = ACTIVATION_REPEAT;
                // 重复激活，统计为成功（操作本身无异常，只是状态重复）
                countUserOperate("activation", RESULT_SUCCESS);
            } else if (code.equals(user.getActivationCode())) {
                userMapper.updateStatus(userId, 1);
                result = ACTIVATION_TRUE;
                countUserOperate("activation", RESULT_SUCCESS);
            } else {
                result = ACTIVATION_FAILURE;
                // 激活码错误，统计为失败
                countUserOperate("activation", RESULT_FAIL);
            }
            return result;
        }catch (Exception e){
            countUserOperate("activation", RESULT_FAIL);
            throw e;
        }

    }


//    public LoginTicket findLoginTicket(String ticket){
//        String redisKey = RedisUtil.getTicketKey(ticket);
//        Object redisValue = redisTemplate.opsForValue().get(redisKey);
//        if (redisValue != null) {
//            return (LoginTicket) redisValue;
//        } else {
//            return loginTicketMapper.selectByTicket(ticket);
//        }
//    }





    public int updatePassword(int userId, String newPassword) {
        try {
            int rows = userMapper.updatePassword(userId, newPassword);
            countUserOperate("update_password",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countUserOperate("update_password",RESULT_FAIL);
            throw e;
        }

    }

    public int updateHeaderUrl(int id, String headerUrl) {
        try {
            int rows = userMapper.updateHeader(id, headerUrl);
            countUserOperate("update_headerUrl",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countUserOperate("update_headerUrl",RESULT_FAIL);
            throw e;
        }

    }


    public User findUserByUsername(String username){
        return userMapper.selectByUsername(username);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.findUserByUsername(username);
        if(user == null) throw new UsernameNotFoundException("用户名不存在");
        if(user.getStatus() == 0) throw new UsernameNotFoundException("用户未激活");
        return user;
    }


    public Map<String, Object> sendForgetEmail(String email) {
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userMapper.selectByEmail(email);
            if (user == null) {
                res.put("errorMsg", "邮箱未注册");
                countUserOperate("forget_mail", RESULT_FAIL);
                return res;
            }
            if (user.getStatus() == 0) {
                res.put("errorMsg", "邮箱未激活");
                countUserOperate("forget_mail", RESULT_FAIL);
                return res;
            }

            // 生成验证码并发送邮件
            Context context = new Context();
            context.setVariable("email", user.getEmail());
            String redisKey = RedisUtil.getForgetCodeKey(email);
            Object object = redisTemplate.opsForValue().get(redisKey);
            String code;
            if (object != null) {
                code = object.toString();
            } else {
                code = CommunityUtil.generateVerifyCode();
                redisTemplate.opsForValue().set(redisKey, code, CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);
            }
            context.setVariable("code", code);
            String content = templateEngine.process("/mail/forget", context);
            mailClient.sendMail(user.getEmail(), "重设密码", content);

            // 发送成功
            countUserOperate("forget_mail", RESULT_SUCCESS);
            return res;
        } catch (Exception e) {
            // 系统异常（Redis/邮件异常）
            res.put("errorMsg", "UNKNOWN");
            countUserOperate("forget_mail", RESULT_FAIL);
            return res;
        }
    }

    public Map<String,Object> doForget( String email,  String password,  String inputCode){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Map<String, Object> res = new HashMap<>();
        try {
            User user = userMapper.selectByEmail(email);
            if (user == null || user.getStatus() == 0) {
                res.put("errorMsg", "验证码错误");
                countUserOperate("forget", RESULT_FAIL);
                return res;
            }
            String redisKey = RedisUtil.getForgetCodeKey(email);
            Object object = redisTemplate.opsForValue().get(redisKey);
            if (object == null) {
                res.put("errorMsg", "验证码已过期");
                countUserOperate("forget", RESULT_FAIL);
                return res;
            }
            String code = (String) object;
            if (code.equals(inputCode)) {
                userMapper.updatePassword(user.getId(), encoder.encode(password));
                countUserOperate("forget", RESULT_SUCCESS);
            } else {
                res.put("errorMsg", "验证码错误");
                countUserOperate("forget", RESULT_FAIL);
            }
            return res;
        } catch (Exception e) {
            res.put("errorMsg", "重置密码失败：" + e.getMessage());
            countUserOperate("forget", RESULT_FAIL);
            return res;
        }
    }


    public String loadCaptcha(String text) {
        try {
            String captchaId = CommunityUtil.generateRandomUUID();
            String redisKey = RedisUtil.getLoginCode(captchaId);
            redisTemplate.opsForValue().set(redisKey, text, 300 , TimeUnit.SECONDS);
            countUserOperate("captcha",RESULT_SUCCESS);
            return redisKey;
        }catch (Exception e){
            countUserOperate("captcha",RESULT_FAIL);
            throw e;
        }

    }

    public String getCaptcha(String captchaKey) {
        return (String) redisTemplate.opsForValue().get(captchaKey);
    }

    private void countUserOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.user.operate.total",
                "module", "user",
                "operate",operateType,
                "result", result
        ).increment();
    }
}
