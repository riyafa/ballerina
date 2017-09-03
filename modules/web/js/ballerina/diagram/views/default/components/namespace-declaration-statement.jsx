/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import StatementDecorator from './statement-decorator';

/**
 * Class for namespace declaration statement component.
 * @class NamespaceDeclarationStatement
 * */
class NamespaceDeclarationStatement extends React.Component {
    /**
     * constructor for namespace declaration statement.
     * @param {object} props - props for component.
     * */
    constructor(props) {
        super(props);
        this.editorOptions = {
            propertyType: 'text',
            key: 'Response Message',
            model: this.props.model,
            getterMethod: this.props.model.getStatementString,
            setterMethod: this.props.model.setStatementFromString,
        };
    }

    /**
     * Render the namespace declaration statement.
     * @return {Object} react component.
     * */
    render() {
        let model = this.props.model;
        const expression = model.getViewState().expression;
        return (
            <StatementDecorator
                viewState={model.viewState}
                model={model}
                expression={expression}
                editorOptions={this.editorOptions}
            />);
    }
}

NamespaceDeclarationStatement.propTypes = {
    bBox: PropTypes.shape({
        x: PropTypes.number.isRequired,
        y: PropTypes.number.isRequired,
        w: PropTypes.number.isRequired,
        h: PropTypes.number.isRequired,
    }),
    expression: PropTypes.shape({
        expression: PropTypes.string,
    }),
};

export default NamespaceDeclarationStatement;
