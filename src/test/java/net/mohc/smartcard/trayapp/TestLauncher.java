package net.mohc.smartcard.trayapp;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestLauncher {
	
	@Test
	public void testGetTrayCommand() {
		SmartCardApplicationLauncher uut = new SmartCardApplicationLauncher("UUT");
		String command = uut.getStartTrayCommand(false);
		// E.g. "C:\\OPMS_J7\\Java7_32bit\\jre\\bin\\java -classpath C:\\Users\\rob\\workspace\\AcosSmartCardDiagnostics\\smartcard.jar;C:\\Users\\rob\\workspace\\AcosSmartCardDiagnostics\\jackson-core.jar;C:\\Users\\rob\\workspace\\AcosSmartCardDiagnostics\\jackson-databind.jar;C:\\Users\\rob\\workspace\\AcosSmartCardDiagnostics\\jackson-annotations.jar;C:\\Users\\rob\\workspace\\AcosSmartCardDiagnostics\\log4j.jar -Ddisable.error.popup=true  net.mohc.smartcard.trayapp.SmartCardApplication";
		assertTrue(command.contains("-Ddisable.error.popup=true  net.mohc.smartcard.trayapp.SmartCardApplication"));
		assertTrue(command.contains(" -classpath "));
		assertTrue(command.contains("smartcard.jar"));
	
	}


}
