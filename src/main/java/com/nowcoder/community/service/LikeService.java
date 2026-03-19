package com.nowcoder.community.service;

import com.nowcoder.community.util.CommunityConst;
import com.nowcoder.community.util.RedisUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService implements CommunityConst {


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MeterRegistry meterRegistry;



    public long findEntityLikeCount(int entityType, int entityId){
        try {
            String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
            countFollowOperate("find_like_count",RESULT_SUCCESS);
            return redisTemplate.opsForSet().size(entityLikeKey);
        }catch (Exception e){
            countFollowOperate("find_like_count",RESULT_FAIL);
            throw e;
        }

    }

    public int findLikeStatus(int userId, int entityType, int entityId){
        try {
            String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
            countFollowOperate("find_like_status",RESULT_SUCCESS);
            return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
        }catch (Exception e){
            countFollowOperate("find_like_status",RESULT_FAIL);
            throw e;
        }

    }

    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count;
    }

    public void like(int userId, int entityType, int entityId, int entityUserId) {
        try {
            redisTemplate.execute(new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    String entityLikeKey = RedisUtil.getEntityLikeKey(entityType, entityId);
                    String userLikeKey = RedisUtil.getUserLikeKey(entityUserId);

                    boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                    operations.multi();

                    if (isMember){
                        operations.opsForSet().remove(entityLikeKey, userId);
                        operations.opsForValue().decrement(userLikeKey);
                    }else {
                        operations.opsForSet().add(entityLikeKey, userId);
                        operations.opsForValue().increment(userLikeKey);
                    }
                    countFollowOperate("like",RESULT_SUCCESS);
                    return operations.exec();
                }
            });
        }catch (Exception e){
            countFollowOperate("like",RESULT_FAIL);
            throw e;
        }

    }

    private void countFollowOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.like.operate.total",
                "module", "like",
                "tag", operateType,
                "result", result
        ).increment();
    }
}
