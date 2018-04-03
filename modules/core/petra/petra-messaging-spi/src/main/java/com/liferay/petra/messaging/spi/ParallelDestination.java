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

package com.liferay.petra.messaging.spi;

import com.liferay.petra.concurrent.NoticeableThreadPoolExecutor;
import com.liferay.petra.messaging.api.InboundMessageProcessor;
import com.liferay.petra.messaging.api.Message;
import com.liferay.petra.messaging.api.MessageListener;
import com.liferay.petra.messaging.api.MessageListenerException;
import com.liferay.petra.messaging.api.MessageProcessorException;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Destination that delivers a message to a list of message listeners in
 * parallel.
 * </p>
 *
 * @author Michael C. Han
 * @author Raymond Augé
 */
public class ParallelDestination extends BaseAsyncDestination {

	@Override
	protected void dispatch(
		final Set<? extends MessageListener> messageListeners,
		final Collection<InboundMessageProcessor> inboundMessageProcessors,
		final Message message) {

		final Thread dispatchThread = Thread.currentThread();

		NoticeableThreadPoolExecutor threadPoolExecutor =
			getThreadPoolExecutor();

		for (final MessageListener messageListener : messageListeners) {
			Runnable runnable = new MessageRunnable(message) {

				@Override
				public void run() {
					Message processedMessage = getMessage();

					try {
						for (InboundMessageProcessor processor :
								inboundMessageProcessors) {

							try {
								processedMessage = processor.beforeThread(
									processedMessage, dispatchThread);
							}
							catch (MessageProcessorException mpe) {
								Object[] objects = new Object[3];

								objects[0] = processedMessage;
								objects[1] = dispatchThread;
								objects[2] = mpe;

								_logger.log(
									Level.SEVERE,
									"Unable to process message {0} before " +
										"thread {1}",
									objects);
							}
						}

						messageListener.receive(processedMessage);
					}
					catch (MessageListenerException mle) {
						Object[] objects = new Object[2];

						objects[0] = processedMessage;
						objects[1] = mle;

						_logger.log(
							Level.SEVERE, "Unable to process message {0}",
							objects);
					}
					finally {
						for (InboundMessageProcessor processor :
								inboundMessageProcessors) {

							try {
								processor.afterThread(
									processedMessage, dispatchThread);
							}
							catch (MessageProcessorException mpe) {
								Object[] objects = new Object[3];

								objects[0] = processedMessage;
								objects[1] = dispatchThread;
								objects[2] = mpe;

								_logger.log(
									Level.SEVERE,
									"Unable to process message {0} after" +
										"thread {1}",
									objects);
							}
						}
					}
				}

			};

			threadPoolExecutor.execute(runnable);
		}
	}

	private static final Logger _logger = Logger.getLogger(
		"ParallelDestination");

}