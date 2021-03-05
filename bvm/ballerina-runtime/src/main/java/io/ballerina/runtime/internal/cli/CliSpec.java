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

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that has the cli argument definitions.
 *
 * @since 2.0.0
 */
public class CliSpec {
    private Option option;
    private Operand[] operands;
    private boolean hasRestParam;
    private Object[] mainArgs;
    private final String[] args;

    public CliSpec(Option option, Operand[] operands, String[] args, boolean hasRestParam) {
        this.option = option;
        this.operands = operands;
        this.hasRestParam = hasRestParam;
        this.args = args;
    }

    public Object[] getMainArgs() {
        Object[] processedOperands;
        int start = 1;
        if (option != null) {
            BMap<BString, Object> record = option.parseRecord(args);
            processedOperands = processOperands(option.getOperandArgs());
            mainArgs = new Object[(processedOperands.length + 1) * 2 + 1];
            mainArgs[start++] = record;
            mainArgs[start++] = true;
        } else {
            processedOperands = processOperands(parseOperandArgs(args));
            mainArgs = new Object[processedOperands.length * 2 + 1];
        }
        int index = 0;
        while (start < mainArgs.length) {
            mainArgs[start++] = processedOperands[index++];
            mainArgs[start++] = true;
        }
        return mainArgs;
    }

    private List<String> parseOperandArgs(String[] args) {
        List<String> operandArgs = new ArrayList<>();
        for (String arg : args) {
            if (CliUtil.isOption(arg)) {
                throw ErrorCreator.createError(StringUtils.fromString("invalid argument '" + arg + "', expected " +
                                                                              "integer value"));
            }
            operandArgs.add(arg);
        }
        return operandArgs;
    }

    private BValue[] processOperands(List<String> operandArgs) {
        BValue[] operandValues = new BValue[operands.length];
        for (String arg : operandArgs) {

        }
        return operandValues;
    }
}
