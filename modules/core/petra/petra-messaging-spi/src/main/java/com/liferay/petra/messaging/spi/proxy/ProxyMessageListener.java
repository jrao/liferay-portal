/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.petra.messaging.spi.proxy;

import com.liferay.petra.messaging.api.Message;
import com.liferay.petra.messaging.api.MessageBus;
import com.liferay.petra.messaging.api.MessageListener;
import com.liferay.petra.messaging.spi.MessageImpl;
import com.liferay.petra.string.StringBundler;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Micha Kiener
 * @author Michael C. Han
 * @author Brian Wing Shun Chan
 * @author Igor Spasic
 */
public class ProxyMessageListener implements MessageListener {

	@Override
	public void receive(Message message) {
		ProxyResponse proxyResponse = new ProxyResponse();

		try {
			Object payload = message.getPayload();

			if (payload == null) {
				throw new Exception("Payload is null");
			}
			else if (!(payload instanceof ProxyRequest)) {
				Class<?> payloadClass = payload.getClass();

				String payloadClassName = payloadClass.getName();

				String errorMessage = StringBundler.concat(
					"Payload ", payloadClassName, " is not of type ",
					ProxyRequest.class.getName());

				throw new Exception(errorMessage);
			}
			else {
				ProxyRequest proxyRequest = (ProxyRequest)payload;

				Object result = proxyRequest.execute(_manager);

				proxyResponse.setResult(result);
			}
		}
		catch (Exception e) {
			proxyResponse.setException(e);
		}
		finally {
			String responseDestinationName =
				message.getResponseDestinationName();

			Exception proxyResponseException = proxyResponse.getException();

			if ((responseDestinationName != null) &&
				!responseDestinationName.isEmpty()) {

				Message responseMessage = new MessageImpl();

				responseMessage.setDestinationName(
					message.getResponseDestinationName());
				responseMessage.setResponseId(message.getResponseId());

				responseMessage.setPayload(proxyResponse);

				if (proxyResponseException != null) {
					_logger.log(
						Level.FINE, proxyResponseException.getMessage(),
						proxyResponseException);
				}

				_messageBus.sendMessage(
					responseDestinationName, responseMessage);
			}
			else {
				if (proxyResponseException != null) {
					_logger.log(
						Level.WARNING, proxyResponseException.getMessage(),
						proxyResponseException);
				}

				message.setResponse(proxyResponse);
			}
		}
	}

	public void setManager(Object manager) {
		_manager = manager;
	}

	public void setMessageBus(MessageBus messageBus) {
		_messageBus = messageBus;
	}

	private static final Logger _logger = Logger.getLogger(
		"ProxyMessageListener");

	private Object _manager;
	private MessageBus _messageBus;

}