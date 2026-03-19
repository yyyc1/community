package com.nowcoder.community.service;


import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.SensitiveFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class DiscussPostService implements CommunityConst {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private MeterRegistry meterRegistry;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        try {
            List<DiscussPost> res = discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
            countPostOperate("list_query", RESULT_SUCCESS);
            return res;
        }catch (Exception e){
            countPostOperate("list_query",RESULT_FAIL);
            throw e;
        }
    }

    public int findDiscussPostsRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public DiscussPost findDiscussPostById(int postId){return discussPostMapper.findDiscussPostById(postId);}


    public int addDiscussPost(DiscussPost discussPost){

        try {
            if(discussPost == null){
                throw new IllegalArgumentException("参数不能为空");
            }

            discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
            discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

            discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
            discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
            int rows = discussPostMapper.insertDiscussPost(discussPost);
            countPostOperate("add",RESULT_SUCCESS);

            return rows;
        }catch (Exception e){
            countPostOperate("add",RESULT_FAIL);
            throw e;
        }
    }

    public int updateCommentCount(int postId, int count) {
        try {
            int rows = discussPostMapper.updateCommentCount(postId, count);
            countPostOperate("add_comment",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countPostOperate("add_comment",RESULT_FAIL);
            throw e;
        }

    }


    public int updateScore(int postId, double score) {
        try {
            int rows = discussPostMapper.updateScore(postId, score);
            countPostOperate("update_score",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countPostOperate("update_score",RESULT_FAIL);
            throw e;
        }

    }

    public int updateType(int id, int type) {
        try {
            int rows = discussPostMapper.updateType(id, type);
            countPostOperate("update_type",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countPostOperate("update_type",RESULT_FAIL);
            throw e;
        }

    }

    public int updateStatus(int id, int status) {
        try {
            int rows = discussPostMapper.updateStatus(id, status);
            countPostOperate("update_status",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countPostOperate("update_status",RESULT_FAIL);
            throw e;
        }

    }


    private void countPostOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.post.operate.total",
                "module", "discuss_post",
                "operate",operateType,
                "result", result
        ).increment();
    }
}
