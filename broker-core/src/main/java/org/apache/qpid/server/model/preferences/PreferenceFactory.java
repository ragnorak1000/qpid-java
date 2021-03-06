/*
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
 */

package org.apache.qpid.server.model.preferences;

import java.security.Principal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.qpid.server.model.ConfiguredObject;
import org.apache.qpid.server.plugin.QpidServiceLoader;
import org.apache.qpid.server.security.auth.AuthenticatedPrincipal;

public class PreferenceFactory
{
    public static Preference create(final ConfiguredObject<?> associatedObject,
                                    final Map<String, Object> attributes)
    {
        AuthenticatedPrincipal currentUser = AuthenticatedPrincipal.getCurrentUser();
        Map<String, Object> overriddenAttributes =  new HashMap<>(attributes);
        overriddenAttributes.put(Preference.OWNER_ATTRIBUTE, currentUser == null ? null : currentUser.getName());
        overriddenAttributes.put(Preference.LAST_UPDATED_DATE_ATTRIBUTE, System.currentTimeMillis());
        return recover(associatedObject, overriddenAttributes);
    }

    public static Preference recover(final ConfiguredObject<?> associatedObject,
                                    final Map<String, Object> attributes)
    {
        final UUID uuid = getId(attributes);
        final String type = getAttributeAsString(Preference.TYPE_ATTRIBUTE, attributes);
        final String name = getAttributeAsString(Preference.NAME_ATTRIBUTE, attributes);
        final String description = getAttributeAsString(Preference.DESCRIPTION_ATTRIBUTE, attributes);
        final String owner = getAttributeAsString(Preference.OWNER_ATTRIBUTE, attributes);
        if (owner == null)
        {
            throw new IllegalArgumentException("Preference owner is mandatory");
        }
        final Set<Principal> visibilitySet = getVisibilitySet(attributes);
        final Date lastUpdatedDate = getAttributeAsDate(Preference.LAST_UPDATED_DATE_ATTRIBUTE, attributes);
        final Map<String, Object> preferenceValueAttributes = getPreferenceValue(attributes);
        PreferenceValue value = convertMapToPreferenceValue(type, preferenceValueAttributes);
        return new PreferenceImpl(associatedObject, uuid, name, type, description,
                                  new GenericPrincipal(owner),
                                  lastUpdatedDate,
                                  visibilitySet,
                                  value);
    }


    private static UUID getId(final Map<String, Object> attributes)
    {
        final Object id = attributes.get(Preference.ID_ATTRIBUTE);
        UUID uuid;
        if (id != null)
        {
            if (id instanceof UUID)
            {
                uuid = (UUID) id;
            }
            else if (id instanceof String)
            {
                uuid = UUID.fromString((String) id);
            }
            else
            {
                throw new IllegalArgumentException(String.format("Preference attribute '%s' is not a UUID",
                                                                 Preference.ID_ATTRIBUTE));
            }
        }
        else
        {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    private static Map<String, Object> getPreferenceValue(final Map<String, Object> attributes)
    {
        Object value = attributes.get(Preference.VALUE_ATTRIBUTE);
        if (value == null)
        {
            return null;
        }
        else if (value instanceof Map)
        {
            HashMap<String, Object> preferenceValue = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet())
            {
                Object key = entry.getKey();
                if (key instanceof String)
                {
                    preferenceValue.put((String) key, entry.getValue());
                }
                else
                {
                    throw new IllegalArgumentException(String.format("Invalid value entry: '%s' is not a String", key));
                }
            }
            return preferenceValue;
        }
        throw new IllegalArgumentException(String.format("Cannot recover '%s' as Map", Preference.VALUE_ATTRIBUTE));
    }

    private static Set<Principal> getVisibilitySet(final Map<String, Object> attributes)
    {
        Object value = attributes.get(Preference.VISIBILITY_LIST_ATTRIBUTE);
        if (value == null)
        {
            return null;
        }
        else if (value instanceof Collection)
        {
            HashSet<Principal> principals = new HashSet<>();
            for (Object element : (Collection) value)
            {
                if (!(element instanceof String))
                {
                    String errorMessage =
                            String.format("Invalid visibilityList element: '%s' is not a String", element);
                    throw new IllegalArgumentException(errorMessage);
                }
                principals.add(new GenericPrincipal((String) element));
            }
            return principals;
        }
        throw new IllegalArgumentException(String.format("Cannot recover '%s' as List",
                                                         Preference.VISIBILITY_LIST_ATTRIBUTE));
    }

    private static String getAttributeAsString(final String attributeName, final Map<String, Object> attributes)
    {
        Object value = attributes.get(attributeName);
        if (value == null || value instanceof String)
        {
            return (String) value;
        }
        throw new IllegalArgumentException(String.format("Preference attribute '%s' is not a String", attributeName));
    }

    private static Date getAttributeAsDate(final String attributeName, final Map<String, Object> attributes)
    {
        Object dateObject = attributes.get(attributeName);
        if (dateObject instanceof Number)
        {
            return new Date(((Number)dateObject).longValue());
        }
        return new Date();
    }

    private static PreferenceValue convertMapToPreferenceValue(String type, Map<String, Object> preferenceValueAttributes)
    {
        String implementationType = type;
        if (type != null && type.startsWith("X-"))
        {
            implementationType = "X-generic";
        }

        final Map<String, PreferenceValueFactoryService> preferenceValueFactories =
                new QpidServiceLoader().getInstancesByType(PreferenceValueFactoryService.class);

        final PreferenceValueFactoryService preferenceValueFactory = preferenceValueFactories.get(implementationType);
        if (preferenceValueFactory == null)
        {
            throw new IllegalArgumentException(String.format("Cannot find preference type factory for type '%s'",
                                                             implementationType));
        }

        return preferenceValueFactory.createInstance(preferenceValueAttributes);
    }
}
