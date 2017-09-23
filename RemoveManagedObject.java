/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.general;

import com.vmware.common.annotations.Action;
import com.vmware.common.annotations.Option;
import com.vmware.common.annotations.Sample;
import com.vmware.connection.ConnectedVimServiceBase;
import com.vmware.vim25.*;

import java.util.Map;

/**
 * <pre>
 * This sample demonstrates Destroy or Unregister
 * Managed Inventory Object like a Host, VM, Folder, etc
 *
 * <b>Parameters:</b>
 * url          [required] : url of the web service
 * username     [required] : username for the authentication
 * password     [required] : password for the authentication
 * objtype      [required] : type of managedobject to remove or unregister");
 *                           e.g. HostSystem, Datacenter, ResourcePool, Folder
 * objname      [required] : Name of the object
 * operation    [optional] : Name of the operation - [remove | unregister]
 *
 * <b>Command Line:</b>
 * Remove a folder named Fold
 * run.bat com.vmware.general.RemoveManagedObject --url [webserviceurl]
 * --username [username] --password  [password]
 * --objtype Folder --objname  Fold
 *
 * Unregister a virtual machine named VM1
 * run.bat com.vmware.general.RemoveManagedObject
 * --url [webserviceurl] --username [username] --password  [password]
 * --objtype VirtualMachine --objname VM1 --operation unregister
 * </pre>
 */

@Sample(name = "remove-managed-object", description = "demonstrates Destroy or Unregister Managed Inventory Object like a Host, VM, Folder, etc")
public class RemoveManagedObject extends ConnectedVimServiceBase {

    public final String SVC_INST_NAME = "ServiceInstance";
    public final static String[] OBJECT_TYPES = {
            "HostSystem","VirtualMachine","Folder","ResourcePool","Datacenter"
    };

    String objectname;
    String objecttype;
    String operation;

    @Option(
            name = "objtype",
            description = "type of managedobject to remove or unregister  " +
                    "e.g. HostSystem, VirtualMachine, Folder, ResourcePool, Datacenter"
    )
    public void setObjecttype(String objecttype) {
        this.objecttype = objecttype;
    }

    @Option(name = "objname", description = "Name of the object")
    public void setObjectname(String objectname) {
        this.objectname = objectname;
    }

    @Option(name = "operation", required = false, description = "Name of the operation - [remove | unregister]")
    public void setOperation(String operation) {
        this.operation = operation;
    }

    public boolean validateObjectType(final String type) {
        boolean found = false;

        for(String name : OBJECT_TYPES) {
            found |= name.equalsIgnoreCase(type);
        }

        return found;
    }

    public boolean validateTheInput() {
        if (operation != null) {
            if (!(operation.equalsIgnoreCase("remove"))
                    && (!(operation.equalsIgnoreCase("unregister")))) {
                throw new IllegalArgumentException("Invalid Operation type");
            }
        }


        if (!validateObjectType(objecttype)) {
            final StringBuilder list = new StringBuilder();
            for(final String name : OBJECT_TYPES) {
                list.append("'");
                list.append(name);
                list.append("' ");
            }
            throw new IllegalArgumentException(
                    String.format(
                            "Invalid --objtype %s! Object Type should be one of: %s",
                            objecttype,list.toString()
                    )
            );
        }
        return true;
    }

    public void deleteManagedObjectReference() throws RuntimeFaultFaultMsg, InvalidPropertyFaultMsg, VimFaultFaultMsg, InvalidCollectorVersionFaultMsg, TaskInProgressFaultMsg, InvalidPowerStateFaultMsg {
        Map<String, ManagedObjectReference> objList = getMOREFs.inContainerByType(serviceContent
                .getRootFolder(), objecttype, new RetrieveOptions());
        ManagedObjectReference objmor = objList.get(objectname);

        if (objmor != null) {
            if ("remove".equals(operation)) {
                ManagedObjectReference taskmor = vimPort.destroyTask(objmor);
                String[] opts = new String[]{"info.state", "info.error"};
                String[] opt = new String[]{"state"};

                Object[] result =
                        waitForValues.wait(taskmor, opts, opt,
                                new Object[][]{new Object[]{
                                        TaskInfoState.SUCCESS, TaskInfoState.ERROR}});
                if (result[0].equals(TaskInfoState.SUCCESS)) {
                    System.out.printf("Success Managed Entity - [ %s ]"
                            + " deleted %n", objectname);
                } else {
                    System.out.printf("Failure Deletion of Managed Entity - "
                            + "[ %s ] %n", objectname);
                }
            } else if ("VirtualMachine".equalsIgnoreCase(objecttype)) {
                vimPort.unregisterVM(objmor);
            } else {
                throw new IllegalArgumentException("Invalid Operation specified.");
            }
            System.out.println("Successfully completed " + operation + " for "
                    + objecttype + " : " + objectname);
        } else {
            System.out.println("Unable to find object of type  " + objecttype
                    + " with name  " + objectname);
            System.out.println(" : Failed " + operation + " of " + objecttype
                    + " : " + objectname);
        }
    }

    @Action
    public void run() throws RuntimeFaultFaultMsg, TaskInProgressFaultMsg, InvalidPropertyFaultMsg, VimFaultFaultMsg, InvalidCollectorVersionFaultMsg, InvalidPowerStateFaultMsg {
        validateTheInput();

        if ((operation == null || operation.length() == 0)
                && (objecttype.equalsIgnoreCase("VirtualMachine"))) {
            operation = "unregisterVM";
        } else if ((operation == null || operation.length() == 0)
                && !(objecttype.equalsIgnoreCase("VirtualMachine"))) {
            operation = "remove";
        } else {
            if (!("remove".equals(operation))
                    && !("unregisterVM".equals(operation))) {
                operation = "unregisterVM";
            }
        }
        deleteManagedObjectReference();
    }
}
