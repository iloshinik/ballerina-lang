package org.ballerinalang.stdlib.ldap.nativeimpl.context;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.Argument;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.stdlib.ldap.Constants;
import org.ballerinalang.stdlib.ldap.util.LdapUtil;

import javax.naming.directory.InitialDirContext;

@BallerinaFunction(
        orgName = "ballerina", packageName = "ldap",
        functionName = "createContext",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "LdapContext", structPackage = "ballerina/ldap"),
        args = {@Argument(name = "config", type = TypeKind.RECORD, structType = "LdapConfiguration")},
        isPublic = true
)

public class CreateContext implements NativeCallableUnit
{
    @Override
    public void execute( Context context, CallableUnitCallback callback )
    {
        Struct contextBObject = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        Struct ldapConfig = contextBObject.getStructField( Constants.B_OBJECT_FIELD_CONFIG );

        InitialDirContext dirContext = LdapUtil.getContext( ldapConfig );

        contextBObject.addNativeData( Constants.LDAP_CONTEXT, dirContext );
    }

    @Override
    public boolean isBlocking()
    {
        return true;
    }
}