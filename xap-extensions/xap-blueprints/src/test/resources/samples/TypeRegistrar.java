package org.gigaspaces.blueprints;

import com.gigaspaces.dih.consumer.CDCInfo;
import com.gigaspaces.dih.model.types.COMPANYDocument;
import com.gigaspaces.dih.model.types.EmployeeDocument;
import com.gigaspaces.dih.model.types.EmployeeOverrideDocument;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceTypeManager;

/**
 * This class was auto-generated by GigaSpaces
 */
public class TypeRegistrar {
    public static void registerTypes(GigaSpace gigaspace) {
        GigaSpaceTypeManager typeManager = gigaspace.getTypeManager();

        typeManager.registerTypeDescriptor(CDCInfo.getTypeDescriptor());
        typeManager.registerTypeDescriptor(COMPANYDocument.getTypeDescriptor());
        typeManager.registerTypeDescriptor(EmployeeDocument.getTypeDescriptor());
        typeManager.registerTypeDescriptor(EmployeeOverrideDocument.getTypeDescriptor());
    }
}
