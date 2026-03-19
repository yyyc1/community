package com.nowcoder.community.quartz;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.ElasticsearchService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.RedisUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, CommunityConst {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    private static final Date epoch;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    static {

        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2008-01-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0){
            logger.info("[任务取消] 没有需要刷新的帖子");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数" + operations.size());
        while (operations.size() > 0){
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕");
    }

    private void refresh(int postId) {
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);

        if (discussPost == null){
            logger.error("该帖子不存在:id=" + postId);
            return;
        }

        boolean wonderful = discussPost.getStatus() == 1;

        int commentCount = discussPost.getCommentCount();

        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        double w = (wonderful ? 75 : 0) + commentCount * 10L + likeCount * 2;

        double score = Math.log10(Math.max(w, 1)) + (double) (discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);

        discussPostService.updateScore(postId, score);
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
    }
}
