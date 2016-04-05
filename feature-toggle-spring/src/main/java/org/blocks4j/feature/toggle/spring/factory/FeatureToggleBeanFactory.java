/*
 *   Copyright 2013-2016 Blocks4J Team (www.blocks4j.org)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.blocks4j.feature.toggle.spring.factory;

import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.blocks4j.feature.toggle.spring.exception.FeatureToggleBeanFactoryException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FeatureToggleBeanFactory<T> implements ApplicationContextAware {

    private static final String PROPERTY_PREFIX = "set";

    private FeatureToggleConfiguration config;

    private String onRef;

    private String offRef;

    private Class<T> commonInterface;

    private String featureName;

    private ApplicationContext context;

    public T toggle() {
        return org.blocks4j.feature.toggle.factory.FeatureToggleFactory
                .forFeature(this.config, this.getFeatureName(), this.commonInterface)
                .whenEnabled(this.getBeanOn())
                .whenDisabled(this.getBeanOff())
                .build();
    }

    public T getBeanOff() {
        return this.getContext().getBean(this.getOffRef(), this.commonInterface);
    }

    public T getBeanOn() {
        return this.getContext().getBean(this.getOnRef(), this.commonInterface);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
        FeatureToggleConfiguration config = context.getBean(FeatureToggleConfiguration.class);
        if (config == null) {
            throw new FeatureToggleBeanFactoryException("FeatureToggleConfiguration not found on context.");
        }
        this.config = config;
    }

    public FeatureToggleConfiguration getConfig() {
        return this.config;
    }

    public void setConfig(FeatureToggleConfiguration config) {
        this.config = config;
    }

    public String getFeatureName() {
        return this.featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public String getOnRef() {
        return this.onRef;
    }

    public void setOnRef(String onRef) {
        this.onRef = onRef;
    }

    public String getOffRef() {
        return this.offRef;
    }

    public void setOffRef(String offRef) {
        this.offRef = offRef;
    }

    public ApplicationContext getContext() {
        return this.context;
    }

    public Class<? super T> getCommonInterface() {
        return this.commonInterface;
    }

    public void setCommonInterface(Class<T> commonInterface) {
        this.commonInterface = commonInterface;
    }
}
