import ballerina/ldap;

function createContextTest() returns (map) {

    ldap:LdapContext ldapContext = new({
            initialContextFactory: "com.sun.jndi.ldap.LdapCtxFactory",
            providerUrl: "ldap://localhost:10389" }
    );

    ldap:SearchControls controls = new();
    controls.setSearchScope( ldap:SUBTREE_SCOPE );

    map m =  ldapContext.search( "o=wso2", "(&(ou=people)(objectClass=top))", controls );

    ldapContext.close();

    return m;
}