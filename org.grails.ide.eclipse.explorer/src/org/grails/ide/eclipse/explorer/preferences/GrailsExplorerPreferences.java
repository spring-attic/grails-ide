/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.grails.ide.eclipse.explorer.preferences;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.grails.ide.eclipse.core.GrailsCoreActivator;
import org.osgi.service.prefs.BackingStoreException;

import org.grails.ide.eclipse.explorer.GrailsExplorerPlugin;

/**
 * @author Kris De Volder
 *
 * @since 2.8
 */
public class GrailsExplorerPreferences {
	
	private static GrailsExplorerPreferences instance;
	private IEclipsePreferences store = new InstanceScope().getNode(GrailsExplorerPlugin.PLUGIN_ID);
	
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);
	private OrderingConfig orderingConfig; //Cached copy of orderingConfig, it is somewhat expensive to initialise.
	
	public interface Listener {
		/**
		 * Called when the orderingConfig was changed.
		 */
		void orderingChanged(OrderingConfig newConfig);
	}

	private static final String ORDERING_CONFIG = GrailsExplorerPlugin.PLUGIN_ID+".ordering";

	/**
	 * Use 'getInstance' this class is a singleton.
	 */
	private GrailsExplorerPreferences() {
	}
	
	public static GrailsExplorerPreferences getInstance() {
		if (instance == null) {
			instance = new GrailsExplorerPreferences();
		}
		return instance;
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener l) {
		listeners.remove(l);
	}

	public OrderingConfig getOrderingConfig() {
		if (orderingConfig==null) {
			//Try to read a stored config
			String encoded = store.get(ORDERING_CONFIG, (String)null);
			if (encoded!=null) {
				orderingConfig = OrderingConfig.fromSaveString(encoded);
			} else {
				orderingConfig = OrderingConfig.DEFAULT;
			}
		}
		return orderingConfig;
	}
	
	public void setOrderingConfig(OrderingConfig config) {
		orderingConfig = config;
		if (config==null) {
			store.remove(ORDERING_CONFIG);
		} else {
			store.put(ORDERING_CONFIG, config.toSaveString());
		}
		try {
			store.flush();
		} catch (BackingStoreException e) {
			GrailsCoreActivator.log(e);
		}
		notifyListeners(config);
	}

	private void notifyListeners(OrderingConfig newConfig) {
		for (Object o : listeners.getListeners()) {
			Listener l = (Listener) o;
			l.orderingChanged(newConfig);
		}
	}

}
