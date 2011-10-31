/*
 * Created on 16/ago/2011
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
package org.sejda.core.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sejda.TestUtils;
import org.sejda.core.context.DefaultSejdaContext;
import org.sejda.core.context.SejdaContext;
import org.sejda.model.exception.TaskException;
import org.sejda.model.input.PdfMergeInput;
import org.sejda.model.input.PdfStreamSource;
import org.sejda.model.parameter.MergeParameters;
import org.sejda.model.pdf.PdfVersion;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.task.Task;

import com.lowagie.text.pdf.PdfReader;

/**
 * Test for the merge task
 * 
 * @author Andrea Vacondio
 * 
 */
@Ignore
public abstract class MergeTaskTest extends PdfOutEnabledTest implements TestableTask<MergeParameters> {

    private DefaultTaskExecutionService victim = new DefaultTaskExecutionService();

    private SejdaContext context = mock(DefaultSejdaContext.class);
    private MergeParameters parameters;

    @Before
    public void setUp() {
        setUpParameters();
        TestUtils.setProperty(victim, "context", context);
    }

    private void setUpParameters() {
        PdfMergeInput firstInput = new PdfMergeInput(PdfStreamSource.newInstanceNoPassword(getClass().getClassLoader()
                .getResourceAsStream("pdf/test_file.pdf"), "first_test_file.pdf"));
        PdfMergeInput secondInput = new PdfMergeInput(PdfStreamSource.newInstanceNoPassword(getClass().getClassLoader()
                .getResourceAsStream("pdf/large_test.pdf"), "large_test.pdf"));
        parameters = new MergeParameters();
        parameters.setOverwrite(true);
        parameters.setCompress(true);
        parameters.setVersion(PdfVersion.VERSION_1_6);
        parameters.addInput(firstInput);
        parameters.addInput(secondInput);
    }

    @Test
    public void testExecuteMergeAll() throws TaskException, IOException {
        when(context.getTask(parameters)).thenReturn((Task) getTask());
        initializeNewFileOutput(parameters);
        victim.execute(parameters);
        PdfReader reader = null;
        try {
            reader = getReaderFromResultFile();
            assertCreator(reader);
            assertVersion(reader, PdfVersion.VERSION_1_6);
            assertEquals(310, reader.getNumberOfPages());
        } finally {
            nullSafeCloseReader(reader);
        }
    }

    @Test
    public void testExecuteMergeAllCopyFields() throws TaskException, IOException {
        // TODO use input with forms
        when(context.getTask(parameters)).thenReturn((Task) getTask());
        initializeNewFileOutput(parameters);
        TestUtils.setProperty(parameters, "copyFormFields", Boolean.TRUE);
        victim.execute(parameters);
        PdfReader reader = null;
        try {
            reader = getReaderFromResultFile();
            assertCreator(reader);
            assertVersion(reader, PdfVersion.VERSION_1_6);
            assertEquals(310, reader.getNumberOfPages());
        } finally {
            nullSafeCloseReader(reader);
        }
    }

    @Test
    public void testExecuteMergeRanges() throws TaskException, IOException {
        when(context.getTask(parameters)).thenReturn((Task) getTask());
        initializeNewFileOutput(parameters);
        for (PdfMergeInput input : parameters.getInputList()) {
            input.addPageRange(new PageRange(3, 10));
            input.addPageRange(new PageRange(20, 23));
            input.addPageRange(new PageRange(80, 90));
        }
        victim.execute(parameters);
        PdfReader reader = null;
        try {
            reader = getReaderFromResultFile();
            assertCreator(reader);
            assertVersion(reader, PdfVersion.VERSION_1_6);
            assertEquals(25, reader.getNumberOfPages());
        } finally {
            nullSafeCloseReader(reader);
        }
    }

    @Test
    public void testExecuteMergeRangesCopyFields() throws TaskException, IOException {
        when(context.getTask(parameters)).thenReturn((Task) getTask());
        initializeNewFileOutput(parameters);
        TestUtils.setProperty(parameters, "copyFormFields", Boolean.TRUE);
        for (PdfMergeInput input : parameters.getInputList()) {
            input.addPageRange(new PageRange(3, 10));
            input.addPageRange(new PageRange(20, 23));
            input.addPageRange(new PageRange(80, 90));
        }
        victim.execute(parameters);
        PdfReader reader = null;
        try {
            reader = getReaderFromResultFile();
            assertCreator(reader);
            assertVersion(reader, PdfVersion.VERSION_1_6);
            assertEquals(25, reader.getNumberOfPages());
        } finally {
            nullSafeCloseReader(reader);
        }
    }

    protected MergeParameters getParameters() {
        return parameters;
    }
}