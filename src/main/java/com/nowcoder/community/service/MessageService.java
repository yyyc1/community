package com.nowcoder.community.service;


import com.nowcoder.community.dao.MessageMapper;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.util.CommunityConst;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService implements CommunityConst {


    @Autowired
    MessageMapper messageMapper;

    @Autowired
    MeterRegistry meterRegistry;

    public int addMessage(Message message){

        try {
            int rows = messageMapper.insertMessage(message);
            countMessageOperate("add",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countMessageOperate("add",RESULT_FAIL);
            throw e;
        }
    }


    public int findUnreadLetterCountByUserId(int userId) {
        try {
            int rows = messageMapper.selectUnreadLetterCountByUserId(userId, null);
            countMessageOperate("find_unread_letter_count",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countMessageOperate("find_unread_letter_count",RESULT_FAIL);
            throw e;
        }
    }

    public int findUnreadNoticeCountByUserId(int userId) {
        try {
            int rows = messageMapper.selectUnreadNoticeCountByUserId(userId, null);
            countMessageOperate("find_unread_notice_count",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countMessageOperate("find_unread_notice_count",RESULT_FAIL);
            throw e;
        }
    }

    public int findConversationCountByUserId(int userId) {
        return messageMapper.selectConversationCountByUserId(userId);
    }

    public List<Message> findConversations(int userId, int offset, int limit) {
        try {
            List<Message> res = messageMapper.selectConversations(userId,offset,limit);
            countMessageOperate("find_conversations",RESULT_SUCCESS);
            return res;
        }catch (Exception e){
            countMessageOperate("find_conversations",RESULT_FAIL);
            throw e;
        }

    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(String conversationId, int userId){
        return messageMapper.selectLetterUnreadCount(conversationId, userId);
    }

    public int findNoticeUnreadCount(String topic, int userId) {
        return messageMapper.selectNoticeUnreadCount(topic, userId);
    }

    public List<Message> findLetters(String conversationId, int latestLetterId, int pageSize) {
        try {
            List<Message> res = messageMapper.selectLetters(conversationId,latestLetterId,pageSize);
            countMessageOperate("find_letters",RESULT_SUCCESS);
            return res;
        }catch (Exception e){
            countMessageOperate("find_letters",RESULT_FAIL);
            throw e;
        }

    }

    public int readMessage(List<Integer> ids) {
        try {
            int rows = messageMapper.updateStatus(ids,1);
            countMessageOperate("read",RESULT_SUCCESS);
            return rows;
        }catch (Exception e){
            countMessageOperate("read",RESULT_FAIL);
            throw e;
        }

    }

    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    public int findNoticeCount(String topic, int userId) {
        return messageMapper.selectNoticeCount(topic, userId);
    }

    public List<Message> findNoticeByUserId(int userId, String topic, int limit, int offset) {
        return messageMapper.selectNoticeByUserId(userId, topic, limit, offset);
    }

    private void countMessageOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.message.operate.total",
                "module", "message",
                "tag", operateType,
                "result", result
        ).increment();
    }
}
