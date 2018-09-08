public type LdapContext object {

    public LdapConfiguration config;

    public new(config) {
        createContext();
    }

    native function createContext();

    public native function search(string baseName, string filter, SearchControls controls) returns (map);

    public native function close();

};

public type LdapConfiguration record {
    string initialContextFactory,
    string providerUrl,
    string? username,
    string? password,
};