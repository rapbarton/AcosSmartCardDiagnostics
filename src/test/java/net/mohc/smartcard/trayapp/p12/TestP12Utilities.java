package net.mohc.smartcard.trayapp.p12;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestP12Utilities {
	private static final String TEST_CERTIFICATE_FILENAME = "src/main/resources/cert/TestCert-cert.p12";
	private static final char[] CORRECT_PASSWORD = "12345678".toCharArray();
	private static final String TEST_DOCUMENT = "My test string!";
	private static final String CORRECT_SIGNATURE = 
			"p6mq9TAvEezyelGimOHQtJMUrGo+d81BRc2U/D7HbysGCT4z/cmrVNHFOTyyCBemdoQb1PMI0cO4\n" + 
			"t6vsQjNxiCrFkJ4S+SOlMjssg7Atz4HoO5CppfTUKVLxnSVnc9Kq12qXGQKVtiN75X48sK7jd3DP\n" + 
			"97FUSbifFg/pau3/w7M=";
	
	@Test
	public void testSignBadFile() {
		try {
			P12Utilities.sign("nosuchfile", CORRECT_PASSWORD, TEST_DOCUMENT);
			fail("Exception should be thrown");
		} catch (P12Exception e) {
		}
	}

	@Test
	public void testSignSuccess() {
		try {
			String signature = P12Utilities.sign(TEST_CERTIFICATE_FILENAME, CORRECT_PASSWORD, TEST_DOCUMENT);
			assertEquals(CORRECT_SIGNATURE, signature);
		} catch (P12Exception e) {
			fail("Exception was thrown: " + e.getMessage());
		}
	}

}
