/*
 * Created on 28/mag/2010
 *
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * This file is part of the Sejda source code
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sejda.core.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.sejda.TestUtils;
import org.sejda.core.TestListenerFactory;
import org.sejda.core.TestListenerFactory.TestListenerStart;
import org.sejda.core.context.DefaultSejdaContext;
import org.sejda.core.context.SejdaContext;
import org.sejda.core.notification.context.GlobalNotificationContext;
import org.sejda.model.exception.TaskException;
import org.sejda.model.exception.TaskExecutionException;
import org.sejda.model.output.FileTaskOutput;
import org.sejda.model.output.SingleTaskOutput;
import org.sejda.model.output.StreamTaskOutput;
import org.sejda.model.parameter.base.TaskParameters;
import org.sejda.model.pdf.PdfVersion;
import org.sejda.model.task.Task;
import org.sejda.model.task.TestTaskParameter;

/**
 * Test unit for the {@link DefaultTaskExecutionService}
 * 
 * @author Andrea Vacondio
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DefaultTaskExecutionServiceTest {

    private DefaultTaskExecutionService victim = new DefaultTaskExecutionService();
    private TestTaskParameter parameters = new TestTaskParameter();
    private SejdaContext context = mock(DefaultSejdaContext.class);
    private Task task = mock(Task.class);

    @Before
    public void setUp() throws TaskException {
        OutputStream stream = mock(OutputStream.class);
        parameters.setOutput(new StreamTaskOutput(stream));
        when(context.getTask(Matchers.any(TaskParameters.class))).thenReturn(task);
        when(context.isValidation()).thenReturn(Boolean.TRUE);
    }

    @Test
    public void testExecute() {
        TestListenerStart listener = TestListenerFactory.newStartListener();
        GlobalNotificationContext.getContext().addListener(listener);
        victim.execute(parameters);
        assertTrue(listener.isStarted());
    }

    @Test
    public void testInvalidParameters() throws TaskException {
        parameters.setVersion(PdfVersion.VERSION_1_4);
        parameters.setCompress(true);
        victim.execute(parameters);
        verify(task, never()).before(parameters);
    }

    @Test
    public void testNegativeBeforeExecution() throws TaskException {
        doThrow(new TaskExecutionException("Mock exception")).when(task).before(Matchers.any(TaskParameters.class));
        SingleTaskOutput<?> output = mock(SingleTaskOutput.class);
        parameters.setOutput(output);
        TestUtils.setProperty(victim, "context", context);
        victim.execute(parameters);
        verify(task).before(parameters);
        verify(task).after();
        verify(task, never()).execute(parameters);
    }

    @Test
    public void testNegativeValidationExecution() throws TaskException {
        TestUtils.setProperty(victim, "context", context);
        File file = mock(File.class);
        when(file.isFile()).thenReturn(Boolean.TRUE);
        when(file.getName()).thenReturn("name.pdf");
        when(file.exists()).thenReturn(Boolean.TRUE);
        parameters.setOutput(new FileTaskOutput(file));
        when(file.isFile()).thenReturn(Boolean.FALSE);
        victim.execute(parameters);
        verify(task, never()).before(parameters);
        verify(task, never()).after();
        verify(task, never()).execute(parameters);
    }
}
