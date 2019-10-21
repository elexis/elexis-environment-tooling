package at.medevit.ee.keycloak.util.runnable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class SyncUsersFromElexisCsvToKeycloak {
	
	private String inputFileName;
	private RealmResource elexisEnvironmentRealm;
	private boolean verbose;
	
	public SyncUsersFromElexisCsvToKeycloak(String inputFileName,
		RealmResource elexisEnvironmentRealm, boolean verbose){
		this.inputFileName = inputFileName;
		this.elexisEnvironmentRealm = elexisEnvironmentRealm;
		this.verbose = verbose;
	}
	
	public void run() throws IOException{
		if (inputFileName == null) {
			throw new IOException("inputFileName is null");
		}
		File csvFile = new File(inputFileName);
		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator('|').withHeader();
		MappingIterator<Map<String, String>> it =
			mapper.readerFor(Map.class).with(schema).readValues(csvFile);
		while (it.hasNext()) {
			Map<String, String> map = it.next();
			
			String userId = map.get("ID");
			String familyName = map.get("BEZEICHNUNG1");
			String name = map.get("BEZEICHNUNG2");
			String email = map.get("EMAIL");
			String elexisContactId = map.get("KONTAKT_ID");
			
			List<UserRepresentation> found = elexisEnvironmentRealm.users().search(userId);
			if (found.isEmpty()) {
				
				CredentialRepresentation credential = new CredentialRepresentation();
				credential.setType(CredentialRepresentation.PASSWORD);
				credential.setValue("changeme");
				
				UserRepresentation userRepresentation = new UserRepresentation();
				userRepresentation.setUsername(userId);
				userRepresentation.setFirstName(name);
				userRepresentation.setLastName(familyName);
				userRepresentation.setEmail(email);
				userRepresentation.singleAttribute("elexisContactId", elexisContactId);
				userRepresentation.setEnabled(Boolean.TRUE);
				userRepresentation.setCredentials(Arrays.asList(credential));
				userRepresentation.getRequiredActions().add("UPDATE_PASSWORD");
				
				Response response = elexisEnvironmentRealm.users().create(userRepresentation);
				if (response.getStatus() != 201) {
					System.err.println("Couldn't create user.");
					System.exit(0);
				}
				String newId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
				if (verbose) {
					System.out.printf("Created user %s with userId %s%n, password is [changeme]",
						userId, newId);
				}
				
			} else {
				UserRepresentation userRepresentation = found.get(0);
				userRepresentation.singleAttribute("elexisContactId", elexisContactId);
				
				UserResource userResource =
					elexisEnvironmentRealm.users().get(userRepresentation.getId());
				userRepresentation.singleAttribute("elexisContactId", elexisContactId);
				userResource.update(userRepresentation);
				if (verbose) {
					System.out.printf("Updated user %s with userId %s%n", userId,
						userRepresentation.getId());
				}
				
			}
			
		}
		
	}
	
}
