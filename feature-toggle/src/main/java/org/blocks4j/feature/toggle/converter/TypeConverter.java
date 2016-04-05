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

package org.blocks4j.feature.toggle.converter;


import org.blocks4j.feature.toggle.exception.FeatureToggleDefinitionParsingException;

public final class TypeConverter {

    public Object convertToType(String value, Class<?> clazz) {
        if (value == null) {
            return null;
        }
        if (value.getClass() == clazz) {
            return value;
        }
        Object result = value;
        try {
            if (clazz.isPrimitive()) {
                result = this.convertToPrimitive(value, clazz);
            } else if (clazz == Long.class) {
                result = Long.parseLong(value);
            } else if (clazz == Integer.class) {
                result = Integer.parseInt(value);
            } else if (clazz == Short.class) {
                result = Short.parseShort(value);
            } else if (clazz == Byte.class) {
                result = Byte.parseByte(value);
            } else if (clazz == Double.class) {
                result = Double.parseDouble(value);
            } else if (clazz == Float.class) {
                result = Float.parseFloat(value);
            } else if (clazz == Character.class) {
                if (value.length() > 1) {
                    throw new RuntimeException(String.format("The String '%s' cannot be converted to java.lang.Character type.", value));
                }
                result = value.charAt(0);
            } else if (clazz == Boolean.class) {
                result = Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            throw new FeatureToggleDefinitionParsingException(String.format("It was not possible to convert from string to '%s' type, due: '%s'.",
                                                                            clazz.getName(),
                                                                            e.getLocalizedMessage()), e);
        }

        return result;
    }

    private Object convertToPrimitive(String value, Class<?> clazz) {
        Object convertedObject = null;
        if (clazz == long.class) {
            convertedObject = Long.parseLong(value);
        } else if (clazz == int.class) {
            convertedObject = Integer.parseInt(value);
        } else if (clazz == short.class) {
            convertedObject = Short.parseShort(value);
        } else if (clazz == byte.class) {
            convertedObject = Byte.parseByte(value);
        } else if (clazz == double.class) {
            convertedObject = Double.parseDouble(value);
        } else if (clazz == float.class) {
            convertedObject = Float.parseFloat(value);
        } else if (clazz == char.class) {
            if (value.length() > 1) {
                throw new RuntimeException(String.format("The String '%s' cannot be converted to java.lang.Character type.", value));
            }
            convertedObject = value.charAt(0);
        } else if (clazz == boolean.class) {
            convertedObject = Boolean.parseBoolean(value);
        }

        return convertedObject;
    }

}
