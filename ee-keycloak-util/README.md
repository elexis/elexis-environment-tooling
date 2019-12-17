
# Keycloak util

This util provides commands to perform tasks on the Keycloak resource.

### Command `syncUsersFromElexisCsv`

Sync all users from a csv file generated out of the Elexis user database to keycloak. The file has to be generated using the following SQL statement: 

`SELECT U.ID,U.KONTAKT_ID,K.BEZEICHNUNG1,K.BEZEICHNUNG2,K.BEZEICHNUNG3,K.EMAIL from USER_ AS U JOIN KONTAKT AS K WHERE U.KONTAKT_ID=K.ID`


### Export

Be sure to use `Package required libraries into generated JAR`