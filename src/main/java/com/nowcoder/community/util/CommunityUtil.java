package com.nowcoder.community.util;


import com.alibaba.fastjson.JSONObject;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateRandomUUID() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }



    public static String generateVerifyCode() {
        Random random = new Random();
        int code = random.nextInt(1000000);
        return String.format("%06d", code);
    }


    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code",code);
        json.put("msg",msg);
        if(map != null){
            for(String key : map.keySet()){
                json.put(key,map.get(key));
            }
        }
        return json.toJSONString();

    }


    public static String getJSONString(int code, String msg) {

        return getJSONString(code, msg, null);

    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);

    }

}
