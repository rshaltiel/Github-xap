package org.gigaspaces.blueprints;

import com.gigaspaces.internal.io.BootIOUtils;
import org.gigaspaces.blueprints.java.dih.dih_model.TypeRegistrarInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openspaces.core.GigaSpace;

import java.io.IOException;

public class TypeRegistratInfoTestCase {

    private static final String SPACE_NAME = "demo";
    private static GigaSpace gigaSpace;

    @BeforeAll
    static void setUp() {
//        gigaSpace = new GigaSpaceConfigurer(new EmbeddedSpaceConfigurer(SPACE_NAME)).gigaSpace();
//        gigaSpace.getTypeManager().registerTypeDescriptor(EmployeeDocument.getTypeDescriptor());
    }

    @AfterEach
//    void tearDown() {
//        gigaSpace.clear(null);
//    }

    @Test
    public void basicTestWithInitialLoad() throws IOException {
        String expected = BootIOUtils.readAsString(BootIOUtils.getResourcePath("samples/TypeRegistrar.java"));
        TypeRegistrarInfo typeRegistrarInfo = new TypeRegistrarInfo("org.gigaspaces.blueprints");
        typeRegistrarInfo.addClass("com.gigaspaces.data_integration.consumer", "CDCInfo");
        typeRegistrarInfo.addClass("com.gigaspaces.data_integration.model.types", "COMPANYDocument");
        typeRegistrarInfo.addClass("com.gigaspaces.data_integration.model.types", "EmployeeDocument");
        typeRegistrarInfo.addClass("com.gigaspaces.data_integration.model.types", "EmployeeOverrideDocument");

        String actual = typeRegistrarInfo.generate();
        System.out.println("Actual=\n\n" + actual);
        System.out.println("Expected=\n\n" + expected);
        Assertions.assertEquals(expected, actual);
    }

}
