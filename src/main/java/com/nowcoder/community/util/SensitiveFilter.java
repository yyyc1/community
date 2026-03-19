package com.nowcoder.community.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


@Component
public class SensitiveFilter {


    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private static final String REPLACEMENT = "***";
    private Node root = new Node();



    private static class Node{
        private boolean isEnd = false;
        private Map<Character, Node> subNodes = new HashMap<>();

        public boolean getEnd() {
            return isEnd;
        }

        public void setEnd(boolean end) {
            isEnd = end;
        }

        public void addSubNodes(char c, Node node){
            subNodes.put(c, node);
        }

        public Node getSubNodes(char c){
            return subNodes.getOrDefault(c,null);
        }
    }


    @PostConstruct
    public void init(){
        try (
            InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
        ){
            String keyWord;
            while((keyWord = br.readLine()) != null){
                this.addKeyWord(keyWord);
            }
        }catch (IOException e){
            logger.error("加载敏感词文件失败");
        }
    }


    public void addKeyWord(String keyWord){
        Node tempNode = root;
        for(int i = 0;i < keyWord.length();i++){
            Node subNode = tempNode.getSubNodes(keyWord.charAt(i));
            if(subNode == null) {
                subNode = new Node();
                tempNode.addSubNodes(keyWord.charAt(i), subNode);
            }
            tempNode = subNode;
            if(i == keyWord.length() - 1) tempNode.setEnd(true);
        }
    }

    public String filter(String s){
        if(StringUtils.isBlank(s))  return null;
        Node tempNode = root;
        int begin = 0;

        int position = 0;

        StringBuilder sb = new StringBuilder();

        while(position < s.length()){
            char c = s.charAt(position);
            if(isSymbol(c)){
                if(tempNode == root){
                    sb.append(c);
                    begin++;
                }
                position++;
                continue;
            }
            tempNode = tempNode.getSubNodes(c);
            if(tempNode == null){
                sb.append(s.charAt(begin));
                begin++;
                position = begin;
                tempNode = root;
            }else if(tempNode.getEnd()){
                sb.append(REPLACEMENT);
                position++;
                begin = position;
            }else{
                position++;
            }
        }
        return sb.toString();
    }


    public boolean isSymbol(char c){
        return !(CharUtils.isAsciiAlphanumeric(c) || (c >= 0x2e80 && c <= 0x9fff));
    }





}
