/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.management.amqp;

import java.security.Principal;

import org.apache.qpid.server.model.NamedAddressSpace;

class ManagementAddressSpacePrincipal implements Principal
{
    private final NamedAddressSpace _addressSpace;
    private final String _name;

    public ManagementAddressSpacePrincipal(NamedAddressSpace addressSpace)
    {
        _addressSpace = addressSpace;
        _name = "addressspace:" + addressSpace.getName();
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        ManagementAddressSpacePrincipal that = (ManagementAddressSpacePrincipal) o;
        return _addressSpace.equals(that._addressSpace);
    }

    @Override
    public int hashCode()
    {
        return _addressSpace.hashCode();
    }
}
