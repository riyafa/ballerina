/**
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
define(['lodash', './node', 'log'], function(_, ASTNode, log){

    /**
     * Constructor for ConnectorDefinition
     * @param {object} args - Constructor arguments
     * @constructor
     */
    var ConnectorAction = function(args) {
        ASTNode.call(this, "ConnectorDefinition");
        this.BallerinaASTFactory = this.getFactory();
        this.action_name = _.get(args, 'action_name', 'newAction');
        this.annotations = _.get(args, 'annotations', []);
        this.arguments = _.get(args, 'arguments', []);
    };

    ConnectorAction.prototype = Object.create(ASTNode.prototype);
    ConnectorAction.prototype.constructor = ConnectorAction;

    /**
     * Get the name of action
     * @return {string} action_name - Action Name
     */
    ConnectorAction.prototype.getActionName = function () {
        return this.action_name
    };

    /**
     * Get the annotations
     * @return {string[]} annotations - Action Annotations
     */
    ConnectorAction.prototype.getAnnotations = function () {
        return this.annotations
    };

    /**
     * Get the action Arguments
     * @return {Object[]} arguments - Action Arguments
     */
    ConnectorAction.prototype.getArguments = function () {
        var actionArgs = [];
        var self = this;

        _.forEach(this.getChildren(), function (child) {
            if (self.BallerinaASTFactory.isArgument(child)) {
                actionArgs.push(child);
            }
        });
        return actionArgs;
    };

    /**
     * Set the Action name
     * @param {string} name - Action Name
     */
    ConnectorAction.prototype.setActionName = function (name) {
        this.connector_name = name;
    };

    /**
     * Set the action annotations
     * @param {string[]} annotations - Action Annotations
     */
    ConnectorAction.prototype.setAnnotations = function (annotations) {
        if (!_.isNil(annotations)) {
            this.annotations = annotations;
        } else {
            log.warn('Trying to set a null or undefined array to annotations');
        }
    };

    /**
     * Get the variable Declarations
     * @return {VariableDeclaration[]} variableDeclarations
     */
    ConnectorAction.prototype.getVariableDeclarations = function () {
        var variableDeclarations = [];
        var self = this;

        _.forEach(this.getChildren(), function (child) {
            if (self.BallerinaASTFactory.isVariableDeclaration(child)) {
                variableDeclarations.push(child);
            }
        });
        return variableDeclarations;
    };

    /**
     * Remove variable declaration.
     */
    ConnectorAction.prototype.removeVariableDeclaration = function (variableDeclaration) {
        this.removeChild(variableDeclaration);
    };

    /**
     * Adds new variable declaration.
     */
    ConnectorAction.prototype.addVariableDeclaration = function (newVariableDeclaration) {
        var self = this;
        // Get the index of the last variable declaration.
        var index = _.findLastIndex(this.getChildren(), function (child) {
            return self.BallerinaASTFactory.isVariableDeclaration(child);
        });

        // index = -1 when there are not any variable declarations, hence get the index for connector
        // declarations.
        if (index == -1) {
            index = _.findLastIndex(this.getChildren(), function (child) {
                return self.BallerinaASTFactory.isConnectorDeclaration(child);
            });
        }

        this.addChild(newVariableDeclaration, index + 1);
    };

    //// Start of return type functions.

    /**
     * Gets the return type as a string separated by commas.
     * @return {string} - Return types.
     */
    ConnectorAction.prototype.getReturnTypesAsString = function () {
        var returnTypes = [];
        _.forEach(this.getReturnTypes(), function (returnTypeChild) {
            returnTypes.push(returnTypeChild.getArgumentAsString())
        });

        return _.join(returnTypes, ", ");
    };

    /**
     * Gets the defined return types.
     * @return {Argument[]} - Array of args.
     */
    ConnectorAction.prototype.getReturnTypes = function () {
        var returnTypeModel = this.getReturnTypeModel();
        return !_.isUndefined(returnTypeModel) ? this.getReturnTypeModel().getChildren().slice(0) : [];
    };

    /**
     * Adds a new argument to return type model.
     * @param {string} type - The type of the argument.
     * @param {string} identifier - The identifier of the argument.
     */
    ConnectorAction.prototype.addReturnType = function (type, identifier) {
        var self = this;

        // Adding return type mode if it doesn't exists.
        if (_.isUndefined(this.getReturnTypeModel())) {
            this.addChild(this.BallerinaASTFactory.createReturnType());
        }

        // Check if there is already a return type with the same identifier.
        if (!_.isUndefined(identifier)) {
            _.forEach(this.getReturnTypeModel().getChildren(), function(child) {
                if (child.getIdentifier() === identifier) {
                    var errorString = "An argument with identifier '" + identifier + "' already exists.";
                    log.error(errorString);
                    throw errorString;
                }
            });
        }

        // Validating whether return type can be added based on identifiers of other return types.
        if (!_.isUndefined(identifier)) {
            var indexWithoutIdentifiers = _.findIndex(this.getReturnTypeModel().getChildren(), function (child) {
                return _.isUndefined(child.getIdentifier());
            });
            if (indexWithoutIdentifiers !== -1) {
                var errorStringWithoutIdentifiers = "Return types without identifiers already exists. Remove them to " +
                    "add return types with identifiers.";
                log.error(errorStringWithoutIdentifiers);
                throw errorStringWithoutIdentifiers;
            }
        } else {
            var indexWithIdentifiers = _.findIndex(this.getReturnTypeModel().getChildren(), function (child) {
                return !_.isUndefined(child.getIdentifier());
            });
            if (indexWithIdentifiers !== -1) {
                var errorStringWithIdentifiers = "Return types with identifiers already exists. Remove them to add " +
                    "return types without identifiers.";
                log.error(errorStringWithIdentifiers);
                throw errorStringWithIdentifiers;
            }
        }

        var argument = this.BallerinaASTFactory.createArgument({type: type, identifier: identifier});

        var existingReturnType = self.getReturnTypeModel();

        // Adding the new argument input position.
        if (!_.isNil(existingReturnType)) {
            existingReturnType.addChild(argument, existingReturnType.getChildren().length + 1);
        } else {
            var returnType = this.BallerinaASTFactory.createReturnType();
            returnType.addChild(argument, 0);
            this.addChild(returnType);
        }
    };

    /**
     * Removes return type argument from the return type model.
     * @param {string} modelID - The id of an {Argument} which resides in the return type model.
     */
    ConnectorAction.prototype.removeReturnType = function (modelID) {
        var self = this;
        var argumentToRemove = undefined;

        // Find the argument to remove/delete.
        _.forEach(self.getReturnTypeModel().getChildren(), function (argument) {
            if (argument.getID() == modelID) {
                argumentToRemove = argument;
                // break
                return false;
            }
        });

        // Deleting the argument from the AST.
        if (!_.isUndefined(argumentToRemove)) {
            self.getReturnTypeModel().removeChild(argumentToRemove);
        } else {
            var exceptionString = "Could not find a return type with ID: " + modelID;
            log.error(exceptionString);
            throw exceptionString;
        }
    };

    /**
     * Gets the return type model. A connector action definition can have only one {ReturnType} model.
     * @return {ReturnType|undefined} - The return type model.
     */
    ConnectorAction.prototype.getReturnTypeModel = function() {
        var self = this;
        var returnTypeModel = undefined;
        _.forEach(this.getChildren(), function (child) {
            if (self.BallerinaASTFactory.isReturnType(child)) {
                returnTypeModel = child;
                // break
                return false;
            }
        });

        return returnTypeModel;
    };

    //// End of return type functions.

    /**
     * Returns the list of arguments as a string separated by commas.
     * @return {string} - Arguments as string.
     */
    ConnectorAction.prototype.getArgumentsAsString = function () {
        var argsAsString = "";
        var args = this.getArguments();
        _.forEach(args, function(argument, index){
            argsAsString += argument.type + " ";
            argsAsString += argument.identifier;
            if (args.length - 1 != index) {
                argsAsString += ", ";
            }
        });

        return argsAsString;
    };

    /**
     * Adds new argument to the function definition.
     * @param type - The type of the argument.
     * @param identifier - The identifier of the argument.
     */
    ConnectorAction.prototype.addArgument = function(type, identifier) {
        //creating argument
        var newArgument = this.BallerinaASTFactory.createArgument();
        newArgument.setType(type);
        newArgument.setIdentifier(identifier);

        var self = this;

        // Get the index of the last argument declaration.
        var index = _.findLastIndex(this.getChildren(), function (child) {
            return self.BallerinaASTFactory.isArgument(child);
        });

        this.addChild(newArgument, index + 1);
    };

    /**
     * Removes an argument from a function definition.
     * @param identifier - The identifier of the argument.
     * @return {Array} - The removed argument.
     */
    ConnectorAction.prototype.removeArgument = function(identifier) {
        var self = this;
        _.remove(this.getChildren(), function (child) {
            return self.BallerinaASTFactory.isArgument(child) && child.getIdentifier() === identifier;
        });
    };

    /**
     * Validates possible immediate child types.
     * @override
     * @param node
     * @return {boolean}
     */
    ConnectorAction.prototype.canBeParentOf = function (node) {
        return this.BallerinaASTFactory.isConnectorDeclaration(node)
            || this.BallerinaASTFactory.isVariableDeclaration(node)
            || this.BallerinaASTFactory.isWorkerDeclaration(node)
            || this.BallerinaASTFactory.isStatement(node);
    };

    return ConnectorAction;
});