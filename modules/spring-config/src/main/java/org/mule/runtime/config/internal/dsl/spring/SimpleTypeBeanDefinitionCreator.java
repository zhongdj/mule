/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Map;
import java.util.Optional;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * <p>
 * Elements that represent a simple type always have the form
 *
 * <pre>
 *  <element value="simpleValue"/>
 * </pre>
 *
 * @since 4.0
 */
class SimpleTypeBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    Class<?> type = createBeanDefinitionRequest.retrieveTypeVisitor().getType();
    if (isSimpleType(type)) {
      ComponentModel componentModel = (ComponentModel) createBeanDefinitionRequest.getComponentModel();
      createBeanDefinitionRequest.getSpringComponentModel().setType(type);
      Map<String, String> parameters = componentModel.getRawParameters();

      if (parameters.size() >= 2) {
        // Component model has more than one parameter when it's supposed to have at most one parameter
        return false;
      }
      if (componentModel.getTextContent() != null && !componentModel.getRawParameters().isEmpty()) {
        // Component model has both a parameter and an inner content
        return false;
      }

      final String value = componentModel.getTextContent() != null
          ? componentModel.getTextContent()
          : parameters.values().stream().findFirst().orElse(null);
      if (value == null) {
        throw new MuleRuntimeException(createStaticMessage("Parameter at %s:%s must provide a non-empty value",
                                                           componentModel.getMetadata().getFileName()
                                                               .orElse("unknown"),
                                                           componentModel.getMetadata().getStartLine().orElse(-1)));
      }
      Optional<TypeConverter> typeConverterOptional =
          createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeConverter();
      createBeanDefinitionRequest.getSpringComponentModel()
          .setBeanDefinition(getConvertibleBeanDefinition(type, value, typeConverterOptional));
      return true;
    }
    return false;
  }
}
