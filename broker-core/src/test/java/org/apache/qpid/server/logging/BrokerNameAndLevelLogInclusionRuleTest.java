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
package org.apache.qpid.server.logging;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.configuration.updater.TaskExecutor;
import org.apache.qpid.server.configuration.updater.TaskExecutorImpl;
import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.BrokerLogger;
import org.apache.qpid.server.model.BrokerModel;
import org.apache.qpid.server.model.Model;
import org.apache.qpid.test.utils.QpidTestCase;

public class BrokerNameAndLevelLogInclusionRuleTest extends QpidTestCase
{
    private BrokerLogger<?> _brokerLogger;
    private TaskExecutor _taskExecutor;
    private final Broker<?> _broker = mock(Broker.class);

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        _taskExecutor =  new TaskExecutorImpl();
        _taskExecutor.start();

        Model model = BrokerModel.getInstance();

        when(_broker.getModel()).thenReturn(model);
        doReturn(Broker.class).when(_broker).getCategoryClass();

        _brokerLogger = mock(BrokerLogger.class);
        when(_brokerLogger.getModel()).thenReturn(model);
        when(_brokerLogger.getChildExecutor()).thenReturn(_taskExecutor);
        when(_brokerLogger.getParent(Broker.class)).thenReturn(_broker);
        doReturn(BrokerLogger.class).when(_brokerLogger).getCategoryClass();
   }

    @Override
    public void tearDown() throws Exception
    {
        try
        {
            _taskExecutor.stopImmediately();
        }
        finally
        {
            super.tearDown();
        }
    }


    public void testAsFilter()
    {
        BrokerNameAndLevelLogInclusionRule<?> rule = createRule("org.apache.qpid", LogLevel.INFO);

        Filter<ILoggingEvent> filter = rule.asFilter();

        assertTrue("Unexpected filter instance", filter instanceof LoggerNameAndLevelFilter);

        LoggerNameAndLevelFilter f = (LoggerNameAndLevelFilter)filter;
        assertEquals("Unexpected log level", Level.INFO, f.getLevel());
        assertEquals("Unexpected logger name", "org.apache.qpid", f.getLoggerName());
    }

    public void testLevelChangeAffectsFilter()
    {
        BrokerNameAndLevelLogInclusionRule<?> rule = createRule("org.apache.qpid", LogLevel.INFO);

        LoggerNameAndLevelFilter filter = (LoggerNameAndLevelFilter)rule.asFilter();

        assertEquals("Unexpected log level", Level.INFO, filter.getLevel());

        rule.setAttributes(Collections.<String, Object>singletonMap("level", LogLevel.DEBUG));
        assertEquals("Unexpected log level attribute", Level.DEBUG, filter.getLevel());
    }

    public void testLoggerNameChangeNotAllowed()
    {
        BrokerNameAndLevelLogInclusionRule<?> rule = createRule("org.apache.qpid", LogLevel.INFO);

        LoggerNameAndLevelFilter filter = (LoggerNameAndLevelFilter)rule.asFilter();

        assertEquals("Unexpected logger name", "org.apache.qpid", filter.getLoggerName());

        try
        {
            rule.setAttributes(Collections.<String, Object>singletonMap(BrokerNameAndLevelLogInclusionRule.LOGGER_NAME, "org.apache.qpid.foo"));
            fail("IllegalConfigurationException is expected to throw on attempt to change logger name");
        }
        catch(IllegalConfigurationException e)
        {
            // pass
        }

        assertEquals("Unexpected logger name", "org.apache.qpid", filter.getLoggerName());
    }


    private BrokerNameAndLevelLogInclusionRule createRule(String loggerName, LogLevel logLevel)
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("loggerName", loggerName);
        attributes.put("level", logLevel);
        attributes.put("name", "test");

        BrokerNameAndLevelLogInclusionRule brokerNameAndLevelLogInclusionRule = new BrokerNameAndLevelLogInclusionRuleImpl(attributes, _brokerLogger);
        brokerNameAndLevelLogInclusionRule.open();
        return brokerNameAndLevelLogInclusionRule;
    }

}
