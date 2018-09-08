package org.ballerinalang.test.ldap;

import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BValue;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class LdapTest
{
    private CompileResult compileResult;
    private static final double DELTA = 0.0000000001;

    @BeforeClass
    public void setup() {
        compileResult = BCompileUtil.compileAndSetup( "test-src/ldap/ldap.bal" );
    }
    @Test(description = "Test 'createContext' function in ballerina.ldap package")
    public void testCreateContext() {

        BValue[] returns = BRunUtil.invoke(compileResult, "createContextTest");

        BMap map = ( BMap ) returns[0];

        System.out.println( map );
    }
}