package smartcard;

import java.io.File;
import java.util.Vector;

public class CommandLineDiagnoseApplication {

	public static void main(String[] args) {
		System.out.println("Smart card diagnoser");
		System.out.println("====================");
		String firstArg = (args.length >= 1)?args[0]:"";
		String secondArg = (args.length >= 2)?args[1]:"";
		
		if (firstArg.equalsIgnoreCase("list")) {
			printListOfDllsFound(secondArg);
			System.exit(0);
		} else if (firstArg.equalsIgnoreCase("gui")) {
			SmartCardFrame.launch();
		} else if (firstArg.isEmpty() || secondArg.isEmpty()) {
			showUsage();
			System.exit(0);
		} else {
			runDiagnosticsToCommandLine(firstArg, secondArg);
			System.exit(0);
		}
	}		

	static private void showUsage() {
		System.out.println("Usage:");
		System.out.println("");
		System.out.println("java -jar SmartCardTest.jar pkcs_dll_filepath card_pin_number");
		System.out.println("This attempts to find a smard card terminal, connect, find a smart card, find a certificate with public private key-pair and then sign a test string");
		System.out.println(" or");
		System.out.println("java -jar SmartCardTest.jar list [base location]");
		System.out.println("To list all likely dlls found");
		System.out.println(" or");
		System.out.println("java -jar SmartCardTest.jar gui");
		System.out.println("For interactive test");
	}
	
	static private void runDiagnosticsToCommandLine(String filename, String pin) {
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("Jave name: " + System.getProperty("java.vm.name"));
		System.out.println("OS name: " + System.getProperty("os.name"));
		String libFilePath = filename;
		File libraryFile = new File(libFilePath);
		SmartCardDiagnosticController controller = SmartCardDiagnosticController.getInstance(libraryFile, pin);
		controller.runDiagnostics();
	}
	
	
	private static void printListOfDllsFound(String baseLocation) {
		SmartCardController controller = SmartCardController.getInstance();
		Vector<File> list;
		if (baseLocation.isEmpty()) {
			list = controller.findDlls();
		} else {
			list = controller.findDlls(baseLocation);
		}
		for (File file : list) {
			System.out.println(file.getAbsolutePath());
		}
	}




}
