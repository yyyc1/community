package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConst {

    @Value("T6PIpdZ4XgQEt6ze6Spa22qXLoZ_7QPq_Q-2n4nx")
    private String accessKey;

    @Value("xQdsCI2HM-NR0V2pmd6_xhuIiafNscnaegwcp540")
    private String secretKey;

    @Value("ycy-community-headerurl")
    private String headerBucketName;

    @Value("http://t95i2j7bk.hd-bkt.clouddn.com")
    private String headerBucketUrl;


    @Autowired
    private UserService userService;


    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private DiscussPostService discussPostService;


    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model){

        String fileName = CommunityUtil.generateRandomUUID();

        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }



    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if (user == null){
            throw new RuntimeException("该用户不存在");
        }

        model.addAttribute("user", user);

        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        long followeeCount= followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        long followerCount = followService.findFollowerCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followerCount", followerCount);

        boolean hasFollowed = false;
        if(hostHolder.getUser() != null && hostHolder.getUser().getId()!=userId)
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        model.addAttribute("hasFollowed", hasFollowed);


        return "/site/profile";
    }

    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    @ResponseBody
    public String updatePassword(String oldPassword, String newPassword, @CookieValue("ticket") String ticket, HttpServletRequest request,
                                 HttpServletResponse response){
        User user = hostHolder.getUser();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if(user.getPassword().equals(encoder.encode(oldPassword))){
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            userService.updatePassword(user.getId(), encoder.encode(newPassword));
            logout(request, response);
            return CommunityUtil.getJSONString(0, "修改成功", result);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        return CommunityUtil.getJSONString(0, "原密码错误，请重新输入", result);

    }

    private void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            // 使用 Spring Security 内置的登出处理器，清除认证信息、失效 Session
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

    }


    @RequestMapping(path = "/updateHeaderUrl/{fileName}", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(@PathVariable("fileName") String fileName){
        if (fileName == null || fileName.trim().isEmpty()) {
            // 返回规范的错误JSON：code=1（失败）+ 错误提示
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }
        try {
            String headerUrl = headerBucketUrl + "/" + fileName;
            userService.updateHeaderUrl(hostHolder.getUser().getId(), headerUrl);
            User newUser = userService.findUserById(hostHolder.getUser().getId());

            // 4. 第三步：更新Spring Security的SecurityContext（核心！替换旧用户）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            // 重新构建Authentication，principal传入最新用户对象
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newUser, // 最新用户对象（含新headerUrl）
                    authentication.getCredentials(), // 保留原有凭证（密码/令牌）
                    authentication.getAuthorities() // 保留原有权限
            );
            // 替换SecurityContext中的旧认证信息
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            return CommunityUtil.getJSONString(0, "头像更新成功！");
        }catch (Exception e ){
            return CommunityUtil.getJSONString(3, "头像更新出错");
        }

    }


    @RequestMapping(path = "/getUploadToken/{fileName}", method = RequestMethod.GET)
    @ResponseBody
    public String getUpdateToken(@PathVariable("fileName") String fileName){
        try {
            StringMap policy = new StringMap();
            policy.put("returnBody", CommunityUtil.getJSONString(0));
            Auth auth = Auth.create(accessKey, secretKey);
            String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
            Map<String, Object> result = new HashMap<>();
            result.put("uploadToken", uploadToken);
            return CommunityUtil.getJSONString(0,"获取token成功", result);
        }catch (Exception e){
            return CommunityUtil.getJSONString(1,"获取token失败");
        }

    }

    @RequestMapping(path = "/my-reply/{userId}", method = RequestMethod.GET)
    public String getUserReplies(@PathVariable("userId") int userId, Model model, Page page){
        User user = userService.findUserById(userId);
        int replyCount = commentService.findCommentCountByUserId(userId, ENTITY_TYPE_COMMENT);

        page.setRows(replyCount);
        page.setPath("/my-reply/" + userId);
        page.setLimit(5);

        List<Comment> list = commentService.findCommentsByUserId(userId, ENTITY_TYPE_COMMENT, page.getLimit(), page.getOffset());




        List<Map<String, Object>> replyVO = new ArrayList<>();
        for(Comment reply : list){
            Map<String, Object> map = new HashMap<>();
            map.put("reply", reply);
            DiscussPost post = discussPostService.findDiscussPostById(reply.getEntityId());
            map.put("post",post);
            replyVO.add(map);
        }
        model.addAttribute("user", user);
        model.addAttribute("replyCount", replyCount);
        model.addAttribute("replyVO",replyVO);
        return "/site/my-reply";
    }

    @RequestMapping(path = "/my-post/{userId}", method = RequestMethod.GET)
    public String getUserPosts(@PathVariable("userId") int userId, Model model, Page page){
        User user = userService.findUserById(userId);
        int postCount = discussPostService.findDiscussPostsRows(userId);

        page.setRows(postCount);
        page.setPath("/my-post/" + userId);
        page.setLimit(5);

        List<DiscussPost> list = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);

        List<Map<String, Object>> postVO = new ArrayList<>();
        for(DiscussPost post : list){
            Map<String, Object> map = new HashMap<>();
            map.put("post", post);
            long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            map.put("likeCount",likeCount);
            postVO.add(map);
        }

        model.addAttribute("user", user);
        model.addAttribute("postCount", postCount);
        model.addAttribute("postVO",postVO);
        return "/site/my-post";
    }


}
