package gw.vark.test;

import junit.framework.TestCase;

/**
 */
public class HelloWorldTest extends TestCase {
  public void testHelloWorld() {
    HelloWorld hw = new HelloWorld();
    assertEquals("Hello World", hw.toString());
  }
}
