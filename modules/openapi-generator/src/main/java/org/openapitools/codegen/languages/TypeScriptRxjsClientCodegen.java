/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * See: https://opensource.stackexchange.com/questions/7300/copyright-notice-in-the-file-header-apache-v2-license/7301#7301
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.codegen.*;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class TypeScriptRxjsClientCodegen extends AbstractTypeScriptClientCodegen {
    private final Logger LOGGER = LoggerFactory.getLogger(AbstractTypeScriptClientCodegen.class);

    public static final String NPM_REPOSITORY = "npmRepository";
    public static final String WITH_PROGRESS_SUBSCRIBER = "withProgressSubscriber";

    @Getter @Setter
    protected String npmRepository = null;
    protected Set<String> reservedParamNames = new HashSet<>();

    public TypeScriptRxjsClientCodegen() {
        super();

        modifyFeatureSet(features -> features.includeDocumentationFeatures(DocumentationFeature.Readme));

        outputFolder = "generated-code/typescript-rxjs";
        embeddedTemplateDir = templateDir = "typescript-rxjs";

        this.apiPackage = "apis";
        this.apiTemplateFiles.put("apis.mustache", ".ts");
        this.modelPackage = "models";
        this.modelTemplateFiles.put("models.mustache", ".ts");
        this.addExtraReservedWords();

        languageSpecificPrimitives.add("Blob");
        typeMapping.put("file", "Blob");

        this.cliOptions.add(new CliOption(NPM_REPOSITORY, "Use this property to set an url your private npmRepo in the package.json"));
        this.cliOptions.add(new CliOption(WITH_PROGRESS_SUBSCRIBER, "Setting this property to true will generate API controller methods with support for subscribing to request progress.", SchemaTypeUtil.BOOLEAN_TYPE).defaultValue(Boolean.FALSE.toString()));

        // these are used in the api template for more efficient destructuring
        this.reservedParamNames.add("headers");
        this.reservedParamNames.add("query");
        this.reservedParamNames.add("formData");
    }

    @Override
    public String getName() {
        return "typescript-rxjs";
    }

    @Override
    public String getHelp() {
        return "Generates a TypeScript client library using Rxjs API.";
    }

    @Override
    public void processOpts() {
        super.processOpts();
        supportingFiles.add(new SupportingFile("index.mustache", "", "index.ts"));
        supportingFiles.add(new SupportingFile("runtime.mustache", "", "runtime.ts"));
        supportingFiles.add(new SupportingFile("servers.mustache", "", "servers.ts"));
        supportingFiles.add(new SupportingFile("apis.index.mustache", apiPackage().replace('.', File.separatorChar), "index.ts"));
        supportingFiles.add(new SupportingFile("models.index.mustache", modelPackage().replace('.', File.separatorChar), "index.ts"));
        supportingFiles.add(new SupportingFile("tsconfig.mustache", "", "tsconfig.json"));
        supportingFiles.add(new SupportingFile("gitignore", "", ".gitignore"));
        if (additionalProperties.containsKey(NPM_NAME)) {
            addNpmPackageGeneration();
        }
    }

    @Override
    public boolean isDataTypeFile(final String dataType) {
        return "Blob".equals(dataType);
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isFileSchema(p)) {
            return "Blob";
        } else if (ModelUtils.isBinarySchema(p)) {
            return "Blob";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        codegenModel.additionalPropertiesType = getTypeDeclaration(ModelUtils.getAdditionalProperties(schema));
        addImport(codegenModel, codegenModel.additionalPropertiesType);
    }

    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        // process enum in models
        List<ModelMap> models = postProcessModelsEnum(objs).getModels();
        for (ModelMap mo : models) {
            CodegenModel cm = mo.getModel();
            cm.imports = new TreeSet<>(cm.imports);
            // name enum with model name, e.g. StatusEnum => PetStatusEnum
            for (CodegenProperty var : cm.vars) {
                if (Boolean.TRUE.equals(var.isEnum)) {
                    // behaviour for enum names is specific for typescript to not use namespaces
                    var.datatypeWithEnum = var.datatypeWithEnum.replace(var.enumName, cm.classname + var.enumName);
                }
            }
            if (cm.parent != null) {
                for (CodegenProperty var : cm.allVars) {
                    if (Boolean.TRUE.equals(var.isEnum)) {
                        var.datatypeWithEnum = var.datatypeWithEnum
                                .replace(var.enumName, cm.classname + var.enumName);
                    }
                }
            }
        }

        return objs;
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        Map<String, ModelsMap> result = super.postProcessAllModels(objs);
        for (ModelsMap entry : result.values()) {
            for (ModelMap model : entry.getModels()) {
                CodegenModel codegenModel = model.getModel();
                model.put("hasImports", codegenModel.imports.size() > 0);
            }
        }
        return result;
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter) {
        super.postProcessParameter(parameter);
        parameter.dataType = applyLocalTypeMapping(parameter.dataType);
    }

    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        if (isLanguagePrimitive(openAPIType)) {
            return openAPIType;
        }
        applyLocalTypeMapping(openAPIType);
        return openAPIType;
    }

    private String applyLocalTypeMapping(String type) {
        if (typeMapping.containsKey(type)) {
            type = typeMapping.get(type);
        }
        return type;
    }

    private boolean isLanguagePrimitive(String type) {
        return languageSpecificPrimitives.contains(type);
    }

    private void addNpmPackageGeneration() {
        if (additionalProperties.containsKey(NPM_REPOSITORY)) {
            this.setNpmRepository(additionalProperties.get(NPM_REPOSITORY).toString());
        }

        // Files for building our lib
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("package.mustache", "", "package.json"));
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap operations, List<ModelMap> allModels) {
        // Convert List of CodegenOperation to List of ExtendedCodegenOperation
        List<CodegenOperation> os = operations.getOperations().getOperation();
        List<ExtendedCodegenOperation> newOs = new ArrayList<>();
        for (CodegenOperation o : os) {
            newOs.add(new ExtendedCodegenOperation(o));
        }
        operations.getOperations().setOperation(newOs);

        this.addOperationModelImportInformation(operations);
        this.updateOperationParameterEnumInformation(operations);
        this.addConditionalImportInformation(operations);

        return operations;
    }

    private void addOperationModelImportInformation(OperationsMap operations) {
        // This method will add extra information to the operations.imports array.
        // The api template uses this information to import all the required
        // models for a given operation.
        List<Map<String, String>> imports = operations.getImports();
        for (Map<String, String> im : imports) {
            im.put("className", im.get("import").replace("models.", ""));
        }
    }

    private void updateOperationParameterEnumInformation(OperationsMap operations) {
        // This method will add extra information as to whether or not we have enums and
        // update their names with the operation.id prefixed.
        boolean hasEnums = false;
        for (CodegenOperation _op : operations.getOperations().getOperation()) {
            ExtendedCodegenOperation op = (ExtendedCodegenOperation) _op;
            for (CodegenParameter param : op.allParams) {
                if (Boolean.TRUE.equals(param.isEnum)) {
                    hasEnums = true;
                    param.datatypeWithEnum = param.datatypeWithEnum
                            .replace(param.enumName, op.operationIdCamelCase + param.enumName);
                }
            }
        }

        operations.put("hasEnums", hasEnums);
    }

    private void setParamNameAlternative(CodegenParameter param, String paramName, String paramNameAlternative) {

        if (param.paramName.equals(paramName)) {
            param.vendorExtensions.put("x-param-name-alternative", paramNameAlternative);
        }
    }

    private void addConditionalImportInformation(OperationsMap operations) {
        // This method will determine if there are required parameters and if there are list containers

        boolean hasRequiredParams = false;
        boolean hasListContainers = false;
        boolean hasHttpHeaders = false;
        boolean hasQueryParams = false;
        boolean hasPathParams = false;

        for (CodegenOperation _op : operations.getOperations().getOperation()) {
            ExtendedCodegenOperation op = (ExtendedCodegenOperation) _op;
            if (op.getHasRequiredParams()) {
                hasRequiredParams = true;
            }

            for (CodegenParameter p : op.allParams) {
                String paramNameAlternative = null;

                if (this.reservedParamNames.contains(p.paramName)) {
                    paramNameAlternative = p.paramName + "Alias";
                    LOGGER.info("param: {} isReserved ––> {}", p.paramName, paramNameAlternative);
                }
                setParamNameAlternative(p, p.paramName, paramNameAlternative);

                for (CodegenParameter param : op.headerParams) {
                    if (param.isArray) {
                        hasListContainers = true;
                    }
                    setParamNameAlternative(param, p.paramName, paramNameAlternative);
                }

                for (CodegenParameter param : op.queryParams) {
                    if (param.isArray && !param.isCollectionFormatMulti) {
                        hasListContainers = true;
                    }
                    if (param.required) {
                        op.hasRequiredQueryParams = true;
                    } else {
                        op.hasOptionalQueryParams = true;
                    }
                    setParamNameAlternative(param, p.paramName, paramNameAlternative);
                }

                for (CodegenParameter param : op.formParams) {
                    if (param.isArray && !param.isCollectionFormatMulti) {
                        hasListContainers = true;
                    }
                    setParamNameAlternative(param, p.paramName, paramNameAlternative);
                }

                for (CodegenParameter param : op.pathParams) {
                    setParamNameAlternative(param, p.paramName, paramNameAlternative);
                }
            }

            if (op.hasHttpHeaders) {
                hasHttpHeaders = true;
            }
            if (op.getHasQueryParams()) {
                hasQueryParams = true;
            }
            if (op.getHasPathParams()) {
                hasPathParams = true;
            }
        }

        operations.put("hasRequiredParams", hasRequiredParams);
        operations.put("hasListContainers", hasListContainers);
        operations.put("hasHttpHeaders", hasHttpHeaders);
        operations.put("hasQueryParams", hasQueryParams);
        operations.put("hasPathParams", hasPathParams);
    }

    private void addExtraReservedWords() {
        this.reservedWords.add("BASE_PATH");
        this.reservedWords.add("BaseAPI");
        this.reservedWords.add("COLLECTION_FORMATS");
        this.reservedWords.add("ConfigurationParameters");
        this.reservedWords.add("Configuration");
        this.reservedWords.add("HttpMethod");
        this.reservedWords.add("HttpHeaders");
        this.reservedWords.add("HttpQuery");
        this.reservedWords.add("HttpBody");
        this.reservedWords.add("RequestArgs");
        this.reservedWords.add("RequestOpts");
        this.reservedWords.add("ResponseArgs");
        this.reservedWords.add("Middleware");
        this.reservedWords.add("AjaxRequest");
        this.reservedWords.add("AjaxResponse");
        this.reservedWords.add("servers");
    }

    class ExtendedCodegenOperation extends CodegenOperation {
        public boolean hasHttpHeaders;
        public boolean hasRequiredQueryParams;
        public boolean hasOptionalQueryParams;

        public ExtendedCodegenOperation(CodegenOperation o) {
            super();

            // Copy all fields of CodegenOperation
            this.responseHeaders.addAll(o.responseHeaders);
            this.hasAuthMethods = o.hasAuthMethods;
            this.hasConsumes = o.hasConsumes;
            this.hasProduces = o.hasProduces;
            this.hasOptionalParams = o.hasOptionalParams;
            this.returnTypeIsPrimitive = o.returnTypeIsPrimitive;
            this.returnSimpleType = o.returnSimpleType;
            this.subresourceOperation = o.subresourceOperation;
            this.isMap = o.isMap;
            this.isArray = o.isArray;
            this.isMultipart = o.isMultipart;
            this.isResponseBinary = o.isResponseBinary;
            this.isResponseFile = o.isResponseFile;
            this.hasReference = o.hasReference;
            this.isDeprecated = o.isDeprecated;
            this.isCallbackRequest = o.isCallbackRequest;
            this.path = o.path;
            this.operationId = o.operationId;
            this.returnType = o.returnType;
            this.httpMethod = o.httpMethod;
            this.returnBaseType = o.returnBaseType;
            this.returnContainer = o.returnContainer;
            this.summary = o.summary;
            this.unescapedNotes = o.unescapedNotes;
            this.notes = o.notes;
            this.baseName = o.baseName;
            this.defaultResponse = o.defaultResponse;
            this.discriminator = o.discriminator;
            this.consumes = o.consumes;
            this.produces = o.produces;
            this.prioritizedContentTypes = o.prioritizedContentTypes;
            this.servers = o.servers;
            this.bodyParam = o.bodyParam;
            this.allParams = o.allParams;
            this.bodyParams = o.bodyParams;
            this.pathParams = o.pathParams;
            this.queryParams = o.queryParams;
            this.headerParams = o.headerParams;
            this.formParams = o.formParams;
            this.cookieParams = o.cookieParams;
            this.requiredParams = o.requiredParams;
            this.optionalParams = o.optionalParams;
            this.authMethods = o.authMethods;
            this.tags = o.tags;
            this.responses = o.responses;
            this.callbacks = o.callbacks;
            this.imports = o.imports;
            this.examples = o.examples;
            this.requestBodyExamples = o.requestBodyExamples;
            this.externalDocs = o.externalDocs;
            this.vendorExtensions = o.vendorExtensions;
            this.nickname = o.nickname;
            this.operationIdOriginal = o.operationIdOriginal;
            this.operationIdLowerCase = o.operationIdLowerCase;
            this.operationIdCamelCase = o.operationIdCamelCase;
            this.operationIdSnakeCase = o.operationIdSnakeCase;

            // new fields
            this.hasHttpHeaders = o.getHasHeaderParams() || o.getHasBodyParam() || o.hasAuthMethods;
            this.hasRequiredQueryParams = false; // will be updated within addConditionalImportInformation
            this.hasOptionalQueryParams = false; // will be updated within addConditionalImportInformation
        }
    }

    @Override
    protected void addImport(Schema composed, Schema childSchema, CodegenModel model, String modelName) {
        // import everything (including child schema of a composed schema)
        addImport(model, modelName);
    }

    @Override
    protected String getLicenseNameDefaultValue() {
        return null;
    }
}
