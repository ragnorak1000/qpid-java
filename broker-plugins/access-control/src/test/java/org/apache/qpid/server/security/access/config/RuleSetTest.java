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

package org.apache.qpid.server.security.access.config;

import static org.mockito.Mockito.mock;

import javax.security.auth.Subject;

import org.apache.qpid.server.logging.EventLoggerProvider;
import org.apache.qpid.server.security.Result;
import org.apache.qpid.server.security.access.RuleOutcome;
import org.apache.qpid.server.security.access.config.ObjectProperties.Property;
import org.apache.qpid.server.security.auth.TestPrincipalUtils;
import org.apache.qpid.test.utils.QpidTestCase;

/**
 * This test checks that the {@link RuleSet} object which forms the core of the access control plugin performs correctly.
 *
 * The ruleset is configured directly rather than using an external file by adding rules individually, calling the
 * {@link RuleSetCreator#grant(Integer, String, RuleOutcome, LegacyOperation, ObjectType, ObjectProperties)} method. Then, the
 * access control mechanism is validated by checking whether operations would be authorised by calling the
 * {@link RuleSet#check(Subject, LegacyOperation, ObjectType, ObjectProperties)} method.
 *
 * It ensure that permissions can be granted correctly on users directly and on groups.
 */
public class RuleSetTest extends QpidTestCase
{
    private static final String DENIED_VH = "deniedVH";
    private static final String ALLOWED_VH = "allowedVH";

    private RuleSetCreator _ruleSetCreator = new RuleSetCreator();

    private static final String TEST_USER = "user";

