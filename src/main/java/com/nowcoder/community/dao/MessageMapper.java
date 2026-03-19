package com.nowcoder.community.dao;

import com.nowcoder.community.entity.Message;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insertMessage(Message message);


    //查询未读私信数量
    int selectUnreadLetterCountByUserId(int userId, String conversationID);


    //查询未读通知数量
    int selectUnreadNoticeCountByUserId(int userId, String topic);

    int selectConversationCountByUserId(int userId);

    List<Message> selectConversations(int userId, int offset, int limit);

    int selectLetterCount(String conversationId);

    int selectLetterUnreadCount(String conversationId, int userId);

    int selectNoticeUnreadCount(String topic, int userId);

    List<Message> selectLetters(String conversationId, int latestLetterId, int pageSize);

    int updateStatus(List<Integer> ids, int status);

    Message selectLatestNotice(int userId, String topic);

    int selectNoticeCount(String topic, int userId);

    List<Message> selectNoticeByUserId(int userId, String topic, int limit, int offset);
}
