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
package org.apache.qpid.server.protocol.converter.v0_8_v0_10;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.qpid.AMQPInvalidClassException;
import org.apache.qpid.bytebuffer.QpidByteBuffer;
import org.apache.qpid.exchange.ExchangeDefaults;
import org.apache.qpid.framing.AMQShortString;
import org.apache.qpid.framing.BasicContentHeaderProperties;
import org.apache.qpid.framing.ContentHeaderBody;
import org.apache.qpid.framing.FieldTable;
import org.apache.qpid.framing.MessagePublishInfo;
import org.apache.qpid.server.message.MessageDestination;
import org.apache.qpid.server.model.Exchange;
import org.apache.qpid.server.model.NamedAddressSpace;
import org.apache.qpid.server.model.VirtualHost;
import org.apache.qpid.server.plugin.MessageConverter;
import org.apache.qpid.server.plugin.PluggableService;
import org.apache.qpid.server.protocol.v0_10.MessageTransferMessage;
import org.apache.qpid.server.protocol.v0_8.AMQMessage;
import org.apache.qpid.server.protocol.v0_8.MessageMetaData;
import org.apache.qpid.server.store.StoredMessage;
import org.apache.qpid.transport.DeliveryProperties;
import org.apache.qpid.transport.Header;
import org.apache.qpid.transport.MessageDeliveryMode;
import org.apache.qpid.transport.MessageProperties;
import org.apache.qpid.transport.ReplyTo;

@PluggableService
public class MessageConverter_0_10_to_0_8 implements MessageConverter<MessageTransferMessage, AMQMessage>
{
    private static final int BASIC_CLASS_ID = 60;

    public static BasicContentHeaderProperties convertContentHeaderProperties(MessageTransferMessage messageTransferMessage,
                                                                              NamedAddressSpace addressSpace)
    {
        BasicContentHeaderProperties props = new BasicContentHeaderProperties();

        Header header = messageTransferMessage.getHeader();
        DeliveryProperties deliveryProps = header.getDeliveryProperties();
        MessageProperties messageProps = header.getMessageProperties();

        if(deliveryProps != null)
        {
            if(deliveryProps.hasDeliveryMode())
            {
                props.setDeliveryMode((deliveryProps.getDeliveryMode() == MessageDeliveryMode.PERSISTENT
                                              ? BasicContentHeaderProperties.PERSISTENT
                                              : BasicContentHeaderProperties.NON_PERSISTENT));
            }
            if(deliveryProps.hasExpiration())
            {
                props.setExpiration(deliveryProps.getExpiration());
            }
            if(deliveryProps.hasPriority())
            {
                props.setPriority((byte) deliveryProps.getPriority().getValue());
            }
            if(deliveryProps.hasTimestamp())
            {
                props.setTimestamp(deliveryProps.getTimestamp());
            }
        }
        if(messageProps != null)
        {
            if(messageProps.hasAppId())
            {
                props.setAppId(new AMQShortString(messageProps.getAppId()));
            }
            if(messageProps.hasContentType())
            {
                props.setContentType(messageProps.getContentType());
            }
            if(messageProps.hasCorrelationId())
            {
                props.setCorrelationId(new AMQShortString(messageProps.getCorrelationId()));
            }
            if(messageProps.hasContentEncoding())
            {
                props.setEncoding(messageProps.getContentEncoding());
            }
            if(messageProps.hasMessageId())
            {
                props.setMessageId("ID:" + messageProps.getMessageId().toString());
            }
            if(messageProps.hasReplyTo())
            {
                ReplyTo replyTo = messageProps.getReplyTo();
                String exchangeName = replyTo.getExchange();
                String routingKey = replyTo.getRoutingKey();
                if(exchangeName == null)
                {
                    exchangeName = "";
                }

                MessageDestination destination = addressSpace.getAttainedMessageDestination(exchangeName);
                Exchange<?> exchange = destination instanceof Exchange ? (Exchange<?>) destination : null;

                String exchangeClass = exchange == null
                                            ? ExchangeDefaults.DIRECT_EXCHANGE_CLASS
                                            : exchange.getType();
                props.setReplyTo(exchangeClass + "://" + exchangeName + "//?routingkey='" + (routingKey == null
                                                                                             ? ""
                                                                                             : routingKey + "'"));

            }
            if(messageProps.hasUserId())
            {
                props.setUserId(new AMQShortString(messageProps.getUserId()));
            }

            if(messageProps.hasApplicationHeaders())
            {
                Map<String, Object> appHeaders = new HashMap<String, Object>(messageProps.getApplicationHeaders());
                if(messageProps.getApplicationHeaders().containsKey("x-jms-type"))
                {
                    props.setType(String.valueOf(appHeaders.remove("x-jms-type")));
                }

                FieldTable ft = new FieldTable();
                for(Map.Entry<String, Object> entry : appHeaders.entrySet())
                {
                    try
                    {
                        ft.put(AMQShortString.validValueOf(entry.getKey()), entry.getValue());
                    }
                    catch (AMQPInvalidClassException e)
                    {
                        // TODO
                        // log here, but ignore - just can;t convert
                    }
                }
                props.setHeaders(ft);

            }
        }

        return props;
    }

