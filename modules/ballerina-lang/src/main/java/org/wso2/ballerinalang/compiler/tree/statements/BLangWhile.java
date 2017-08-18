/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.ballerinalang.compiler.tree.statements;

import org.ballerinalang.model.tree.statements.WhileNode;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangExpression;

/**
 * @since 0.94
 */
public class BLangWhile extends BLangStatement implements WhileNode {
    public BLangExpression expr;
    public BLangBlockStmt body;

    public BLangWhile(BLangExpression expr, BLangBlockStmt body) {
        this.expr = expr;
        this.body = body;
    }

    @Override
    public BLangExpression getCondition() {
        return expr;
    }

    @Override
    public BLangBlockStmt getBody() {
        return body;
    }
}
