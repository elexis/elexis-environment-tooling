package at.medevit.ee.keycloak.util.runnable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvFactory;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser.Feature;

import at.medevit.ee.keycloak.util.ElexisEnvironment;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Import/Update the users from a csv file
 */
public class SyncUsersFromElexisCsvToKeycloak {
	
	private String inputFileName;
	private RealmResource elexisEnvironmentRealm;
	private boolean verbose;
	
	private Set<String> usedEmail;
	
	public SyncUsersFromElexisCsvToKeycloak(String inputFileName, Keycloak keycloak,
		boolean verbose){
		this.inputFileName = inputFileName;
		this.elexisEnvironmentRealm = keycloak.realm(ElexisEnvironment.REALM_ID);
		this.verbose = verbose;
		usedEmail = new HashSet<String>();
	}
	
	public void run() throws IOException{
		if (inputFileName == null) {
			throw new IOException("inputFileName is null");
		}
		File csvFile = new File(inputFileName);
		CsvFactory factory = CsvFactory.builder().configure(Feature.SKIP_EMPTY_LINES, true).build();
		CsvMapper mapper = new CsvMapper(factory);
		CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator('|').withHeader();
		MappingIterator<Map<String, String>> it =
			mapper.readerFor(Map.class).with(schema).readValues(csvFile);
		while (it.hasNext()) {
			Map<String, String> map = it.next();
			
			String userId = map.get("ID");
			String familyName = map.get("BEZEICHNUNG1");
			String name = map.get("BEZEICHNUNG2");
			String email = map.get("EMAIL");
			
			if (userId == null || userId.length() == 0) {
				if (verbose) {
					System.out.println("Invalid empty user id. Skipping line.");
				}
				continue;
			}
			
			if (StringUtils.isBlank(email)) {
				email = userId + "@" + System.getenv("ORGANSATION_DOMAIN");
				System.out.println(
					"No email-address for userId [" + userId + "], setting [" + email + "]");
			}
			
			if (usedEmail.contains(email)) {
				email = userId + "@" + System.getenv("ORGANSATION_DOMAIN");
				System.out.println(
					"Replace existing email for user [" + userId + "] with [" + email + "]");
			}
			email = email.toLowerCase();
			usedEmail.add(email);
			
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
				List<String> requiredActions = Collections.singletonList("UPDATE_PASSWORD");
				userRepresentation.setRequiredActions(requiredActions);
				
				Response response = elexisEnvironmentRealm.users().create(userRepresentation);
				if (response.getStatus() != 201) {
					System.err.println(
						"Couldn't create user [" + userId + "]: " + response.getStatusInfo());
					System.err.println("Most probably this is due to the e-mail address [" + email
						+ "] being already used. Please investigate.");
					continue;
				}
				String newId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
				if (verbose) {
					System.out.printf("C [%s], userId [%s], email [%s], password [changeme]%n",
						userId, newId, email);
				}
				
			} else {
				UserRepresentation userRepresentation = found.get(0);
				userRepresentation.singleAttribute("elexisContactId", elexisContactId);
				
				UserResource userResource =
					elexisEnvironmentRealm.users().get(userRepresentation.getId());
				userRepresentation.singleAttribute("elexisContactId", elexisContactId);
				userRepresentation.setEmail(email);
				userResource.update(userRepresentation);
				if (verbose) {
					System.out.printf("U [%s], userId [%s], elexisContactId [%s], email [%s]%n",
						userId, userRepresentation.getId(), elexisContactId, email);
				}
				
			}
			
		}
		
	}
	
}
