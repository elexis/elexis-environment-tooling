package at.medevit.ee.selenium.tester;

import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class SysOutExecutionListener implements TestExecutionListener {
	
	@Override
	public void executionStarted(TestIdentifier testIdentifier){
		Type type = testIdentifier.getType();
		if (Type.TEST == type) {
			System.out.printf(testIdentifier.getDisplayName() + " ......");
		}
		
	}
	
	@Override
	public void executionFinished(TestIdentifier testIdentifier,
		TestExecutionResult testExecutionResult){
		Type type = testIdentifier.getType();
		if (Type.TEST == type) {
			System.out.printf(testExecutionResult.toString());
		}
	}
	
	
}
