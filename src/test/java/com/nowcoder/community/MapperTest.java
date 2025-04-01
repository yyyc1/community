package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTest {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Test
    public void testConnection(){
        try (Connection connection = dataSource.getConnection()) {
            System.out.println("数据库连接成功！");
            System.out.println("连接信息：" + connection.getMetaData().getURL());
        } catch (SQLException e) {
            System.err.println("数据库连接失败！错误信息：");
            e.printStackTrace();
        }
    }

    @Test
    public void testSelect(){
        List<DiscussPost> list =  discussPostMapper.selectDiscussPosts(149,0,20);
        for(DiscussPost discussPost : list)
            System.out.println(discussPost.toString());
        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println(rows);
    }

    @Test
    public void testInsert(){
        User user = new User();
        user.setUsername("ycy");
        user.setPassword("123456");
        user.setSalt("abcd");
        user.setEmail("740752492@qq.com");
        user.setHeaderUrl("http://www.newcoder.com/101.png");
        user.setCreateTime(new Date());
        System.out.println(userMapper.insertUser(user));
    }

    @Test
    public void testUpdate(){
        System.out.println(userMapper.updateStatus(152,1));
    }


}
