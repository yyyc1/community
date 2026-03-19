package com.nowcoder.community.controller;


import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConst {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private CommentService commentService;


    //发帖
    @RequestMapping(path = "/add", method = RequestMethod.POST)
    @ResponseBody
    public String addDisscussPost(String title, String content){
        User user = hostHolder.getUser();
        if(user == null){
            return CommunityUtil.getJSONString(403, "用户未登录");
        }

        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreatTime(new Date());

        discussPostService.addDiscussPost(discussPost);

        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId());

        eventProducer.fireEvent(event);

        String redisKey = RedisUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());



        return CommunityUtil.getJSONString(0,"发布成功");

    }


    @RequestMapping(path = "/detail/{discussId}",method = RequestMethod.GET)
    public String getPost(@PathVariable("discussId") int postId, Model model, Page page){
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);

        model.addAttribute("post", discussPost);

        User user = userService.findUserById(discussPost.getUserId());
        model.addAttribute("user", user);

        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST,postId);
        model.addAttribute("likeCount", likeCount);

        int likeStatus = hostHolder.getUser() == null ? 0 : likeService.findLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_POST, postId);
        model.addAttribute("likeStatus", likeStatus);

        page.setLimit(5);
        page.setPath("/discuss/detail/" + postId);
        page.setRows(discussPost.getCommentCount());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("当前用户: " + auth.getName());
        System.out.println("用户权限: " + auth.getAuthorities());


        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, postId, page.getOffset(), page.getLimit());

        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if(commentList != null){
            for(Comment comment :commentList){
                Map<String, Object> commentVo = new HashMap<>();
                commentVo.put("comment", comment);
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                likeStatus = hostHolder.getUser() == null ? 0 : likeService.findLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);

                List<Map<String, Object>> replyVoList = new ArrayList<>();
                for(Comment reply : replyList){
                    Map<String, Object> replyVo = new HashMap<>();
                    replyVo.put("reply", reply);
                    replyVo.put("user", userService.findUserById(reply.getUserId()));
                    User target = reply.getTargetId() == 0 ? null : userService.findUserById(reply.getTargetId());
                    replyVo.put("target", target);

                    likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeCount", likeCount);

                    likeStatus = hostHolder.getUser() == null ? 0 : likeService.findLikeStatus(hostHolder.getUser().getId(), ENTITY_TYPE_COMMENT, reply.getId());
                    replyVo.put("likeStatus", likeStatus);

                    replyVoList.add(replyVo);
                }

                commentVo.put("replys", replyVoList);

                int replyCount = commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getEntityId());
                commentVo.put("replyCount", replyCount);

                commentVoList.add(commentVo);
            }

        }
        model.addAttribute("comments", commentVoList);

        return "site/discuss-detail";
    }

        @RequestMapping(path = "/top", method = RequestMethod.POST)
        @ResponseBody
        public String setTop(@RequestParam("postId") int id,
                             @RequestParam("type") int targetType) {



            // 基础校验：状态值仅允许0/1，避免非法参数
            if (targetType != 0 && targetType != 1) {
                return CommunityUtil.getJSONString(1, "参数错误：置顶状态仅支持0/1");
            }

            discussPostService.updateType(id, targetType);

            Event event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(id);
            eventProducer.fireEvent(event);

            return CommunityUtil.getJSONString(0, targetType == 1 ? "置顶成功" : "取消置顶成功");
        }

        @RequestMapping(path="/wonderful", method = RequestMethod.POST)
        @ResponseBody
        public String setWonderful(@RequestParam("postId") int id,
                                   @RequestParam("status") int targetStatus) {
            discussPostService.updateStatus(id, targetStatus);
            Event event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(id);
            eventProducer.fireEvent(event);

            String redisKey = RedisUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, id);

            return CommunityUtil.getJSONString(0);
        }

        @RequestMapping(path="/delete", method = RequestMethod.POST)
        @ResponseBody
        public String deleteOrRecoverPost(@RequestParam("postId") int id,
                                          @RequestParam("status") int targetStatus) {
            // 基础校验：删除/恢复状态仅允许0/2（0=正常/恢复，2=删除）
            if (targetStatus != 0 && targetStatus != 2) {
                return CommunityUtil.getJSONString(1, "参数错误：帖子状态仅支持0/2");
            }
            // 更新帖子状态：status=2删除，status=0恢复
            discussPostService.updateStatus(id, targetStatus);
            if(targetStatus == 2){
                Event event = new Event()
                        .setTopic(TOPIC_DELETE)
                        .setUserId(hostHolder.getUser().getId())
                        .setEntityType(ENTITY_TYPE_POST)
                        .setEntityId(id);
                eventProducer.fireEvent(event);
            }
            return CommunityUtil.getJSONString(0, targetStatus == 2 ? "删除帖子成功" : "恢复帖子成功");
    }

}
