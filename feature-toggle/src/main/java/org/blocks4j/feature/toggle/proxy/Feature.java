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

package org.blocks4j.feature.toggle.proxy;

import org.apache.commons.collections4.CollectionUtils;
import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.blocks4j.feature.toggle.parameters.ParametersToggleHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Feature<T> implements InvocationHandler {

    private ParametersToggleHandler paramters;
    private FeatureToggleConfiguration config;
    private Class<? super T> commonInterface;
    private String featureName;
    private T featureOff;
    private T featureOn;

    public void init() {
        this.paramters = new ParametersToggleHandler(this.config, this.commonInterface);
    }

    public void setName(String featureName) {
        this.featureName = featureName;
    }

    public void setOff(T featureOff) {
        this.featureOff = featureOff;
    }

    public void setOn(T featureOn) {
        this.featureOn = featureOn;
    }

    public void setCommonsInterface(Class<? super T> commonInterface) {
        this.commonInterface = commonInterface;
    }

    public void setConfig(FeatureToggleConfiguration config) {
        this.config = config;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object _this;
        if (this.isOn(method, args)) {
            _this = this.featureOn;
        } else {
            _this = this.featureOff;
        }
        try {
            return method.invoke(_this, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private boolean isOn(Method method, Object[] args) {
        if (this.isFeatureOnFeatureList()) {
            if (args != null) {
                return this.paramters.handle(method, args, this.featureName);
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isFeatureOnFeatureList() {
        return CollectionUtils.isNotEmpty(this.config.getEnabledFeatures()) && this.config.getEnabledFeatures().contains(this.featureName);
    }

    public String getFeatureName() {
        return this.featureName;
    }

    public T getFeatureOff() {
        return this.featureOff;
    }

    public T getFeatureOn() {
        return this.featureOn;
    }
}