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

import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class ParametersToggleHandler {

    private static final String PARAM_BY_FEATURE = "%s&%s";
    private static final TypeConverter CONVERTER = new TypeConverter();
    private static final HashSet<Class<?>> ALLOWED_PARAMETER_TOGGLE = Sets.<Class<?>>newHashSet(Character.class,
                                                                                                Byte.class,
                                                                                                Short.class,
                                                                                                Integer.class,
                                                                                                Long.class,
                                                                                                Boolean.class,
                                                                                                String.class);

    private FeatureToggleConfiguration config;
    private Map<Method, List<TogglableParameter>> paramsMethodsCache = new HashMap<Method, List<TogglableParameter>>();


    public ParametersToggleHandler(FeatureToggleConfiguration config, Class<?> commonInterface) {
        this.loadParamtersTogglable(commonInterface);
        this.config = config;
    }

    private void loadParamtersTogglable(Class<?> commonInterface) {
        for (Method method : commonInterface.getDeclaredMethods()) {
            Collection<TogglableParameter<?>> togglableParameters = new ArrayList<TogglableParameter<?>>();

            this.extractAnnotatedToggleParametersOnPrimitiveMethodParameters(method, togglableParameters);
            this.extractAnnotatedToggleParametersOnComplexMethodParameters(method, togglableParameters);

            this.put(method, togglableParameters);
        }
    }

    public boolean isOn(Method method, Object[] args, String featureName) {
        List<TogglableParameter> togglableParameters = this.paramsMethodsCache.get(method);
        if (CollectionUtils.isEmpty(togglableParameters)) {
            return true;
        }

        for (TogglableParameter togglableParameter : togglableParameters) {
            if (!this.isParamOn(args, featureName, togglableParameter)) {
                return false;
            }
        }

        return true;
    }

    private boolean isParamOn(Object[] args, String featureName, TogglableParameter togglableParameter) {
        Collection<String> allowedParameters = this.getParamtersConfigured(togglableParameter);
        if (allowedParameters.isEmpty()) {
            allowedParameters = this.getParamtersByFeatureConfigured(featureName, togglableParameter);
            if (allowedParameters.isEmpty()) {
                return true;
            }
        }
        return this.validateFeatureToggleParameters(args[togglableParameter.getIndex()], togglableParameter, allowedParameters);
    }

    private boolean validateFeatureToggleParameters(Object arg, TogglableParameter togglableParameter, Collection<String> allowedParameters) {
        Object togglableParameterValue = this.getTogglableParameterValue(togglableParameter, arg);

        return allowedParameters.contains(CONVERTER.convertToString(togglableParameterValue));
    }

    @SuppressWarnings("unchecked")
    private Object getTogglableParameterValue(TogglableParameter<?> togglableParameter, Object arg) {
        Object togglableParameterValue;

        switch (togglableParameter.getAccessMethod()) {
            case DIRECT:
                togglableParameterValue = arg;
                break;
            case METHOD:
                try {
                    TogglableParameter<Method> methodTogglableParameter = (TogglableParameter<Method>) togglableParameter;
                    togglableParameterValue = methodTogglableParameter.getAccessibleObject().invoke(arg);
                } catch (Exception e) {
                    throw new ParamtersToggleFactoryException(String.format("The method [%s] must be implemented! ", togglableParameter.getAccessibleObject()), e);
                }
                break;
            case FIELD:
                try {
                    TogglableParameter<Field> fieldTogglableParameter = (TogglableParameter<Field>) togglableParameter;
                    togglableParameterValue = fieldTogglableParameter.getAccessibleObject().get(arg);
                } catch (Exception e) {
                    throw new ParamtersToggleFactoryException(String.format("Error accessing [%s] field! ", togglableParameter.getAccessibleObject()), e);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }

        return togglableParameterValue;
    }

    private void extractAnnotatedToggleParametersOnComplexMethodParameters(Method method, Collection<TogglableParameter<?>> togglableParameters) {
        int index = 0;
        for (Class<?> param : method.getParameterTypes()) {
            this.extractAnnotatedToggleParametersOnFields(index, param, togglableParameters);
            this.extractAnnotatedToggleParametersOnMethods(index, param, togglableParameters);
            index++;
        }
    }

    private void extractAnnotatedToggleParametersOnMethods(int index, Class<?> param, Collection<TogglableParameter<?>> togglableParameters) {
        for (Method method : param.getDeclaredMethods()) {
            ParameterToggle annotation = method.getAnnotation(ParameterToggle.class);
            if (annotation != null) {
                try {
                    if (!this.allowedParameterType(method.getReturnType())) {
                        throw new IllegalArgumentException("Parameter Toggle is not allowed here: " + method);
                    }
                    method.setAccessible(true);
                    togglableParameters.add(TogglableParameter.createTogglableParameter(index, ParameterToggle.class.cast(annotation).value(), method));
                } catch (Exception e) {
                    throw new ParamtersToggleFactoryException(String.format("Error on registering the method [%s] for togglable parameter", method), e);
                }
            }
        }
    }

    private void extractAnnotatedToggleParametersOnFields(int index, Class<?> param, Collection<TogglableParameter<?>> togglableParameters) {
        for (Field field : param.getDeclaredFields()) {
            ParameterToggle annotation = field.getAnnotation(ParameterToggle.class);
            if (annotation != null) {
                try {
                    if (!this.allowedParameterType(field.getType())) {
                        throw new IllegalArgumentException("Parameter Toggle is not allowed here: " + field);
                    }
                    field.setAccessible(true);
                    togglableParameters.add(TogglableParameter.createTogglableParameter(index, ParameterToggle.class.cast(annotation).value(), field));
                } catch (Exception e) {
                    throw new ParamtersToggleFactoryException(String.format("Error on registering the field [%s] for togglable parameter", field), e);
                }
            }
        }
    }

    private void extractAnnotatedToggleParametersOnPrimitiveMethodParameters(Method method, Collection<TogglableParameter<?>> togglableParameters) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Map<Integer, String> parameterToggleIndexes = new HashMap<Integer, String>();

        this.extractParameterToggleIndexes(method, parameterToggleIndexes);

        for (Map.Entry<Integer, String> parametersToggle : parameterToggleIndexes.entrySet()) {
            Integer parameterIndex = parametersToggle.getKey();
            String parameterToggleName = parametersToggle.getValue();

            if (this.allowedParameterType(parameterTypes[parameterIndex])) {
                togglableParameters.add(TogglableParameter.createTogglableParameter(parameterIndex, parameterToggleName));
            } else {
                throw new IllegalArgumentException("Parameter Toggle is not allowed here: " + method);
            }
        }
    }

    private void extractParameterToggleIndexes(Method method, Map<Integer, String> parameterToggleIndexes) {
        int index = 0;
        for (Annotation[] parameterAnnotations : method.getParameterAnnotations()) {
            for (Annotation annotation : parameterAnnotations) {
                if (annotation instanceof ParameterToggle) {
                    parameterToggleIndexes.put(index, ((ParameterToggle) annotation).value());
                }
            }
            index++;
        }
    }

    private boolean allowedParameterType(Class<?> parameterType) {
        return ALLOWED_PARAMETER_TOGGLE.contains(Primitives.wrap(parameterType)) || Enum.class.isAssignableFrom(parameterType);
    }

    private void put(Method method, Collection<TogglableParameter<?>> togglableParameters) {
        List<TogglableParameter> list = this.paramsMethodsCache.get(method);
        if (list == null) {
            list = new ArrayList<TogglableParameter>();
            this.paramsMethodsCache.put(method, list);
        }
        list.addAll(togglableParameters);
    }

    private Collection<String> getParamtersConfigured(TogglableParameter togglableParameter) {
        return this.getConfigured(togglableParameter.getId());
    }

    private Collection<String> getParamtersByFeatureConfigured(String featureName, TogglableParameter togglableParameter) {
        return this.getConfigured(String.format(PARAM_BY_FEATURE, featureName, togglableParameter.getId()));
    }

    private Collection<String> getConfigured(String key) {
        Collection<String> paramtersConfigured = this.config.getEnabledParameters().get(key);
        if (paramtersConfigured == null) {
            return Collections.emptyList();
        }
        return paramtersConfigured;
    }
}