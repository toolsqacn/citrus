/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.jms;

import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.messaging.AbstractSelectiveMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class JmsConsumer extends AbstractSelectiveMessageConsumer {

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(JmsConsumer.class);

    /** Endpoint configuration */
    private JmsEndpoint endpoint;

    /**
     * Default constructor using endpoint.
     * @param endpoint
     */
    public JmsConsumer(JmsEndpoint endpoint) {
        super(endpoint.getEndpointConfiguration().getTimeout());
        this.endpoint = endpoint;
    }

    @Override
    public Message<?> receive(String selector, long timeout) {
        String destinationName;

        if (StringUtils.hasText(selector)) {
            destinationName = endpoint.getEndpointConfiguration().getDefaultDestinationName() + "(" + selector + ")'";
        } else {
            destinationName = endpoint.getEndpointConfiguration().getDefaultDestinationName();
        }

        log.info("Waiting for JMS message on destination: '" + destinationName);

        endpoint.getEndpointConfiguration().getJmsTemplate().setReceiveTimeout(timeout);
        Object receivedObject = null;

        if (StringUtils.hasText(selector)) {
            receivedObject = endpoint.getEndpointConfiguration().getJmsTemplate().receiveSelectedAndConvert(selector);
        } else {
            receivedObject = endpoint.getEndpointConfiguration().getJmsTemplate().receiveAndConvert();
        }

        if (receivedObject == null) {
            throw new ActionTimeoutException("Action timed out while receiving JMS message on '" + destinationName);
        }

        Message<?> receivedMessage;
        if (receivedObject instanceof Message<?>) {
            receivedMessage = (Message<?>)receivedObject;
        } else {
            receivedMessage = new GenericMessage<Object>(receivedObject);
        }

        log.info("Received JMS message on destination: '" + destinationName);

        onInboundMessage(receivedMessage);

        return receivedMessage;
    }

    /**
     * Informs message listeners if present.
     * @param receivedMessage
     */
    protected void onInboundMessage(Message<?> receivedMessage) {
        if (endpoint.getMessageListener() != null) {
            endpoint.getMessageListener().onInboundMessage((receivedMessage != null ? receivedMessage.toString() : ""));
        } else {
            log.debug("Received message is:" + System.getProperty("line.separator") + (receivedMessage != null ? receivedMessage.toString() : ""));
        }
    }

}
