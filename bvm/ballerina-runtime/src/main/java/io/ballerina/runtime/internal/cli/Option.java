/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.runtime.internal.cli;

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.internal.util.RuntimeUtils;
import io.ballerina.runtime.internal.values.DecimalValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the option passed via the cli.
 */
public class Option {
    private static final String NAMED_ARG_DELIMITER = "=";

    private static final String TRUE = "TRUE";
    private static final String FALSE = "FALSE";
    private static final String UNSUPPORTED_TYPE_PREFIX = "unsupported type expected with entry function";
    private static final String HEX_PREFIX = "0X";

    private String name;
    private final RecordType recordType;
    private final BMap<BString, Object> record;
    private final List<String> operandArgs;
    Set<BString> paramsFound;

    public Option(String name, Type recordType) {
        this(name, (RecordType) recordType,
             ValueCreator.createRecordValue(recordType.getPackage(), recordType.getName()));
    }

    public Option(String name, RecordType recordType, BMap<BString, Object> record) {
        this.name = name;
        this.recordType = recordType;
        this.record = record;
        operandArgs = new ArrayList<>();
        paramsFound = new HashSet<>();
    }

    public BMap<BString, Object> parseRecord(String[] args) {
        int index = 0;
        while (index < args.length) {
            String arg = args[index++];
            if (CliUtil.isOption(arg)) {
                // Handle the case when there's only -- not followed by a string
                if (arg.length() == 2) {
                    while (index < args.length) {
                        operandArgs.add(args[index++]);
                    }
                    break;
                }
                BString paramName = StringUtils.fromString(getNamedOptionName(arg));
                validateRecordParam(paramName);
                paramsFound.add(paramName);
                if (isNamedArg(arg)) {
                    processNamedArg(arg, paramName);
                } else {
                    if (handleBoolean(paramName)) {
                        continue;
                    }
                    if (index < args.length) {
                        String val = args[index];
                        Type fieldType = recordType.getFields().get(paramName.getValue()).getFieldType();
                        if (fieldType.getTag() == TypeTags.ARRAY_TAG) {
                            BArray bArray = getBArray(paramName, (ArrayType) fieldType);
                            Type elementType = bArray.getElementType();
                            validateOptionArg(arg, val);
                            bArray.append(getBValue(elementType, val));
                        } else {
                            validateOptionArg(arg, val);
                            record.put(paramName, getBValue(fieldType, val));
                        }
                        index++;
                    } else {
                        operandArgs.add(arg);
                    }
                }
            } else if (arg.startsWith("-")) {
                // Skip the config options
                if (arg.length() > 1 && arg.charAt(1) == 'C') {
                    if (!isNamedArg(arg)) {
                        index++;
                    }
                } else {
                    throw ErrorCreator.createError(
                            StringUtils.fromString("undefined parameter: '" + arg + "'"));
                }
            } else {
                operandArgs.add(arg);
            }
        }
        validateRecord();
        return record;
    }

    private boolean handleBoolean(BString paramName) {
        Type fieldType = recordType.getFields().get(paramName.getValue()).getFieldType();
        if (isABoolean(fieldType)) {
            record.put(paramName, true);
            return true;
        } else if (fieldType.getTag() == TypeTags.ARRAY_TAG) {
            BArray bArray = getBArray(paramName, (ArrayType) fieldType);
            Type elementType = bArray.getElementType();
            if (isABoolean(elementType)) {
                bArray.append(true);
                return true;
            }
        }
        return false;
    }

    private void validateRecord() {
        for (BString key : record.getKeys()) {
            if (!paramsFound.contains(key)) {
                Type fieldType = recordType.getFields().get(key.getValue()).getFieldType();
                if (fieldType.getTag() == TypeTags.UNION_TAG) {
                    List<Type> unionMemberTypes = ((UnionType) fieldType).getMemberTypes();
                    if (unionMemberTypes.contains(PredefinedTypes.TYPE_NULL)) {
                        continue;
                    }
                }
                throw ErrorCreator.createError(
                        StringUtils.fromString("Missing required parameter: '" + key + "'"));
            }
        }
    }

