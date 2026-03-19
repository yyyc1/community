package com.nowcoder.community.service;

import com.nowcoder.community.dao.CommentMapper;
import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.SensitiveFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;


@Service
public class CommentService implements CommunityConst {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private MeterRegistry meterRegistry;



    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit){
        try {
            List<Comment> res = commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
            countCommentOperate("list_query",RESULT_SUCCESS);
            return res;
        }catch (Exception e){
            countCommentOperate("list_query",RESULT_FAIL);
            throw e;
        }

    }

    public int findCommentCount(int entityType, int entityId){
        return commentMapper.selectCommentCount(entityType,entityId);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        try {
            if(comment == null){
                throw new IllegalArgumentException("参数不能为空");
            }

            comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
            comment.setContent(sensitiveFilter.filter(comment.getContent()));

            int rows = commentMapper.insertComment(comment);

            if (comment.getEntityType() == ENTITY_TYPE_POST){
                int count = commentMapper.selectCommentCount(comment.getEntityType(), comment.getEntityId());
                discussPostService.updateCommentCount(comment.getEntityId(), count);
            }
            countCommentOperate("add",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countCommentOperate("add",RESULT_FAIL);
            throw e;
        }

    }

    public Comment findCommentById(int id){
        return commentMapper.selectCommentById(id);
    }


    public List<Comment> findCommentsByUserId(int userId, int entityType, int limit, int offset) {
        try {
            List<Comment> res = commentMapper.selectCommentsByUserId(userId, entityType, limit, offset);
            countCommentOperate("user_list_query", RESULT_SUCCESS);
            return res;
        }catch (Exception e){
            countCommentOperate("user_list_query",RESULT_FAIL);
            throw e;
        }

    }

    public int findCommentCountByUserId(int userId, int entityType) {
        return commentMapper.selectCommentCountByUserId(userId, entityType);
    }

    private void countCommentOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.comment.operate.total",
                "module", "comment",
                "operate", operateType,
                "result", result
        ).increment();
    }
}
