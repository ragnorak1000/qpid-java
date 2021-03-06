/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.server.management.plugin.servlet.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.ConfiguredObject;

public class StructureServlet extends AbstractServlet
{
    public StructureServlet()
    {
        super();
    }

    @Override
    protected void doGetWithSubjectAndActor(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {

        // TODO filtering??? request.getParameter("filter"); // filter=1,2,3   /groups/*/*

        Map<String,Object> structure = generateStructure(getBroker(), Broker.class);

        sendJsonResponse(structure, request, response);

    }

    private Map<String, Object> generateStructure(ConfiguredObject object, Class<? extends ConfiguredObject> clazz)
    {
        Map<String, Object> structure = new LinkedHashMap<String, Object>();
        structure.put("id", object.getId());
        structure.put("name", object.getName());

        for(Class<? extends ConfiguredObject> childClass : object.getModel().getChildTypes(clazz))
        {
            Collection<? extends ConfiguredObject> children = object.getChildren(childClass);
            if(children != null)
            {
                List<Map<String, Object>> childObjects = new ArrayList<Map<String, Object>>();

                for(ConfiguredObject child : children)
                {
                    childObjects.add(generateStructure(child, childClass));
                }

                if(!childObjects.isEmpty())
                {
                    structure.put(pluralize(childClass),childObjects);
                }
            }
        }

        return structure;
    }

    private String pluralize(Class<? extends ConfiguredObject> childClass)
    {
        String name = childClass.getSimpleName().toLowerCase();
        return name + (name.endsWith("s") ? "es" : "s");
    }
}
