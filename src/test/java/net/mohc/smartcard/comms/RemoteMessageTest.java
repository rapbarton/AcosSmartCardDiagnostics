package net.mohc.smartcard.comms;

import static org.junit.Assert.*;

import org.junit.Test;

public class RemoteMessageTest {

	@Test
	public void createsAFormattedMessage() {
		RemoteMessage uut = new RemoteMessage();
		uut.setMessage("Abc");
		String formattedMessage = uut.getFormattedMessage();
		assertEquals("<TTRM>3:1b10:Abc</TTRM>",formattedMessage);
	}	
	
	@Test
	public void canRemoveHeader() {
		RemoteMessage uut = new RemoteMessage();
		String result = uut.removeHeader("3:1b10:Abc");
		assertEquals("Abc", result);
	}	
	
	@Test
	public void canDetectInvalidChecksumInHeader() {
		RemoteMessage uut = new RemoteMessage();
		String result = uut.removeHeader("3:1b11:Abc");
		assertNull(result);
		assertEquals("message crc failure", uut.getLastError());
	}	
	
	@Test
	public void canDetectBadMessageLengthInHeader() {
		RemoteMessage uut = new RemoteMessage();
		String result = uut.removeHeader("2:1b10:Abc");
		assertNull(result);
		assertEquals("message length incorrect", uut.getLastError());
	}	
	
	@Test
  public void scanDetectsMessageAndStripsFromBuffer () {
		RemoteMessage uut = new RemoteMessage();
		uut.setMessage("Abc");
		String formattedMessage = uut.getFormattedMessage();
		StringBuffer inputBuffer = new StringBuffer(formattedMessage);
		String result = uut.scan(inputBuffer);
		assertEquals("Abc", result);
		assertEquals("", inputBuffer.toString());
	}
	@Test

	public void scanDetectsMessageAndStripsFromBufferIgnoringPreceedingChars () {
		RemoteMessage uut = new RemoteMessage();
		uut.setMessage("Abc");
		String formattedMessage = uut.getFormattedMessage();
		StringBuffer inputBuffer = new StringBuffer("xxx"+formattedMessage+"yyy");
		String result = uut.scan(inputBuffer);
		assertEquals("Abc", result);
		assertEquals("yyy", inputBuffer.toString());
	}
	
	@Test
  public void scanDetectsMessageAndStripsFromBufferOneMessageAtATime () {
		RemoteMessage uut = new RemoteMessage();
		uut.setMessage("Abc");
		String formattedMessage1 = uut.getFormattedMessage();
		uut.setMessage("Def");
		String formattedMessage2 = uut.getFormattedMessage();
		uut.setMessage("Ghijk");
		String formattedMessage3 = uut.getFormattedMessage();
		StringBuffer inputBuffer = new StringBuffer();
		inputBuffer.append("xxx");
		inputBuffer.append(formattedMessage1);
		inputBuffer.append(formattedMessage2);
		inputBuffer.append(formattedMessage3);
		String result = uut.scan(inputBuffer);
		assertEquals("Abc", result);
		assertEquals(formattedMessage2+formattedMessage3, inputBuffer.toString());
		result = uut.scan(inputBuffer);
		assertEquals("Def", result);
		assertEquals(formattedMessage3, inputBuffer.toString());
		result = uut.scan(inputBuffer);
		assertEquals("Ghijk", result);
		assertEquals("", inputBuffer.toString());
	}
	
	@Test
  public void scanDetectsMessageAndStripsFromBufferOneMessageAtATimeIgnoringBadMessages () {
		RemoteMessage uut = new RemoteMessage();
		uut.setMessage("Abc");
		String formattedMessage1 = uut.getFormattedMessage();
		uut.setMessage("Bad message");
		String formattedMessage2 = uut.getFormattedMessage();
		String formattedMessage2Corrupted = formattedMessage2.replaceAll("Bad message", "B@d message");
		uut.setMessage("Ghijk");
		String formattedMessage3 = uut.getFormattedMessage();
		StringBuffer inputBuffer = new StringBuffer();
		inputBuffer.append("xxx");
		inputBuffer.append(formattedMessage1);
		inputBuffer.append(formattedMessage2Corrupted);
		inputBuffer.append(formattedMessage3);
		String result = uut.scan(inputBuffer);
		assertEquals("Abc", result);
		assertEquals(formattedMessage2Corrupted+formattedMessage3, inputBuffer.toString());
		result = uut.scan(inputBuffer);
		assertNull(result);
		assertEquals("message crc failure", uut.getLastError());
		assertEquals(formattedMessage3, inputBuffer.toString());
		result = uut.scan(inputBuffer);
		assertEquals("Ghijk", result);
		assertEquals("", inputBuffer.toString());
	}
	
	
	
}
