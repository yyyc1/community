package com.nowcoder.community.controller;


import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConst {


    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;


    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCountByUserId(user.getId()));

        List<Message> conversationList = messageService.findConversations(user.getId(),page.getOffset(),page.getLimit());

        List<Map<String, Object>> conversations = new ArrayList<>();
        if(conversationList != null){
            for(Message message : conversationList){
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(message.getConversationId(), user.getId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));
                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);


        int letterUnreadCount = messageService.findLetterUnreadCount(null, user.getId());
        int noticeUnreadCount = messageService.findNoticeUnreadCount(null, user.getId());

        model.addAttribute("letterUnreadCount", letterUnreadCount);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return"/site/letter";
    }


    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Model model){

        int pageSize = 10;

        List<Message> letterList = messageService.findLetters(conversationId, 99999999, pageSize);
        List<Map<String, Object>> letterVoList= new ArrayList<>();
        for(int i = letterList.size() - 1;i >= 0;i--){
            Map<String, Object> map = new HashMap<>();
            Message letter = letterList.get(i);
            map.put("letter",letter);
            User fromUser = userService.findUserById(letter.getFromId());
            map.put("fromUser",fromUser);
            letterVoList.add(map);
        }
        model.addAttribute("letterVoList", letterVoList);

        User target = getLetterTarget(conversationId);

        model.addAttribute("target", target);

        model.addAttribute("user",hostHolder.getUser());

        List<Integer> ids = getLetterIds(letterList);
        if(!ids.isEmpty())
            messageService.readMessage(ids);

        return "/site/letter-detail";
    }

    private User getLetterTarget(String conversationId){
        int id0 = Integer.parseInt(conversationId.split("_")[0]);
        int id1 = Integer.parseInt(conversationId.split("_")[1]);
        if(id0 == hostHolder.getUser().getId())
            return userService.findUserById(id1);
        else
            return userService.findUserById(id0);
    }

    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> res = new ArrayList<>();
        if(letterList != null){
            for(Message letter : letterList){
                if(letter.getToId() == hostHolder.getUser().getId() && letter.getStatus() == 0){
                    res.add(letter.getId());
                }
            }
        }
        return res;
    }


    @RequestMapping(path = "/letter/loadMoreHistoryLetter", method = RequestMethod.POST)
    @ResponseBody
    public String loadMoreHistoryLetter(@RequestParam("conversationId") String conversationId, @RequestParam("latestLetterId") int latestLetterId){
        int pageSize = 10;
        List<Message> letterList = messageService.findLetters(conversationId, latestLetterId, pageSize);
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> letterVoList = new ArrayList<>();
        for(int i = letterList.size() - 1;i >= 0;i--){
            Map<String, Object> map = new HashMap<>();
            Message letter = letterList.get(i);
            map.put("letter",letter);
            map.put("fromUser", userService.findUserById(letter.getFromId()));

            letterVoList.add(map);
        }
        result.put("letterList", letterVoList);
        result.put("hasMore", letterList.size() == pageSize);
        return CommunityUtil.getJSONString(0,"加载成功",result);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model){

        User user = hostHolder.getUser();

        //评论通知
        Message commentNotice = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if(commentNotice != null){
            Map<String, Object> map = new HashMap<>();
            map.put("commentNotice", commentNotice);
            String content = HtmlUtils.htmlUnescape(commentNotice.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user", userService.findUserById((Integer)data.get("userId")));
            map.put("entityType", data.get("entityType"));
            map.put("entityId", data.get("entityId"));
            map.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(TOPIC_COMMENT,user.getId());
            map.put("count", count);
            int unread = messageService.findNoticeUnreadCount(TOPIC_COMMENT, user.getId());
            map.put("unread", unread);

            model.addAttribute("commentVo" ,map);
        }


        //点赞通知
        Message likeNotice = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        if(likeNotice != null){
            Map<String, Object> map = new HashMap<>();
            map.put("likeNotice", likeNotice);
            String content = HtmlUtils.htmlUnescape(likeNotice.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user", userService.findUserById((Integer)data.get("userId")));
            map.put("entityType", data.get("entityType"));
            map.put("entityId", data.get("entityId"));
            map.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(TOPIC_LIKE,user.getId());
            map.put("count", count);
            int unread = messageService.findNoticeUnreadCount(TOPIC_LIKE, user.getId());
            map.put("unread", unread);

            model.addAttribute("likeVo" ,map);
        }


        //关注通知
        Message followNotice = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if(likeNotice != null){
            Map<String, Object> map = new HashMap<>();
            map.put("followNotice", followNotice);
            String content = HtmlUtils.htmlUnescape(followNotice.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            map.put("user", userService.findUserById((Integer)data.get("userId")));
            map.put("entityType", data.get("entityType"));
            map.put("entityId", data.get("entityId"));
            map.put("postId", data.get("postId"));

            int count = messageService.findNoticeCount(TOPIC_FOLLOW,user.getId());
            map.put("count", count);
            int unread = messageService.findNoticeUnreadCount(TOPIC_FOLLOW, user.getId());
            map.put("unread", unread);

            model.addAttribute("followVo" ,map);
        }
        int unreadLetterCount = messageService.findLetterUnreadCount(null, user.getId());
        int unreadNoticeCount = messageService.findNoticeUnreadCount(null, user.getId());
        model.addAttribute("unreadLetterCount" ,unreadLetterCount);
        model.addAttribute("unreadNoticeCount" ,unreadNoticeCount);
        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Model model, Page page){
        User user = hostHolder.getUser();
        int noticeCount = messageService.findNoticeCount(topic, user.getId());

        page.setRows(noticeCount);
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);

        List<Message> commentNoticeList = messageService.findNoticeByUserId(user.getId(), topic, page.getLimit(), page.getOffset());


        model.addAttribute("commentNoticeList", commentNoticeList);
        return "/site/notice-detail";
    }



    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(int targetId, String content){
        int fromId = hostHolder.getUser().getId();
        if (userService.findUserById(targetId) == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(fromId);
        message.setToId(targetId);
        message.setContent(HtmlUtils.htmlUnescape(content));
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return CommunityUtil.getJSONString(0);
    }





}
