package at.medevit.ee.keycloak.util;

import java.io.IOException;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.ServerInfoResource;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import at.medevit.ee.keycloak.util.runnable.SyncUsersFromElexisCsvToKeycloak;

public class KeycloakUtil {
	
	@Parameter(names = "-u", required = true, description = "Keycloak service url, e.g. https://server/keycloak")
	String serviceUrl;
	
	@Parameter(names = "-l", required = true, description = "username")
	String user;
	
	@Parameter(names = "-p", required = true, description = "password")
	String password;
	
	@Parameter(names = "-c", required = true, description = "command to execute")
	String command;
	
	@Parameter(names = "-f", description = "input file name, if required for command")
	String inputFileName;
	
	@Parameter(names = "-v", description = "Verbose output")
	boolean verbose = false;
	
	@Parameter(names = "-t", description = "Trust all HTTPS certificates")
	boolean trustSelfSignedCertificate = false;
	
	private static Keycloak keycloak;
	private RealmResource elexisEnvironmentRealm;
	
	public static void main(String[] argv) throws IOException{
		KeycloakUtil main = new KeycloakUtil();
		JCommander commander = JCommander.newBuilder().addObject(main).build();
		try {
			commander.parse(argv);
			main.run();
		} catch (ParameterException e) {
			System.out.println(e.getMessage() + "\n");
			commander.usage();
		} finally {
			if (keycloak != null) {
				keycloak.close();
			}
		}
	}
	
	private void run() throws IOException{
		
		initiateKeycloakConnection();
		
		switch (command) {
		case "syncUsersFromElexisCsv":
			new SyncUsersFromElexisCsvToKeycloak(inputFileName, keycloak, verbose).run();
			break;
		default:
			System.out.printf("Command %s not found.%n", command);
			break;
		}
		
	}
	
	private void initiateKeycloakConnection(){
		
		ResteasyClientBuilder builder = (ResteasyClientBuilder) ResteasyClientBuilder.newBuilder();
		if (trustSelfSignedCertificate) {
			builder.setIsTrustSelfSignedCertificates(true);
			builder.disableTrustManager();
		}
		
		keycloak =
			KeycloakBuilder.builder().serverUrl(serviceUrl).realm("master").clientId("admin-cli")
				.username(user).resteasyClient(builder.build()).password(password).build();
		
		ServerInfoResource serverInfo = keycloak.serverInfo();
		if (verbose) {
			System.out.println("Connected to " + serviceUrl + ", Keycloak v"
				+ serverInfo.getInfo().getSystemInfo().getVersion());
		}
		
		elexisEnvironmentRealm = keycloak.realm("ElexisEnvironment");
	}
	
}
