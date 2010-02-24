/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.markup.transformer;

import org.apache.wicket.Component;
import org.apache.wicket.Response;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.ng.request.cycle.RequestCycle;
import org.apache.wicket.response.StringResponse;

/**
 * A IBehavior which can be added to any component. It allows to post-process (transform) the markup
 * generated by the component.
 * 
 * @see org.apache.wicket.markup.transformer.AbstractOutputTransformerContainer
 * 
 * @author Juergen Donnerstag
 */
public abstract class AbstractTransformerBehavior extends AbstractBehavior implements ITransformer
{
	private static final long serialVersionUID = 1L;

	private Response webResponse;

	/**
	 * Construct.
	 */
	public AbstractTransformerBehavior()
	{
	}

	/**
	 * Create a new response object which is used to store the markup generated by the child
	 * objects.
	 * 
	 * @return Response object. Must not be null
	 */
	protected Response newResponse()
	{
		return new StringResponse();
	}

	/**
	 * @see org.apache.wicket.behavior.IBehavior#onComponentTag(org.apache.wicket.Component,
	 *      org.apache.wicket.markup.ComponentTag)
	 */
	@Override
	public void onComponentTag(final Component component, final ComponentTag tag)
	{
		tag.put("xmlns:wicket", "http://wicket.apache.org");
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#beforeRender(org.apache.wicket.Component)
	 */
	@Override
	public void beforeRender(Component component)
	{
		super.beforeRender(component);

		final RequestCycle requestCycle = RequestCycle.get();

		// Temporarily replace the web response with a String response
		webResponse = requestCycle.getResponse();

		// Create a new response object
		final Response response = newResponse();
		if (response == null)
		{
			throw new IllegalStateException("newResponse() must not return null");
		}

		// and make it the current one
		requestCycle.setResponse(response);
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#onRendered(org.apache.wicket.Component)
	 */
	@Override
	public void onRendered(final Component component)
	{
		final RequestCycle requestCycle = RequestCycle.get();

		try
		{
			Response response = requestCycle.getResponse();

			// Transform the data
			CharSequence output = transform(component, response.toString());
			webResponse.write(output);
		}
		catch (Exception ex)
		{
			throw new WicketRuntimeException("Error while transforming the output: " + this, ex);
		}
		finally
		{
			// Restore the original response object
			requestCycle.setResponse(webResponse);
		}
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#cleanup()
	 */
	@Override
	public void cleanup()
	{
		webResponse = null;
	}

	/**
	 * @see org.apache.wicket.behavior.AbstractBehavior#onException(org.apache.wicket.Component,
	 *      java.lang.RuntimeException)
	 */
	@Override
	public void onException(Component component, RuntimeException exception)
	{
		if (webResponse != null)
		{
			final RequestCycle requestCycle = RequestCycle.get();
			requestCycle.setResponse(webResponse);
		}
	}

	/**
	 * 
	 * @see org.apache.wicket.markup.transformer.ITransformer#transform(org.apache.wicket.Component,
	 *      CharSequence)
	 */
	public abstract CharSequence transform(final Component component, final CharSequence output)
		throws Exception;
}