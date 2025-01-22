/*
 * Copyright (c) 2010-2012 Engine Yard, Inc.
 * Copyright (c) 2007-2009 Sun Microsystems, Inc.
 * This source code is available under the MIT license.
 * See the file LICENSE.txt for details.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jruby.rack.mock;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

/**
 * Mock implementation of the {@link javax.servlet.http.HttpSession} interface.
 *
 * <p>Compatible with Servlet 2.5 as well as Servlet 3.0.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Mark Fisher
 */
@SuppressWarnings("deprecation")
public class MockHttpSession implements HttpSession {

	private static int nextId = 1;

	private String id;

	private final long creationTime = System.currentTimeMillis();

	private int maxInactiveInterval;

	private long lastAccessedTime = System.currentTimeMillis();

	private final ServletContext servletContext;

	private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

	private boolean invalid = false;

	private boolean isNew = true;


	/**
	 * Create a new MockHttpSession with a default {@link MockServletContext}.
	 * 
	 * @see MockServletContext
	 */
	public MockHttpSession() {
		this(null);
	}

	/**
	 * Create a new MockHttpSession.
	 * 
	 * @param servletContext the ServletContext that the session runs in
	 */
	public MockHttpSession(ServletContext servletContext) {
		this(servletContext, null);
	}

	/**
	 * Create a new MockHttpSession.
	 * 
	 * @param servletContext the ServletContext that the session runs in
	 * @param id a unique identifier for this session
	 */
	public MockHttpSession(ServletContext servletContext, String id) {
		this.servletContext = servletContext;
		this.id = (id != null ? id : Integer.toString(nextId++));
	}

	public long getCreationTime() throws IllegalStateException {
        checkInvalid("getCreationTime()");
		return this.creationTime;
	}

	public String getId() {
		return this.id;
	}

    public void setId(String id) {
        this.id = id;
    }
    
	public void access() {
		this.lastAccessedTime = System.currentTimeMillis();
		this.isNew = false;
	}

	public long getLastAccessedTime() throws IllegalStateException {
        checkInvalid("getLastAccessedTime()");
		return this.lastAccessedTime;
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	public HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException("getSessionContext");
	}

	public Object getAttribute(String name) throws IllegalStateException {
        checkInvalid("getAttribute("+ name +")");
		return this.attributes.get(name);
	}

	public Object getValue(String name) {
		return getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() throws IllegalStateException {
        checkInvalid("getAttributeNames()");
		return new Vector<String>(this.attributes.keySet()).elements();
	}

	public String[] getValueNames() throws IllegalStateException {
        checkInvalid("getValueNames()");
		return this.attributes.keySet().toArray(new String[this.attributes.size()]);
	}

	public void setAttribute(String name, Object value) throws IllegalStateException {
        checkInvalid("setAttribute("+ name + ")");
		if (value != null) {
			this.attributes.put(name, value);
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueBound(new HttpSessionBindingEvent(this, name, value));
			}
		}
		else {
			removeAttribute(name);
		}
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String name) throws IllegalStateException {
        checkInvalid("removeAttribute("+ name + ")");
		Object value = this.attributes.remove(name);
		if (value instanceof HttpSessionBindingListener) {
			((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
		}
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	/**
	 * Clear all of this session's attributes.
	 */
	public void clearAttributes() {
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof HttpSessionBindingListener) {
				((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
			}
		}
	}

	public void invalidate() throws IllegalStateException {
        checkInvalid("invalidate()");
		this.invalid = true;
		clearAttributes();
	}

	public boolean isInvalid() {
		return this.invalid;
	}

	public void setNew(boolean value) {
		this.isNew = value;
	}

	public boolean isNew() throws IllegalStateException {
        checkInvalid("isNew()");
		return this.isNew;
	}

	/**
	 * Serialize the attributes of this session into an object that can be
	 * turned into a byte array with standard Java serialization.
	 * 
	 * @return a representation of this session's serialized state
	 */
	public Serializable serializeState() {
		HashMap<String, Serializable> state = new HashMap<String, Serializable>();
		for (Iterator<Map.Entry<String, Object>> it = this.attributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Object> entry = it.next();
			String name = entry.getKey();
			Object value = entry.getValue();
			it.remove();
			if (value instanceof Serializable) {
				state.put(name, (Serializable) value);
			}
			else {
				// Not serializable... Servlet containers usually automatically
				// unbind the attribute in this case.
				if (value instanceof HttpSessionBindingListener) {
					((HttpSessionBindingListener) value).valueUnbound(new HttpSessionBindingEvent(this, name, value));
				}
			}
		}
		return state;
	}

	/**
	 * Deserialize the attributes of this session from a state object created by
	 * {@link #serializeState()}.
	 * 
	 * @param state a representation of this session's serialized state
	 */
	@SuppressWarnings("unchecked")
	public void deserializeState(Serializable state) {
		this.attributes.putAll((Map<String, Object>) state);
	}

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + this.attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    private void checkInvalid(String method) throws IllegalStateException {
        if ( invalid == true ) {
            if ( method == null ) method = "";
            throw new IllegalStateException(method + ": session already invalidated");
        }
    }
    
}
