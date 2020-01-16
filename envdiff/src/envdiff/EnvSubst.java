package envdiff;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

public class EnvSubst {
	
	@Parameter(names = "-s", required = true, description = "The source env-variables file to envsubst the template file")
	String source;
	
	@Parameter(names = "-t", required = true, description = "The template file the variables from source will be set on")
	String target;
	
	@Parameter(names = "-f", required = true, description = "The file to write the replaced template to")
	String outfile;
	
	@Parameter(names = "-i", required = false, description = "Env-variable not to replace (i.e. leave as-is) in template")
	List<String> ignore = new ArrayList<>();
	
	@Parameter(names = "-a", description = "the value in a template key=value string to perform an ask-user operation on")
	String askForValue = "ask-user";
	
	public static void main(String... argv){
		EnvSubst main = new EnvSubst();
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
		
		Map<String, Entry> sourceMap = parseFile(source);
		Map<String, Entry> targetMap = parseFile(target);
		
		for (Map.Entry<String, Entry> entry : sourceMap.entrySet()) {
			if (targetMap.containsKey(entry.getKey()) && !ignore.contains(entry.getKey())) {
				// replace existing from source into target map
				targetMap.put(entry.getKey(), entry.getValue());
			}
		}
		
		if (askForValue != null) {
			try (Scanner cliScanner = new Scanner(System.in)) {
				
				for (Map.Entry<String, Entry> entry : targetMap.entrySet()) {
					if (askForValue.equalsIgnoreCase(entry.getValue().getValue())) {
						if (entry.getValue().getDocumentation() != null) {
							System.out.println(entry.getValue().getDocumentation());
						}
						System.out.print(entry.getKey() + ": ");
						System.out.flush();
						String value = cliScanner.nextLine();
						targetMap.put(entry.getKey(), new Entry(value, null));
					}
				}
			}
		}
		
		String replaced = outputReplaced(target, targetMap);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
			writer.write(replaced);
		}
		System.out.println("Written " + outfile);
		
	}
	
	private String outputReplaced(String file, Map<String, Entry> outputMap) throws IOException{
		File sourceFile = new File(file);
		List<String> allLines = Files.readAllLines(sourceFile.toPath());
		StringBuilder sb = new StringBuilder();
		for (String string : allLines) {
			if (!string.startsWith("#") && string.length() > 0) {
				String[] split = new String[2];
				int firstEquals = string.indexOf('=');
				split[0] = string.substring(0, firstEquals);
				split[1] = string.substring(firstEquals + 1);
				if (split.length == 2) {
					sb.append(split[0] + "=" + outputMap.get(split[0]).getValue() + "\n");
				} else {
					System.err.println("[ERR] (" + file + ") " + string);
				}
			} else {
				sb.append(string + "\n");
			}
		}
		return sb.toString();
	}
	
	private Map<String, Entry> parseFile(String file) throws IOException{
		File sourceFile = new File(file);
		List<String> allLines = Files.readAllLines(sourceFile.toPath());
		Map<String, Entry> ret = new HashMap<>();
		
		StringBuilder doc = new StringBuilder();
		for (String string : allLines) {
			
			if (string.isEmpty() || string.startsWith("##")) {
				doc = new StringBuilder();
				continue;
			}
			if (string.startsWith("# ")) {
				doc.append(string + "\n");
			} else {
				String[] split = new String[2];
				int firstEquals = string.indexOf('=');
				split[0] = string.substring(0, firstEquals);
				split[1] = string.substring(firstEquals + 1);
				if (split.length == 2) {
					String _doc = doc.toString();
					_doc = (_doc.length() >= 1) ? _doc.substring(0, _doc.length() - 1) : "";
					ret.put(split[0].toUpperCase(), new Entry(split[1], _doc));
					doc = new StringBuilder();
				} else {
					System.out.println("[ERR] (" + file + ") " + string);
				}
			}
		}
		return ret;
	}
	
	private class Entry {
		
		String value;
		String documentation;
		
		public Entry(String value, String documentation){
			this.value = value;
			this.documentation = documentation;
		}
		
		public String getValue(){
			return value;
		}
		
		public String getDocumentation(){
			return documentation;
		}
	}
	
}