    private void validateOptionArg(String arg, String val) {
        if (CliUtil.isOption(val)) {
            throw ErrorCreator.createError(
                    StringUtils.fromString("Missing required parameter for " + arg + "'"));
        }
    }

    private BArray getBArray(BString paramName, ArrayType fieldType) {
        BArray bArray = (BArray) record.get(paramName);
        if (bArray == null) {
            bArray = ValueCreator.createArrayValue(fieldType, -1);
            record.put(paramName, bArray);
        }
        return bArray;
    }

    private boolean isABoolean(Type fieldType) {
        return fieldType.getTag() == TypeTags.BOOLEAN_TAG;
    }

    private void processNamedArg(String arg, BString paramName) {
        String val = getValueString(arg);
        Type fieldType = recordType.getFields().get(paramName.getValue()).getFieldType();
        if (fieldType.getTag() == TypeTags.ARRAY_TAG) {
            BArray bArray = getBArray(paramName, (ArrayType) fieldType);
            Type arrayType = bArray.getElementType();
            bArray.append(getBValue(arrayType, val));
            return;
        }
        record.put(paramName, getBValue(fieldType, val));
    }

    private void validateRecordParam(BString paramName) {
        if (!(record.containsKey(paramName) || recordType.getFields().containsKey(paramName.getValue()))) {
            throw ErrorCreator.createError(
                    StringUtils.fromString("undefined parameter: '" + paramName + "'"));
        }
    }

    private String getValueString(String arg) {
        return arg.split(NAMED_ARG_DELIMITER, 2)[1];
    }

    private static Object getBValue(Type type, String value) {
        switch (type.getTag()) {
            case TypeTags.STRING_TAG:
                return StringUtils.fromString(value);
            case TypeTags.INT_TAG:
                return getIntegerValue(value);
            case TypeTags.FLOAT_TAG:
                return getFloatValue(value);
            case TypeTags.DECIMAL_TAG:
                return getDecimalValue(value);
            case TypeTags.BYTE_TAG:
                return getByteValue(value);
            default:
                throw ErrorCreator.createError(StringUtils.fromString(UNSUPPORTED_TYPE_PREFIX + " '" + type + "'"));
        }
    }

    private static long getIntegerValue(String argument) {
        try {
            if (argument.toUpperCase().startsWith(HEX_PREFIX)) {
                return Long.parseLong(argument.toUpperCase().replace(HEX_PREFIX, ""), 16);
            }
            return Long.parseLong(argument);
        } catch (NumberFormatException e) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument + "', expected " +
                                                                          "integer value"));
        }
    }

    private static double getFloatValue(String argument) {
        try {
            return Double.parseDouble(argument);
        } catch (NumberFormatException e) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument + "', expected " +
                                                                          "float value"));
        }
    }

    private static DecimalValue getDecimalValue(String argument) {
        try {
            return new DecimalValue(argument);
        } catch (NumberFormatException e) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument + "', expected " +
                                                                          "decimal value"));
        }
    }

    private static boolean getBooleanValue(String argument) {
        if (!TRUE.equalsIgnoreCase(argument) && !FALSE.equalsIgnoreCase(argument)) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument
                                                                          + "', expected boolean value 'true' or " +
                                                                          "'false'"));
        }
        return Boolean.parseBoolean(argument);
    }

    private static int getByteValue(String argument) {
        int byteValue;
        try {
            byteValue = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument + "', expected " +
                                                                          "byte value"));
        }

        if (!RuntimeUtils.isByteLiteral(byteValue)) {
            throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + argument +
                                                                          "', expected byte value, found int"));
        }

        return byteValue;
    }

    private boolean isNamedArg(String arg) {
        return arg.contains(NAMED_ARG_DELIMITER);
    }

    private String getNamedOptionName(String arg) {
        return arg.split(NAMED_ARG_DELIMITER, 2)[0].substring(2).trim();
    }

    public List<String> getOperandArgs() {
        return operandArgs;
    }
}