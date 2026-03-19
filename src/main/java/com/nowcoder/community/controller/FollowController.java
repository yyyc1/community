package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller

public class FollowController implements CommunityConst {


    @Autowired
    FollowService followService;

    @Autowired
    HostHolder hostHolder;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    UserService userService;

    private static final int MAX_FOLLOWEE_DISPLAY_ROWS = 300;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId){
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);

        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);

        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0, "关注成功!");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId){
        User user = hostHolder.getUser();
        followService.unfollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注");
    }

    @RequestMapping(path = "/followee/{userId}", method = RequestMethod.GET)
    public String getFollowee(@PathVariable("userId") int userId, Model model, Page page){
        User user = userService.findUserById(userId);
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        page.setRows((int)Math.min(followeeCount, MAX_FOLLOWEE_DISPLAY_ROWS));
        page.setPath("/followee/" + userId);
        page.setLimit(5);
        List<Map<String, Object>> followeeVO = followService.findFollowees(userId, page.getLimit(), page.getOffset());
        model.addAttribute("user", user);
        model.addAttribute("followeeVO", followeeVO);
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null && hostHolder.getUser().getId()!=userId)
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/followee";
    }

    @RequestMapping(path = "/follower/{userId}", method = RequestMethod.GET)
    public String getFollower(@PathVariable("userId") int userId, Model model, Page page){

        User user = userService.findUserById(userId);
        long followerCount = followService.findFollowerCount(userId, ENTITY_TYPE_USER);
        page.setRows((int)Math.min(followerCount, MAX_FOLLOWEE_DISPLAY_ROWS));
        page.setPath("/follower/" + userId);
        page.setLimit(5);
        List<Map<String, Object>> followerVO = followService.findFollowers(userId, page.getLimit(), page.getOffset());
        model.addAttribute("user", user);
        model.addAttribute("followerVO", followerVO);
        boolean hasFollowed = false;
        if(hostHolder.getUser() != null && hostHolder.getUser().getId()!=userId)
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/follower";
    }




}
