package com.ysx.folimall.search;

import com.alibaba.fastjson.JSON;
import com.ysx.folimall.search.config.FolimallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.annotation.security.RunAs;
import javax.naming.directory.SearchResult;
import java.io.IOException;

@SpringBootTest
class FolimallSearchApplicationTests {


    @Autowired
	private RestHighLevelClient client;

//    @Autowired
//	StringRedisTemplate
	@Test
	public void contextLoads() {
		String s ="2341_oljk";
		String[] s1 = s.split("_");
		for (String s2 : s1) {
			System.out.println(s2);
		}
	}


	@Test
	public void indexTest() throws IOException {
		IndexRequest request = new IndexRequest("users");

		request.id("1");
//		request.source("userName","zhangsan","age",18);

		User user = new User();
		user.setAge(123);
		user.setGender("male");
		user.setUserName("marco");
		String s = JSON.toJSONString(user);
		request.source(s, XContentType.JSON);

		IndexResponse index = client.index(request, FolimallElasticSearchConfig.COMMON_OPTIONS);


		System.out.println(index);
	}



	@Test
	public void searchData() throws IOException{
		SearchRequest  searchRequest = new SearchRequest();
		//指定索引
		searchRequest.indices("bank");
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(QueryBuilders.matchQuery("address","mill"));
		searchRequest.source(searchSourceBuilder);


		//aggAgg
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		searchSourceBuilder.aggregation(ageAgg);
		//balanceAgg

		AvgAggregationBuilder balanceAgg = AggregationBuilders.avg("balanceAgg").field("balance");
		searchSourceBuilder.aggregation(balanceAgg)	;
		System.out.println(searchSourceBuilder.toString());
		SearchResponse search = client.search(searchRequest, FolimallElasticSearchConfig.COMMON_OPTIONS);

		SearchHits hits = search.getHits();

		for(SearchHit hit: hits){
			String string  = hit.getSourceAsString();
			Account account  = JSON.parseObject(string,Account.class);
			System.out.println("account"+account);
		}
		System.out.println(search.toString());


		Aggregations aggregations = search.getAggregations();

//		for (Aggregation aggregation : aggregations.asList()) {
//			System.out.println(aggregation.getName());
//
//		}

		Terms ageAgg1 = aggregations.get("ageAgg");
		for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
			System.out.println("age: " + bucket.getKeyAsString());
		}

		Avg balanceAge1 = aggregations.get("balanceAgg");
		System.out.println("average balance "+ balanceAge1.getValue());
	}
	@Data
	class User{
		private String UserName;
		private String gender;
		private Integer age;
	}
	@ToString
	@Data
	static class Account {

		private int account_number;
		private int balance;
		private String firstname;
		private String lastname;
		private int age;
		private String gender;
		private String address;
		private String employer;
		private String email;
		private String city;
		private String state;
	}

}
