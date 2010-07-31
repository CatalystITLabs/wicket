package org.apache.wicket.protocol.https;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Application;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.protocol.https.HttpsConfig;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.WebResponse;

/**
 * Request handler that performs redirects across http and https
 */
public class SwitchProtocolRequestHandler implements IRequestHandler
{

	/**
	 * Protocols
	 */
	public enum Protocol {
		/*** HTTP */
		HTTP,
		/** HTTPS */
		HTTPS,
		/** CURRENT */
		PRESERVE_CURRENT
	}

	/** the protocol this request handler is going to switch to */
	private final Protocol protocol;

	/** the original request handler */
	private final IRequestHandler handler;

	/**
	 * Constructor
	 * 
	 * @param protocol
	 *            required protocol
	 */
	public SwitchProtocolRequestHandler(Protocol protocol)
	{
		this(protocol, null);
	}

	/**
	 * Constructor
	 * 
	 * @param protocol
	 *            required protocol
	 * @param handler
	 *            target to redirect to, or {@code null} to replay the current url
	 */
	public SwitchProtocolRequestHandler(Protocol protocol, IRequestHandler handler)
	{
		if (protocol == null)
		{
			throw new IllegalArgumentException("Argument 'protocol' may not be null.");
		}
		if (protocol == Protocol.PRESERVE_CURRENT)
		{
			throw new IllegalArgumentException("Argument 'protocol' may not have value '" +
				Protocol.PRESERVE_CURRENT.toString() + "'.");
		}
		this.protocol = protocol;
		this.handler = handler;
	}

	/**
	 * Rewrite the url using the specified protocol
	 * 
	 * @param protocol
	 * @param port
	 * @param request
	 * @return url
	 */
	protected String getUrl(String protocol, Integer port, HttpServletRequest request)
	{
		StringBuilder result = new StringBuilder();
		result.append(protocol);
		result.append("://");
		result.append(request.getServerName());
		if (port != null)
		{
			result.append(":");
			result.append(port);
		}
		result.append(request.getRequestURI());
		if (request.getQueryString() != null)
		{
			result.append("?");
			result.append(request.getQueryString());
		}
		return result.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	public void respond(IRequestCycle requestCycle)
	{
		WebRequest webRequest = (WebRequest)requestCycle.getRequest();
		HttpServletRequest request = ((ServletWebRequest)webRequest).getHttpServletRequest();

		final HttpsConfig httpsConfig = Application.get().getSecuritySettings().getHttpsConfig();

		Integer port = null;
		if (protocol == Protocol.HTTP)
		{
			if (httpsConfig.getHttpPort() != 80)
			{
				port = httpsConfig.getHttpPort();
			}
		}
		else if (protocol == Protocol.HTTPS)
		{
			if (httpsConfig.getHttpsPort() != 443)
			{
				port = httpsConfig.getHttpsPort();
			}
		}

		final String url;
		if (handler == null)
		{
			url = getUrl(protocol.toString().toLowerCase(), port, request);
		}
		else
		{
			url = ((RequestCycle)requestCycle).urlFor(handler).toString();
		}

		WebResponse response = (WebResponse)requestCycle.getResponse();

		// an attempt to rewrite a secure jsessionid into nonsecure, doesnt seem to work
		// Session session = Session.get();
		// if (!session.isTemporary())
		// {
		// response.addCookie(new Cookie("JSESSIONID", session.getId()));
		// }

		response.sendRedirect(url);
	}

	/**
	 * Returns a target that can be used to redirect to the specified protocol. If no change is
	 * required {@code null} will be returned.
	 * 
	 * @param protocol
	 *            required protocol
	 * @return request target or {@code null}
	 */
	public static IRequestHandler requireProtocol(Protocol protocol)
	{
		return requireProtocol(protocol, null);
	}

	/**
	 * Returns a target that can be used to redirect to the specified protocol. If no change is
	 * required {@code null} will be returned.
	 * 
	 * @param protocol
	 *            required protocol
	 * @param handler
	 *            request target to redirect to or {@code null} to redirect to current url
	 * @return request handler or {@code null}
	 */
	public static IRequestHandler requireProtocol(Protocol protocol, IRequestHandler handler)
	{
		IRequestCycle requestCycle = RequestCycle.get();
		WebRequest webRequest = (WebRequest)requestCycle.getRequest();
		HttpServletRequest request = ((ServletWebRequest)webRequest).getHttpServletRequest();
		if (protocol == null || protocol == Protocol.PRESERVE_CURRENT ||
			request.getScheme().equals(protocol.toString().toLowerCase()))
		{
			return null;
		}
		else
		{
			return new SwitchProtocolRequestHandler(protocol, handler);
		}
	}

	/** {@inheritDoc} */
	public void detach(IRequestCycle requestCycle)
	{
	}

}