package org.ballerinalang.stdlib.ldap.nativeimpl.context;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.stdlib.ldap.Constants;
import org.ballerinalang.stdlib.ldap.util.LdapUtil;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

@BallerinaFunction(
        orgName = "ballerina", packageName = "ldap",
        functionName = "search",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "LdapContext", structPackage = "ballerina/ldap"),
        args = {
                @Argument(name = "baseName", type = TypeKind.STRING),
                @Argument(name = "filter", type = TypeKind.STRING),
                @Argument(name = "controls", type = TypeKind.OBJECT, structType = "SearchControls")
        },
        returnType = {@ReturnType(type = TypeKind.MAP)},
        isPublic = true
)

public class Search implements NativeCallableUnit
{
    @Override
    public void execute( Context context, CallableUnitCallback callback )
    {
        Struct contextBObject = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        InitialDirContext dirContext = ( InitialDirContext ) contextBObject.getNativeData( Constants.LDAP_CONTEXT );

        String baseName = context.getStringArgument( 0 );
        String searchFilter = context.getStringArgument( 1 );
        SearchControls searchControls = LdapUtil.getSearchControls( context.getRefArgument( 1 ) );

        try
        {
            NamingEnumeration<SearchResult> results = dirContext.search( baseName, searchFilter, searchControls );
            BMap returnMap = LdapUtil.mapResults( results );
            context.setReturnValues( returnMap );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBlocking()
    {
        return true;
    }
}