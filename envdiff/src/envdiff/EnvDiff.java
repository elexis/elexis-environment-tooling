package envdiff;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class EnvDiff {
	
	@Parameter(names = "-s", required = true, description = "File that acts as match source for the required key/variable pairs")
	String source;
	
	@Parameter(names = "-t", required = true, description = "File that acts as match target for the required key/variable pairs")
	String target;
	
	@Parameter(names = "-d", description = "Perform a dry run, do not diff modify target, return status is 1 if changes found")
	boolean dryRun = false;
	
	@Parameter(names = "-r", description = "Do remove vars from target if not in source")
	boolean doRemove = false;
	
	public static void main(String... argv){
		EnvDiff main = new EnvDiff();
		JCommander commander = JCommander.newBuilder().addObject(main).build();
		try {
			commander.parse(argv);
			try {
				main.run();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} catch (ParameterException e) {
			System.out.println(e.getMessage() + "\n");
			commander.usage();
		}
	}
	
	private void run() throws IOException{
		Map<String, String> sourceMap = parseFile(source);
		Map<String, String> targetMap = parseFile(target);
		
		Set<String> addList = new HashSet<>();
		
		for (Map.Entry<String, String> entry : sourceMap.entrySet()) {
			// add missing
			if (!targetMap.containsKey(entry.getKey())) {
				addList.add(entry.getKey() + "=" + entry.getValue());
			}
		}
		
		if (doRemove) {
			System.out.println("doRemove not yet implemented");
		}
		
		if (!addList.isEmpty()) {
			for (String string : addList) {
				System.out.println("+ " + string);
			}
			
			if(dryRun) {
				System.exit(1);
			} else {
				System.out.println("only dry run implemented up to now");
			}
		}
		
	}
	
	private Map<String, String> parseFile(String file) throws IOException{
		File sourceFile = new File(file);
		List<String> allLines = Files.readAllLines(sourceFile.toPath());
		Map<String, String> ret = new HashMap<>();
		
		for (String string : allLines) {
			if (!string.startsWith("#") && string.length() > 0) {
				String[] split = new String[2];
				int firstEquals = string.indexOf('=');
				split[0] = string.substring(0, firstEquals);
				split[1] = string.substring(firstEquals + 1);
				if (split.length == 2) {
					ret.put(split[0].toUpperCase(), split[1]);
				} else {
					System.out.println("[ERR] (" + file + ") " + string);
				}
			}
		}
		return ret;
	}
	
}