    // Common things that are passed to frame constructors
    private String _queueName = this.getClass().getName() + "queue";
    private String _exchangeName = "amq.direct";
    private String _exchangeType = "direct";
    private Subject _testSubject = TestPrincipalUtils.createTestSubject(TEST_USER);

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        _ruleSetCreator = new RuleSetCreator();
    }

    private RuleSet createRuleSet()
    {
        return _ruleSetCreator.createRuleSet(mock(EventLoggerProvider.class));
    }

    private void assertDenyGrantAllow(Subject subject, LegacyOperation operation, ObjectType objectType)
    {
        assertDenyGrantAllow(subject, operation, objectType, ObjectProperties.EMPTY);
    }

    private void assertDenyGrantAllow(Subject subject,
                                      LegacyOperation operation,
                                      ObjectType objectType,
                                      ObjectProperties properties)
    {
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(subject, operation, objectType, properties));
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, operation, objectType, properties);
        ruleSet = createRuleSet();
        assertEquals(1, ruleSet.getRuleCount());
        assertEquals(Result.ALLOWED, ruleSet.check(subject, operation, objectType, properties));
    }

    public void testEmptyRuleSet()
    {
        RuleSet ruleSet = createRuleSet();
        assertNotNull(ruleSet);
        assertEquals(ruleSet.getRuleCount(), 0);
        assertEquals(ruleSet.getDefault(), ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }

    public void testVirtualHostNodeCreateAllowPermissionWithVirtualHostName() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.VIRTUALHOSTNODE, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.VIRTUALHOSTNODE, ObjectProperties.EMPTY));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.DELETE, ObjectType.VIRTUALHOSTNODE, ObjectProperties.EMPTY));
    }

    public void testVirtualHostAccessAllowPermissionWithVirtualHostName() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH));
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.DEFER, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessAllowPermissionWithNameSetToWildCard() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ObjectProperties.WILD_CARD));
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessAllowPermissionWithNoName() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessDenyPermissionWithNoName() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessDenyPermissionWithNameSetToWildCard() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ObjectProperties.WILD_CARD));
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessAllowDenyPermissions() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH));
        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH));
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(ALLOWED_VH)));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }

    public void testVirtualHostAccessAllowPermissionWithVirtualHostNameOtherPredicate() throws Exception
    {
        ObjectProperties properties = new ObjectProperties();
        properties.put(Property.VIRTUALHOST_NAME, ALLOWED_VH);

        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, properties);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, properties));
        assertEquals(Result.DEFER, ruleSet.check(_testSubject, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, new ObjectProperties(DENIED_VH)));
    }


    public void testQueueCreateNamed() throws Exception
    {
        assertDenyGrantAllow(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, new ObjectProperties(_queueName));
    }

    public void testQueueCreateNamedVirtualHost() throws Exception
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, new ObjectProperties(Property.VIRTUALHOST_NAME, ALLOWED_VH));
        RuleSet ruleSet = createRuleSet();
        ObjectProperties allowedQueueObjectProperties = new ObjectProperties(_queueName);
        allowedQueueObjectProperties.put(Property.VIRTUALHOST_NAME, ALLOWED_VH);
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, new ObjectProperties(allowedQueueObjectProperties)));

        ObjectProperties deniedQueueObjectProperties = new ObjectProperties(_queueName);
        deniedQueueObjectProperties.put(Property.VIRTUALHOST_NAME, DENIED_VH);
        assertEquals(Result.DEFER, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, deniedQueueObjectProperties));
    }

    public void testQueueCreateNamedNullRoutingKey()
    {
        ObjectProperties properties = new ObjectProperties(_queueName);
        properties.put(ObjectProperties.Property.ROUTING_KEY, (String) null);

        assertDenyGrantAllow(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, properties);
    }

    public void testExchangeCreateNamedVirtualHost()
    {
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.EXCHANGE, new ObjectProperties(Property.VIRTUALHOST_NAME, ALLOWED_VH));
        RuleSet ruleSet = createRuleSet();
        ObjectProperties allowedExchangeProperties = new ObjectProperties(_exchangeName);
        allowedExchangeProperties.put(Property.TYPE, _exchangeType);
        allowedExchangeProperties.put(Property.VIRTUALHOST_NAME, ALLOWED_VH);

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.EXCHANGE, allowedExchangeProperties));

        ObjectProperties deniedExchangeProperties = new ObjectProperties(_exchangeName);
        deniedExchangeProperties.put(Property.TYPE, _exchangeType);
        deniedExchangeProperties.put(Property.VIRTUALHOST_NAME, DENIED_VH);
        assertEquals(Result.DEFER, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.EXCHANGE, deniedExchangeProperties));
    }

    public void testExchangeCreate()
    {
        ObjectProperties properties = new ObjectProperties(_exchangeName);
        properties.put(ObjectProperties.Property.TYPE, _exchangeType);

        assertDenyGrantAllow(_testSubject, LegacyOperation.CREATE, ObjectType.EXCHANGE, properties);
    }

    public void testConsume()
    {
        assertDenyGrantAllow(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE);
    }

    public void testPublish()
    {
        assertDenyGrantAllow(_testSubject, LegacyOperation.PUBLISH, ObjectType.EXCHANGE);
    }

    /**
    * If the consume permission for temporary queues is for an unnamed queue then it should
    * be global for any temporary queue but not for any non-temporary queue
    */
    public void testTemporaryUnnamedQueueConsume()
    {
        ObjectProperties temporary = new ObjectProperties();
        temporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);

        ObjectProperties normal = new ObjectProperties();
        normal.put(ObjectProperties.Property.AUTO_DELETE, Boolean.FALSE);

        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));
        _ruleSetCreator.grant(0, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary);
        ruleSet = createRuleSet();
        assertEquals(1, ruleSet.getRuleCount());
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));

        // defer to global if exists, otherwise default answer - this is handled by the security manager
        assertEquals(Result.DEFER, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, normal));
    }

    /**
     * Test that temporary queue permissions before queue perms in the ACL config work correctly
     */
    public void testTemporaryQueueFirstConsume()
    {
        ObjectProperties temporary = new ObjectProperties(_queueName);
        temporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);

        ObjectProperties normal = new ObjectProperties(_queueName);
        normal.put(ObjectProperties.Property.AUTO_DELETE, Boolean.FALSE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));

        // should not matter if the temporary permission is processed first or last
        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CONSUME, ObjectType.QUEUE, normal);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, normal));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));
    }

    /**
     * Test that temporary queue permissions after queue perms in the ACL config work correctly
     */
    public void testTemporaryQueueLastConsume()
    {
        ObjectProperties temporary = new ObjectProperties(_queueName);
        temporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);

        ObjectProperties normal = new ObjectProperties(_queueName);
        normal.put(ObjectProperties.Property.AUTO_DELETE, Boolean.FALSE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));

        // should not matter if the temporary permission is processed first or last
        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CONSUME, ObjectType.QUEUE, normal);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, normal));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CONSUME, ObjectType.QUEUE, temporary));
    }

    /*
     * Test different rules for temporary queues.
     */

    /**
     * The more generic rule first is used, so both requests are allowed.
     */
    public void testFirstNamedSecondTemporaryQueueDenied()
    {
        ObjectProperties named = new ObjectProperties(_queueName);
        ObjectProperties namedTemporary = new ObjectProperties(_queueName);
        namedTemporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);

        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));

        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, named);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.DENY, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
    }

    /**
     * The more specific rule is first, so those requests are denied.
     */
    public void testFirstTemporarySecondNamedQueueDenied()
    {
        ObjectProperties named = new ObjectProperties(_queueName);
        ObjectProperties namedTemporary = new ObjectProperties(_queueName);
        namedTemporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));

        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.DENY, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, named);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
    }

    /**
     * The more specific rules are first, so those requests are denied.
     */
    public void testFirstTemporarySecondDurableThirdNamedQueueDenied()
    {
        ObjectProperties named = new ObjectProperties(_queueName);
        ObjectProperties namedTemporary = new ObjectProperties(_queueName);
        namedTemporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);
        ObjectProperties namedDurable = new ObjectProperties(_queueName);
        namedDurable.put(ObjectProperties.Property.DURABLE, Boolean.TRUE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedDurable));

        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.DENY, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.DENY, LegacyOperation.CREATE, ObjectType.QUEUE, namedDurable);
        _ruleSetCreator.grant(3, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, named);
        ruleSet = createRuleSet();
        assertEquals(3, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedDurable));
    }

    public void testNamedTemporaryQueueAllowed()
    {
        ObjectProperties named = new ObjectProperties(_queueName);
        ObjectProperties namedTemporary = new ObjectProperties(_queueName);
        namedTemporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));

        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, named);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
    }

    public void testNamedTemporaryQueueDeniedAllowed()
    {
        ObjectProperties named = new ObjectProperties(_queueName);
        ObjectProperties namedTemporary = new ObjectProperties(_queueName);
        namedTemporary.put(ObjectProperties.Property.AUTO_DELETE, Boolean.TRUE);
        RuleSet ruleSet = createRuleSet();
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));

        _ruleSetCreator.grant(1, TEST_USER, RuleOutcome.ALLOW, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary);
        _ruleSetCreator.grant(2, TEST_USER, RuleOutcome.DENY, LegacyOperation.CREATE, ObjectType.QUEUE, named);
        ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.DENIED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, named));
        assertEquals(Result.ALLOWED, ruleSet.check(_testSubject, LegacyOperation.CREATE, ObjectType.QUEUE, namedTemporary));
    }

    /**
     * Tests support for the {@link Rule#ALL} keyword.
     */
    public void testAllowToAll()
    {
        _ruleSetCreator.grant(1, Rule.ALL, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(1, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(TestPrincipalUtils.createTestSubject("usera"), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
        assertEquals(Result.ALLOWED, ruleSet.check(TestPrincipalUtils.createTestSubject("userb"), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }

    public void testGroupsSupported()
    {
        String allowGroup = "allowGroup";
        String deniedGroup = "deniedGroup";

        _ruleSetCreator.grant(1, allowGroup, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        _ruleSetCreator.grant(2, deniedGroup, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(TestPrincipalUtils.createTestSubject("usera", allowGroup), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
        assertEquals(Result.DENIED, ruleSet.check(TestPrincipalUtils.createTestSubject("userb", deniedGroup), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
        assertEquals(Result.DEFER, ruleSet.check(TestPrincipalUtils.createTestSubject("user", "group not mentioned in acl"), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }

    /**
     * Rule order in the ACL determines the outcome of the check.  This test ensures that a user who is
     * granted explicit permission on an object, is granted that access even though a group
     * to which the user belongs is later denied the permission.
     */
    public void testAllowDeterminedByRuleOrder()
    {
        String group = "group";
        String user = "user";

        _ruleSetCreator.grant(1, user, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        _ruleSetCreator.grant(2, group, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.ALLOWED, ruleSet.check(TestPrincipalUtils.createTestSubject(user, group), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }

    /**
     * Rule order in the ACL determines the outcome of the check.  This tests ensures that a user who is denied
     * access by group, is denied access, despite there being a later rule granting permission to that user.
     */
    public void testDenyDeterminedByRuleOrder()
    {
        String group = "aclgroup";
        String user = "usera";

        _ruleSetCreator.grant(1, group, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        _ruleSetCreator.grant(2, user, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        assertEquals(2, ruleSet.getRuleCount());

        assertEquals(Result.DENIED, ruleSet.check(TestPrincipalUtils.createTestSubject(user, group), LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }

    public void testUserInMultipleGroups()
    {
        String allowedGroup = "group1";
        String deniedGroup = "group2";

        _ruleSetCreator.grant(1, allowedGroup, RuleOutcome.ALLOW, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        _ruleSetCreator.grant(2, deniedGroup, RuleOutcome.DENY, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY);
        RuleSet ruleSet = createRuleSet();
        Subject subjectInBothGroups = TestPrincipalUtils.createTestSubject("user", allowedGroup, deniedGroup);
        Subject subjectInDeniedGroupAndOneOther = TestPrincipalUtils.createTestSubject("user", deniedGroup, "some other group");
        Subject subjectInAllowedGroupAndOneOther = TestPrincipalUtils.createTestSubject("user", allowedGroup, "some other group");

        assertEquals(Result.ALLOWED, ruleSet.check(subjectInBothGroups, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));

        assertEquals(Result.DENIED, ruleSet.check(subjectInDeniedGroupAndOneOther, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));

        assertEquals(Result.ALLOWED, ruleSet.check(subjectInAllowedGroupAndOneOther, LegacyOperation.ACCESS, ObjectType.VIRTUALHOST, ObjectProperties.EMPTY));
    }
}
