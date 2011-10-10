/* 
 * Copyright (c) 2008-2010, Hazel Ltd. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hazelcast.hibernate.instance;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;

import org.hibernate.cache.CacheException;
import org.hibernate.util.ConfigHelper;
import org.hibernate.util.StringHelper;

import com.hazelcast.config.Config;
import com.hazelcast.config.UrlXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.DuplicateInstanceNameException;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.hibernate.CacheEnvironment;
import com.hazelcast.impl.GroupProperties;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;

class HazelcastInstanceLoader implements IHazelcastInstanceLoader {

	private final static ILogger logger = Logger.getLogger(HazelcastInstanceFactory.class.getName());
	
	private final Properties props = new Properties();
	private boolean useSuperClient = false;
	private boolean staticInstance = false;
	private String instanceName = null;
	private HazelcastInstance instance;
	private Config config = null;
	
	public void configure(Properties props) {
		this.props.putAll(props);
	}

	public HazelcastInstance loadInstance()	throws CacheException {
		if(instance != null && instance.getLifecycleService().isRunning()) {
			logger.log(Level.WARNING, "Current HazelcastInstance is already loaded and running! " +
					"Returning current instance...");
			return instance;
		}
		
		String configResourcePath = null;

		if (props != null) {
			instanceName = CacheEnvironment.getInstanceName(props);
			useSuperClient = CacheEnvironment.isSuperClient(props);
			configResourcePath = CacheEnvironment.getConfigFilePath(props);
		}
		

		if (useSuperClient) {
			logger.log(Level.WARNING,
					"Creating Hazelcast node as Super-Client. "
					+ "Be sure this node has access to an already running cluster...");
		}

		if (StringHelper.isEmpty(configResourcePath)) {
			// If HazelcastInstance will not be super-client
			// then just return default instance. 
			// We do not need to edit configuration. 
			if(!useSuperClient) {
				staticInstance = true;
			}
		} else {
			URL url = ConfigHelper.locateConfig(configResourcePath);
			try {
				config = new UrlXmlConfig(url);
			} catch (IOException e) {
				throw new CacheException(e);
			}
		}
		
		if(instanceName != null) {
			instance = Hazelcast.getHazelcastInstanceByName(instanceName);
			if(instance == null) {
				try {
					createOrGetInstance();
				} catch (DuplicateInstanceNameException ignored) {
					instance = Hazelcast.getHazelcastInstanceByName(instanceName);		
				}
			}
		} else {
			createOrGetInstance();
		}
		return instance;
	}
	
	private void createOrGetInstance() throws DuplicateInstanceNameException {
		if(staticInstance) {
			instance = Hazelcast.getDefaultInstance();
		}
		else {
			if(config == null) {
				config = new XmlConfigBuilder().build();
			}
			config.setSuperClient(useSuperClient);
			instance = Hazelcast.newHazelcastInstance(config);
		}
	}

	public void unloadInstance() throws CacheException {
		if(instance == null) {
			return;
		}
		final boolean shutDown = CacheEnvironment.shutdownOnStop(props, (instanceName == null));
		if(!shutDown) {
			logger.log(Level.WARNING, CacheEnvironment.SHUTDOWN_ON_STOP + " property is set to 'false'. " +
					"Leaving current HazelcastInstance active! (Warning: Do not disable Hazelcast " 
					+ GroupProperties.PROP_SHUTDOWNHOOK_ENABLED + " property!)");
			return;
		}
		try {
			instance.getLifecycleService().shutdown();
			instance = null;
		} catch (Exception e) {
			throw new CacheException(e);
		}
	}
}
