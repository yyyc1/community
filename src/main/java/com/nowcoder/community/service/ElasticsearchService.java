package com.nowcoder.community.service;


import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.util.CommunityConst;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ElasticsearchService implements CommunityConst {


    @Autowired
    DiscussPostRepository discussPostRepository;

    @Resource
    ElasticsearchOperations elasticsearchOperations;

    @Autowired
    MeterRegistry meterRegistry;





    public void saveDiscussPost(DiscussPost discussPost){

        try {
            countElasticsearchOperate("save",RESULT_SUCCESS);
            discussPostRepository.save(discussPost);
        }catch (Exception e){
            countElasticsearchOperate("save",RESULT_FAIL);
            throw e;
        }
    }


    public void deleteDiscussPost(int id){
        try {
            countElasticsearchOperate("delete",RESULT_SUCCESS);
            discussPostRepository.deleteById(id);
        }catch (Exception e){
            countElasticsearchOperate("delete",RESULT_FAIL);
            throw e;
        }

    }


    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        try {
            //page
            Pageable pageable = PageRequest.of(current, limit);

            //高亮
            HighlightField highlightTitle = new HighlightField("title");
            HighlightField highlightContent = new HighlightField("content");
            HighlightParameters parameters = HighlightParameters.builder()
                    .withPreTags("<em>")
                    .withPostTags("</em>").build();
            Highlight highlight = new Highlight(parameters, List.of(highlightTitle, highlightContent));
            HighlightQuery highlightQuery = new HighlightQuery(highlight, DiscussPost.class);

            //排序
            Sort sort = Sort.by(
                    Sort.Order.desc("type"),
                    Sort.Order.desc("score"),
                    Sort.Order.desc("createTime")
            );

            Criteria criteria = new Criteria("title").matches(keyword).or(new Criteria("content").matches(keyword));

            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

            criteriaQuery.setSort(sort);
            criteriaQuery.setPageable(pageable);
            criteriaQuery.setHighlightQuery(highlightQuery);

            SearchHits<DiscussPost> searchHits = elasticsearchOperations.search(criteriaQuery, DiscussPost.class);

            List<DiscussPost> discussPosts = searchHits.stream().map(hit -> {
                DiscussPost post = hit.getContent();
                Optional.ofNullable(hit.getHighlightFields().get("title"))
                        .ifPresent(highlightFields -> post.setTitle(highlightFields.get(0)));
                // 替换内容：如果有高亮结果，用高亮的，否则用原内容
                Optional.ofNullable(hit.getHighlightFields().get("content"))
                        .ifPresent(highlightFields -> post.setContent(highlightFields.get(0)));
                return post;
            }).collect(Collectors.toList());

            // 5.2 封装Page对象：数据列表 + 分页信息（总条数、分页参数）
            long total = searchHits.getTotalHits();
            Page<DiscussPost> page = new PageImpl<>(discussPosts, pageable, total);
            countElasticsearchOperate("search",RESULT_SUCCESS);
            return page;
        }catch (Exception e){
            countElasticsearchOperate("search",RESULT_FAIL);
            throw e;
        }

    }


    private void countElasticsearchOperate(String operateType, String result) {
        meterRegistry.counter(
                "discuss.elasticsearch.operate.total",
                "module", "elasticsearch",
                "operate",operateType,
                "result", result
        ).increment();
    }
}
