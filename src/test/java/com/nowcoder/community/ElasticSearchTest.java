package com.nowcoder.community;

import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticSearchTest {
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private DiscussPostService discussPostService;


//    @Test
//    public void testSave(){
//        discussPostRepository.save(discussPostMapper.selectDiscussPosts(101,0,1).getFirst());
//    }
//
//
//    @Test
//    public void init(){
//        List<DiscussPost> posts = discussPostService.findDiscussPosts(0, 0, Integer.MAX_VALUE);
//
//        for(DiscussPost post : posts){
//            discussPostRepository.save(post);
//        }
//
//    }

}
