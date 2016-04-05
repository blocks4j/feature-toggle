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

package org.blocks4j.feature.toggle.factory;


import org.apache.commons.lang3.StringUtils;
import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.blocks4j.feature.toggle.exception.FeatureToggleFactoryException;
import org.blocks4j.feature.toggle.proxy.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class FeatureToggleFactory {

    private static Collection<Feature> toggleList = Collections.synchronizedCollection(new HashSet<>());

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureToggleFactory.class);
    private FeatureToggleConfiguration config;

    private FeatureToggleFactory(FeatureToggleConfiguration config) {
        this.config = config;
    }

    public static Collection<Feature> getToggleList() {
        return Collections.unmodifiableCollection(toggleList);
    }

    private <T> T createFeatureProxy(String featureName, Class<? super T> commonInterface, T featureOn, T featureOff) {
        this.validateParams(featureName, commonInterface, featureOn, featureOff);
        Feature<T> feature = new Feature<>();
        feature.setConfig(this.config);
        feature.setCommonsInterface(commonInterface);
        feature.setOn(featureOn);
        feature.setOff(featureOff);
        feature.setName(featureName);
        feature.init();

        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newProxyInstance(commonInterface.getClassLoader(), new Class[]{commonInterface}, feature);

        LOGGER.info(String.format("Feature [%s] initialized , Object for ON is [%s] and Object for OFF is [%s]", featureName, featureOn.getClass().getSimpleName(), featureOff.getClass().getSimpleName()));
        toggleList.add(feature);
        return proxy;
    }

    private void validateParams(String featureName, Class<?> commonInterface, Object featureOn, Object featureOff) {
        if (!this.isFeatureNameValid(featureName)) {
            throw new FeatureToggleFactoryException("The featureName mustn't be null.");
        }
        if (!this.isCommonInterfaceValid(commonInterface)) {
            throw new FeatureToggleFactoryException("The commonInterface must be a inteface.");
        }
        if (!this.isObjectValid(commonInterface, featureOn)) {
            throw new FeatureToggleFactoryException(String.format("The featureOn have to implement [%s] ", commonInterface.getName()));
        }
        if (!this.isObjectValid(commonInterface, featureOff)) {
            throw new FeatureToggleFactoryException(String.format("The featureOff have to implement [%s] ", commonInterface.getName()));
        }
    }

    private boolean isFeatureNameValid(String featureName) {
        return !StringUtils.isEmpty(featureName);
    }

    private boolean isObjectValid(Class<?> commonInterface, Object featureOn) {
        return (commonInterface != null) && (featureOn != null) && commonInterface.isAssignableFrom(featureOn.getClass());
    }

    private boolean isCommonInterfaceValid(Class<?> commonInterface) {
        return (commonInterface != null) && commonInterface.isInterface();
    }

    public static <T> Builder<T> forFeature(FeatureToggleConfiguration config, String featureName, Class<? super T> commonInterface) {
        return new Builder<>(config, featureName, commonInterface);
    }

    public static <T> SwitchableFeatureBuilder<T> forSwitchableFeaturesConfiguration(FeatureToggleConfiguration config, Class<? super T> commonInterface) {
        return new SwitchableFeatureBuilder<>(config, commonInterface);
    }

    public static class Builder<T> {
        private SwitchableFeatureBuilder<T> switchableFeatureBuilder;
        private String featureName;

        private Builder(FeatureToggleConfiguration config, String featureName, Class<? super T> commonInterface) {
            this.switchableFeatureBuilder = new SwitchableFeatureBuilder<>(config, commonInterface);
            this.featureName = featureName;
        }

        public Builder<T> whenEnabled(T featureOn) {
            this.switchableFeatureBuilder.when(this.featureName, featureOn);
            return this;
        }

        public Builder<T> whenDisabled(T featureOff) {
            this.switchableFeatureBuilder.defaultFeature(featureOff);
            return this;
        }

        public T build() {
            return this.switchableFeatureBuilder.build();
        }
    }

    public static class SwitchableFeatureBuilder<T> {
        private FeatureToggleConfiguration config;
        private Class<? super T> commonInterface;
        private T defaultFeatureImpl;
        private LinkedHashMap<String, T> cases;

        public SwitchableFeatureBuilder(FeatureToggleConfiguration config, Class<? super T> commonInterface) {
            this.config = config;
            this.commonInterface = commonInterface;
            this.cases = new LinkedHashMap<>(4);
        }

        public SwitchableFeatureBuilder<T> when(String featureName, T featureImpl) {
            this.cases.put(featureName, featureImpl);
            return this;
        }

        public SwitchableFeatureBuilder<T> defaultFeature(T defaultFeatureImpl) {
            this.defaultFeatureImpl = defaultFeatureImpl;
            return this;
        }

        public T build() {
            if (this.cases.isEmpty()) {
                throw new IllegalStateException();
            }

            FeatureToggleFactory featureToggleFactory = new FeatureToggleFactory(this.config);

            T main = null;
            T next;
            List<Map.Entry<String, T>> featuresInformation = new ArrayList<>(this.cases.entrySet());

            for (int i = featuresInformation.size() - 1; i >= 0; i--) {
                if (main == null) {
                    next = this.defaultFeatureImpl;
                } else {
                    next = main;
                }

                Map.Entry<String, T> featureInformation = featuresInformation.get(i);
                main = featureToggleFactory.createFeatureProxy(featureInformation.getKey(),
                                                               this.commonInterface,
                                                               featureInformation.getValue(),
                                                               next);
            }

            return main;
        }

    }

}
