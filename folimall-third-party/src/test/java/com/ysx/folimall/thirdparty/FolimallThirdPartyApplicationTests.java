package com.ysx.folimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class FolimallThirdPartyApplicationTests {

	@Test
	void contextLoads() {
	}

	@Autowired
	OSSClient ossClient;
	@Test
	public void testUpload() throws FileNotFoundException {
		InputStream inputStream = new FileInputStream("/home/ysx/Documents/Picture1.png");
		ossClient.putObject("folimall","dialogg.jpg",inputStream);

		ossClient.shutdown();
		System.out.println("ok!!!!");
	}
}
