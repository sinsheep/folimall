package com.ysx.folimall.coupon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FolimallCouponApplicationTests {

	@Test
	void contextLoads() {
	}

	void solveProblem(){
		String s = "12321";
		char arr[] = s.toCharArray();
		int  t = 0, q=1, n = arr.length;
		for(int i = n - 1; i >= 0; i--){
			t = (arr[i] - 'A'  +1) * q;
			q*=26;
		}
	}

}
