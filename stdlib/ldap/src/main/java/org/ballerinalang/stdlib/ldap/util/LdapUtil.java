package org.ballerinalang.stdlib.ldap.util;

import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.types.BField;
import org.ballerinalang.model.types.BRecordType;
import org.ballerinalang.model.values.BInteger;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.stdlib.ldap.Constants;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;


public class LdapUtil
{
    private LdapUtil(){}

    public static InitialDirContext getContext( Struct ldapConfig ){

        Hashtable<String, String> env = new Hashtable<>();

        String initialContextFactory = ldapConfig.getStringField( Constants.ALIAS_INITIAL_CONTEXT_FACTORY);
        env.put( Context.INITIAL_CONTEXT_FACTORY, initialContextFactory );

        String providerUrl = ldapConfig.getStringField( Constants.ALIAS_PROVIDER_URL);
        env.put( Context.PROVIDER_URL, providerUrl );

        InitialDirContext ctx = null;

        try
        {
            ctx = new InitialDirContext( env );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }
        return ctx;

    }

    public static SearchControls getSearchControls( BValue searchControlParamValue )
    {
        SearchControls searchControls = new SearchControls();

        BMap searchControlParamMap = (BMap ) searchControlParamValue;

        BInteger bInteger = ( BInteger ) searchControlParamMap.getMap().getOrDefault( Constants.SCH_CONT_FIELD_SEARCH_SCOPE, new BInteger(1) );
        searchControls.setSearchScope( ( int ) bInteger.intValue() );

        return searchControls;
    }

    public static BMap mapResults( NamingEnumeration<SearchResult> results ) throws NamingException
    {
        BMap<String, BValue> stringObjectBMap = new BMap<>();
        while( results.hasMore() )
        {
            SearchResult result = results.next();

            NamingEnumeration<? extends Attribute> atrEnum = result.getAttributes().getAll();

            while(atrEnum.hasMore())
            {
                Attribute attribute = atrEnum.next();
                stringObjectBMap.addNativeData( attribute.getID(), attribute );
            }
        }
        return stringObjectBMap;
    }
}