    @Override
    public Class<MessageTransferMessage> getInputClass()
    {
        return MessageTransferMessage.class;
    }

    @Override
    public Class<AMQMessage> getOutputClass()
    {
        return AMQMessage.class;
    }

    @Override
    public AMQMessage convert(MessageTransferMessage message, NamedAddressSpace addressSpace)
    {
        return new AMQMessage(convertToStoredMessage(message, addressSpace));
    }

    private StoredMessage<MessageMetaData> convertToStoredMessage(final MessageTransferMessage message,
                                                                  NamedAddressSpace addressSpace)
    {
        final MessageMetaData metaData = convertMetaData(message, addressSpace);
        return new StoredMessage<org.apache.qpid.server.protocol.v0_8.MessageMetaData>()
        {
            @Override
            public MessageMetaData getMetaData()
            {
                return metaData;
            }

            @Override
            public long getMessageNumber()
            {
                return message.getMessageNumber();
            }

            @Override
            public Collection<QpidByteBuffer> getContent(final int offset, final int length)
            {
                return message.getContent(offset, length);
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isInMemory()
            {
                return true;
            }

            @Override
            public boolean flowToDisk()
            {
                return false;
            }
        };
    }

    private MessageMetaData convertMetaData(MessageTransferMessage message, NamedAddressSpace addressSpace)
    {
        return new MessageMetaData(convertPublishBody(message),
                convertContentHeaderBody(message, addressSpace),
                message.getArrivalTime());
    }

    private ContentHeaderBody convertContentHeaderBody(MessageTransferMessage message, NamedAddressSpace addressSpace)
    {
        BasicContentHeaderProperties props = convertContentHeaderProperties(message, addressSpace);
        ContentHeaderBody chb = new ContentHeaderBody(props);
        chb.setBodySize(message.getSize());
        return chb;
    }

    private MessagePublishInfo convertPublishBody(MessageTransferMessage message)
    {
        DeliveryProperties delvProps = message.getHeader().getDeliveryProperties();
        final AMQShortString exchangeName = (delvProps == null || delvProps.getExchange() == null)
                                            ? null
                                            : new AMQShortString(delvProps.getExchange());
        final AMQShortString routingKey = (delvProps == null || delvProps.getRoutingKey() == null)
                                          ? null
                                          : new AMQShortString(delvProps.getRoutingKey());
        final boolean immediate = delvProps != null && delvProps.getImmediate();
        final boolean mandatory = delvProps != null && !delvProps.getDiscardUnroutable();

        return new MessagePublishInfo(exchangeName, immediate, mandatory, routingKey);
    }

    @Override
    public String getType()
    {
        return "v0-10 to v0-8";
    }
}
