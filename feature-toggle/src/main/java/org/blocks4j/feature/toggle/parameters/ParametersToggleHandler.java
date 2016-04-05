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

package org.blocks4j.feature.toggle.parameters;

import org.blocks4j.feature.toggle.FeatureToggleConfiguration;
import org.blocks4j.feature.toggle.annotation.parameters.ParameterToggle;
import org.blocks4j.feature.toggle.converter.TypeConverter;
import org.blocks4j.feature.toggle.domain.TogglableParameter;
import org.blocks4j.feature.toggle.exception.ParamtersToggleFactoryException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParametersToggleHandler {

    private static final String PARAM_BY_FEATURE = "%s&%s";
    private static final TypeConverter CONVERTER = new TypeConverter();

    private FeatureToggleConfiguration config;
    private Map<Method, List<TogglableParameter>> paramsMapOfList = new HashMap<>();


    public ParametersToggleHandler(FeatureToggleConfiguration config, Class<?> commonInterface) {
        this.loadParamtersTogglable(commonInterface);
        this.config = config;
    }

    private void loadParamtersTogglable(Class<?> commonInterface) {
        for (Method method : commonInterface.getDeclaredMethods()) {
            this.containsAnnotationOnParameters(method);
            this.containsAnnotationOnField(method);
        }
    }

    public boolean handle(Method method, Object[] args, String featureName) {
        if (!this.paramsMapOfList.containsKey(method)) {
            return true;
        }
        for (TogglableParameter togglableParameter : this.paramsMapOfList.get(method)) {
            if (!this.isParamOn(args, featureName, togglableParameter)) {
                return false;
            }
        }
        return true;
    }

    private boolean isParamOn(Object[] args, String featureName, TogglableParameter togglableParameter) {
        Collection<String> configuredParameters = this.getParamtersConfigured(togglableParameter);
        if (configuredParameters.isEmpty()) {
            configuredParameters = this.getParamtersByFeatureConfigured(featureName, togglableParameter);
            if (configuredParameters.isEmpty()) {
                return true;
            }
        }
        return this.containsParameters(args[togglableParameter.getIndex()], togglableParameter, configuredParameters);
    }

    private boolean containsParameters(Object arg, TogglableParameter togglableParameter, Collection<String> configuredParameters) {
        for (String paramter : configuredParameters) {
            if (togglableParameter.isAnnotatedOnField()) {
                try {
                    if (this.compareParams(togglableParameter.getMethod().invoke(arg), paramter)) {
                        return true;
                    }
                } catch (Exception e) {
                    throw new ParamtersToggleFactoryException(String.format("The method [%s] must be implemented! ", togglableParameter.getMethod().getName()), e);
                }
            } else {
                if (this.compareParams(arg, paramter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void containsAnnotationOnField(Method method) {
        int index = 0;
        for (Class<?> param : method.getParameterTypes()) {
            for (Field field : param.getDeclaredFields()) {
                Annotation annotation = field.getAnnotation(ParameterToggle.class);
                if (annotation != null) {
                    String methodName = String.format("get%s%s", field.getName().toUpperCase().charAt(0), field.getName().substring(1));
                    try {
                        Method getField = param.getDeclaredMethod(methodName);
                        if (getField != null) {
                            this.put(method, new TogglableParameter(index, ParameterToggle.class.cast(annotation).value(), getField));
                        } else {
                            throw new ParamtersToggleFactoryException(String.format("The method [%s] for attribute [%s] must be implemented !", methodName, field.getName()));
                        }
                    } catch (Exception e) {
                        throw new ParamtersToggleFactoryException(String.format("The method [%s] for attribute [%s] must be implemented !", methodName, field.getName()), e);
                    }
                }
            }
            index++;
        }
    }

    private void containsAnnotationOnParameters(Method method) {
        int index = 0;
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation annotation : parameterAnnotations) {
                if (annotation.annotationType().equals(ParameterToggle.class)) {
                    this.put(method, new TogglableParameter(index, ParameterToggle.class.cast(annotation).value()));
                }
            }
            index++;
        }
    }

    private void put(Method method, TogglableParameter togglableParameter) {
        if (this.paramsMapOfList.containsKey(method)) {
            List<TogglableParameter> list = this.paramsMapOfList.get(method);
            list.add(togglableParameter);
            this.paramsMapOfList.put(method, list);
        } else {
            List<TogglableParameter> list = new ArrayList<>();
            list.add(togglableParameter);
            this.paramsMapOfList.put(method, list);
        }
    }

    private Collection<String> getParamtersConfigured(TogglableParameter togglableParameter) {
        return this.getConfigured(togglableParameter.getId());
    }

    private Collection<String> getParamtersByFeatureConfigured(String featureName, TogglableParameter togglableParameter) {
        return this.getConfigured(String.format(PARAM_BY_FEATURE, featureName, togglableParameter.getId()));
    }

    private Collection<String> getConfigured(String key) {
        Collection<String> paramtersConfigured = this.config.getEnabledParameters().get(key);
        if ((paramtersConfigured == null) || paramtersConfigured.isEmpty()) {
            return Collections.emptyList();
        }
        return paramtersConfigured;
    }

    private boolean compareParams(Object param, String paramConfigured) {
        return param != null && param.equals(CONVERTER.convertToType(paramConfigured, param.getClass()));
    }
}