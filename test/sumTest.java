package test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class sumTest {
//	@Test annotation specifies that method is the test method.
//
//	@Test(timeout=1000) annotation specifies that method will be failed if it takes longer than 1000 milliseconds (1 second).
//
//	@BeforeClass annotation specifies that method will be invoked only once, before starting all the tests.
//
//
//	@Before annotation specifies that method will be invoked before each test.
//
//	@After annotation specifies that method will be invoked after each test.
//
//	@AfterClass annotation specifies that method will be invoked only once, after finishing all the tests.

	
	@Test
	void test() throws InterruptedException {
		sum sum=new sum();
		assertEquals(sum.sum(10,20),30);
	} 
	@Test
	@Timeout(value=11 , unit=TimeUnit.SECONDS)
	void test1() throws InterruptedException {
		sum sum=new sum();
		assertEquals(sum.sum(50,6),56);
	}
	@Test
	@Before
	void test2() throws InterruptedException {
		sum sum=new sum();
		assertEquals(sum.sum(10,20),55);
	} 
	@Test
	@After
	void test3() {
		sum sum=new sum();
		try {
			assertEquals(sum.sum(10,20),55);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} 
	@Test
	@BeforeClass
	void test4() throws InterruptedException {
		sum sum=new sum();
		assertEquals(sum.sum(10,20),55);
	} 
	@Test
	@AfterClass
	void test5() throws InterruptedException {
		sum sum=new sum();
		assertEquals(sum.sum(10,20),55);
	} 

}

//op(pattern):
//	before class
//    before
//    test case find max
//    after
//    before
//    test case cube
//    after
//    before
//    test case reverse word
//    after
//    after class
