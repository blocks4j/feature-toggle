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

package org.blocks4j.feature.toggle.spring.scheme;

import org.apache.commons.lang3.StringUtils;
import org.blocks4j.feature.toggle.exception.FeatureToggleDefinitionParsingException;
import org.blocks4j.feature.toggle.spring.factory.FeatureToggleBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import java.util.UUID;

public class FeatureToggleDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String TAG_ONREF_NAME = "onRef";
    private static final String TAG_OFFREF_NAME = "offRef";
    private static final String TAG_TOGGLE_NAME = "toggle";
    private static final String TAG_ON_REF_NAME = "on-ref";
    private static final String TAG_OFF_REF_NAME = "off-ref";
    private static final String TAG_FEATURENAME = "featureName";
    private static final String TAG_FEATURE_NAME = "feature-name";
    private static final String TAG_COMMONINTERFACE_NAME = "commonInterface";
    private static final String TAG_COMMON_INTERFACE_NAME = "common-interface";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureToggleDefinitionParser.class);

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext context) {
        AbstractBeanDefinition result = null;
        if (DomUtils.nodeNameEquals(element, TAG_TOGGLE_NAME)) {
            BeanDefinitionHolder holder = context.getDelegate().parseBeanDefinitionElement(element);
            result = (AbstractBeanDefinition) holder.getBeanDefinition();

            String feature = element.getAttribute(TAG_FEATURE_NAME);
            String commonInterfaceName = element.getAttribute(TAG_COMMON_INTERFACE_NAME);
            String onRefName = element.getAttribute(TAG_ON_REF_NAME);
            String offRefName = element.getAttribute(TAG_OFF_REF_NAME);

            if (StringUtils.isEmpty(onRefName)) {
                throw new FeatureToggleDefinitionParsingException("You must specify on-ref");
            }
            if (StringUtils.isEmpty(offRefName)) {
                throw new FeatureToggleDefinitionParsingException("You must specify on-ref");
            }
            Class<?> commonInterface;
            try {
                commonInterface = Class.forName(commonInterfaceName);
            } catch (Exception e) {
                LOGGER.error("It was not possible to find interface of type '{}'", commonInterfaceName);
                throw new FeatureToggleDefinitionParsingException(String.format("It was not possible to find interface of type '%s'", commonInterfaceName), e);
            }
            String fbeanName = feature.concat("_" + this.randomUUID() + "_factory");
            this.buildNewFactoryBean(fbeanName, context, onRefName, offRefName, commonInterface, feature);
            result.setFactoryBeanName(fbeanName);
            result.setFactoryMethodName(TAG_TOGGLE_NAME);
            context.getRegistry().registerBeanDefinition(TAG_TOGGLE_NAME.concat(Integer.toHexString(this.hashCode())), result);
        }
        return result;
    }

    private String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void buildNewFactoryBean(String factoryBeanName, ParserContext context, String onRefName, String offRefName,
                                     Class<?> commonInterface, String feature) {
        BeanDefinitionRegistry factory = context.getReaderContext().getReader().getBeanFactory();
        if (!factory.isBeanNameInUse(factoryBeanName)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FeatureToggleBeanFactory.class);
            builder.addPropertyValue(TAG_ONREF_NAME, onRefName);
            builder.addPropertyValue(TAG_OFFREF_NAME, offRefName);
            builder.addPropertyValue(TAG_FEATURENAME, feature);
            builder.addPropertyValue(TAG_COMMONINTERFACE_NAME, commonInterface);
            factory.registerBeanDefinition(factoryBeanName, builder.getBeanDefinition());
        } else {
            throw new RuntimeException(String.format("A bean with name '%s' already exists in the application context.", factoryBeanName));
        }
    }
}