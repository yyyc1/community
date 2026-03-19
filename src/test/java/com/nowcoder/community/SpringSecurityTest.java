package com.nowcoder.community;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class SpringSecurityTest {
    @Test
    public void getPassword(){
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        for (int i = 0; i < 10; i++) {
            String password = encoder.encode("123456");
            System.out.println("123456的密文为" + password);
            System.out.println(encoder.matches("123456", password));
        }
    }
}
