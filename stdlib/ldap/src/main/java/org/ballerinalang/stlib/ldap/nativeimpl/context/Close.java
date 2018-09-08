package org.ballerinalang.stdlib.ldap.nativeimpl.context;

import org.ballerinalang.bre.Context;
import org.ballerinalang.bre.bvm.CallableUnitCallback;
import org.ballerinalang.connector.api.BLangConnectorSPIUtil;
import org.ballerinalang.connector.api.Struct;
import org.ballerinalang.model.NativeCallableUnit;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.Receiver;
import org.ballerinalang.stdlib.ldap.Constants;

import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;


@BallerinaFunction(
        orgName = "ballerina", packageName = "ldap",
        functionName = "close",
        receiver = @Receiver(type = TypeKind.OBJECT, structType = "LdapContext", structPackage = "ballerina/ldap"),
        isPublic = true
)
public class Close implements NativeCallableUnit
{
    @Override
    public void execute(Context context, CallableUnitCallback callback) {

        Struct contextBObject = BLangConnectorSPIUtil.getConnectorEndpointStruct(context);
        InitialDirContext dirContext = ( InitialDirContext ) contextBObject.getNativeData( Constants.LDAP_CONTEXT );

        try {
            dirContext.close();
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isBlocking() {
        return true;
    }
}