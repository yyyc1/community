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

import java.util.*;

@Service
public class FollowService implements CommunityConst {


    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private MeterRegistry meterRegistry;


    public void follow(int userId, int entityType, int entityId){
        try {
            redisTemplate.execute(new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    String followerKey = RedisUtil.getFollowerKey(entityType, entityId);
                    String followeeKey = RedisUtil.getFolloweeKey(entityType, userId);
                    operations.multi();
                    redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                    redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                    return operations.exec();
                }
            });
            countFollowOperate("follow",RESULT_SUCCESS);
        }catch (Exception e){
            countFollowOperate("follow",RESULT_FAIL);
            throw e;
        }



    }

    public void unfollow(int userId,int entityType,int entityId){
        try {
            redisTemplate.execute(new SessionCallback() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    String followerKey = RedisUtil.getFollowerKey(entityType, entityId);
                    redisTemplate.opsForZSet().remove(followerKey, userId);
                    String followeeKey = RedisUtil.getFolloweeKey(entityType, userId);
                    redisTemplate.opsForZSet().remove(followeeKey, entityId);
                    return operations.exec();
                }
            });
            countFollowOperate("unfollow",RESULT_SUCCESS);
        }catch (Exception e){
            countFollowOperate("unfollow",RESULT_SUCCESS);
            throw e;
        }

    }



    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisUtil.getFolloweeKey(entityType, userId);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    public long findFollowerCount(int userId, int entityType) {
        String followerKey = RedisUtil.getFollowerKey(entityType, userId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisUtil.getFolloweeKey(entityType, userId);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    public List<Map<String, Object>> findFollowees(int userId, int limit, int offset) {
        try {
            String followeeKey = RedisUtil.getFolloweeKey(ENTITY_TYPE_USER, userId);
            Set<Integer> resIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

            if (resIds == null) return null;

            List<Map<String, Object>> list = new ArrayList<>();
            for(Integer id : resIds){
                Map<String, Object> map = new HashMap<>();
                map.put("user", userService.findUserById(id));
                Double score = redisTemplate.opsForZSet().score(followeeKey, id);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            countFollowOperate("find_followees",RESULT_SUCCESS);
            return list;
        }catch (Exception e){
            countFollowOperate("find_followees",RESULT_FAIL);
            throw e;
        }

    }

    public List<Map<String, Object>> findFollowers(int userId, int limit, int offset) {
        try {
            String followerKey = RedisUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
            Set<Integer> resIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
            if(resIds == null) return null;

            List<Map<String, Object>> list = new ArrayList<>();
            for(Integer id : resIds){
                Map<String, Object> map = new HashMap<>();
                map.put("user", userService.findUserById(id));
                Double score = redisTemplate.opsForZSet().score(followerKey, id);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            countFollowOperate("find_followers",RESULT_SUCCESS);
            return list;
        }catch (Exception e){
            countFollowOperate("find_followers",RESULT_FAIL);
            throw e;
        }

    }

    private void countFollowOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.follow.operate.total",
                "module", "follow",
                "tag", operateType,
                "result", result
        ).increment();
    }
}
