/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.config.core.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.MBeanException;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.karaf.config.core.ConfigMBean;
import org.apache.karaf.config.core.ConfigRepository;
import org.osgi.service.cm.Configuration;

/**
 * Implementation of the ConfigMBean.
 */
public class ConfigMBeanImpl extends StandardMBean implements ConfigMBean {

    private ConfigRepository configRepo;

    public ConfigMBeanImpl() throws NotCompliantMBeanException {
        super(ConfigMBean.class);
    }

    private Configuration getConfiguration(String pid) throws IOException {
        Configuration configuration = configRepo.getConfigAdmin().getConfiguration(pid, null);
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration PID " + pid + " doesn't exist");
        }
        return configuration;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Dictionary<String, Object> getConfigProperties(String pid) throws IOException {
        Configuration configuration = getConfiguration(pid);

        Dictionary<String, Object> dictionary = configuration.getProperties();
        if (dictionary == null) {
            dictionary = new Hashtable(new java.util.Properties());
        }
        return dictionary;
    }

    /**
     * Get all config pids
     */
    @Override
    public List<String> getConfigs() throws MBeanException {
        try {
            Configuration[] configurations = this.configRepo.getConfigAdmin().listConfigurations(null);
            List<String> pids = new ArrayList<>();
            for (int i = 0; i < configurations.length; i++) {
                pids.add(configurations[i].getPid());
            }
            return pids;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void create(String pid) throws MBeanException {
        try {
            configRepo.update(pid, new Hashtable<>());
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void delete(String pid) throws MBeanException {
        try {
            this.configRepo.delete(pid);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map<String, String> listProperties(String pid) throws MBeanException {
        try {
            Dictionary dictionary = getConfigProperties(pid);

            Map<String, String> propertiesMap = new HashMap<>();
            for (Enumeration e = dictionary.keys(); e.hasMoreElements(); ) {
                Object key = e.nextElement();
                Object value = dictionary.get(key);
                propertiesMap.put(key.toString(), value.toString());
            }
            return propertiesMap;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void deleteProperty(String pid, String key) throws MBeanException {
        try {
            Dictionary<String, Object> dictionary = getConfigProperties(pid);
            dictionary.remove(key);
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void appendProperty(String pid, String key, String value) throws MBeanException {
        try {
            Dictionary<String, Object> dictionary = getConfigProperties(pid);
            Object currentValue = dictionary.get(key);
            if (currentValue == null) {
                dictionary.put(key, value);
            } else if (currentValue instanceof String) {
                dictionary.put(key, currentValue + value);
            } else {
                throw new IllegalStateException("Current value is not a String");
            }
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void setProperty(String pid, String key, String value) throws MBeanException {
        try {
            Dictionary<String, Object> dictionary = getConfigProperties(pid);
            dictionary.put(key, value);
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public String getProperty(String pid, String key) throws MBeanException {
        try {
            Dictionary<String, Object> dictionary = getConfigProperties(pid);
            Object value = dictionary.get(key);
            if (value != null) {
                return value.toString();
            }
            return null;
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

    @Override
    public void update(String pid, Map<String, String> properties) throws MBeanException {
        try {
            if (properties == null) {
                properties = new HashMap<>();
            }
            Dictionary<String, Object> dictionary = toDictionary(properties);
            configRepo.update(pid, dictionary);
        } catch (Exception e) {
            throw new MBeanException(null, e.toString());
        }
    }

	private Dictionary<String, Object> toDictionary(
			Map<String, String> properties) {
		Dictionary<String, Object> dictionary = new Hashtable<>();
		for (String key : properties.keySet()) {
		    dictionary.put(key, properties.get(key));
		}
		return dictionary;
	}


    public void setConfigRepo(ConfigRepository configRepo) {
        this.configRepo = configRepo;
    }

	@Override
	public String createFactoryConfiguration(String factoryPid,
			Map<String, String> properties) throws MBeanException {
		Dictionary<String, Object> dict = toDictionary(properties);
		return configRepo.createFactoryConfiguration(factoryPid, dict);
	}

}
