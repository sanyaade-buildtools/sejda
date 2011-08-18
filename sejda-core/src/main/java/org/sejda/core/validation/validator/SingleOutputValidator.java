/*
 * Created on 12/ago/2011
 * Copyright 2011 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.core.validation.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang.StringUtils;
import org.sejda.core.manipulation.model.output.OutputType;
import org.sejda.core.manipulation.model.parameter.SingleOutputDocumentParameter;
import org.sejda.core.validation.constraint.ValidSingleOutput;

/**
 * Validates that a single output parameter has a valid output name if the selected output is not a file. The output name is used when writing the generated output to a zip stream
 * or a directory.
 * 
 * @author Andrea Vacondio
 * 
 */
public class SingleOutputValidator implements ConstraintValidator<ValidSingleOutput, SingleOutputDocumentParameter> {

    public void initialize(ValidSingleOutput constraintAnnotation) {
        // nothing to do
    }

    public boolean isValid(SingleOutputDocumentParameter value, ConstraintValidatorContext context) {
        if (value != null) {
            return value.getOutput().getOutputType() == OutputType.FILE_OUTPUT
                    || StringUtils.isNotBlank(value.getOutputName());
        }
        return true;
    }

}