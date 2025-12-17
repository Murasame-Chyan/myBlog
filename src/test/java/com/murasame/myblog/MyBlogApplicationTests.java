package com.murasame.myblog;

import com.murasame.domain.vo.BlogBriefVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class MyBlogApplicationTests {
	@Resource
	JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		List<Map<String, Object>> Brief = jdbcTemplate.queryForList("SELECT * FROM blogs LIMIT 5");
		for (Map<String, Object> map : Brief) {
			System.out.println(map);
		}
	}

}
