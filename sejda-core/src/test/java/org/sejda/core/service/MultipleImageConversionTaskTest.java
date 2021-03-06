/*
 * Created on 08/mar/2013
 * Copyright 2011 by Andrea Vacondio (andrea.vacondio@gmail.com).
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
import static org.junit.Assert.fail;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.sejda.ImageTestUtils;
import org.sejda.core.TestListenerFactory;
import org.sejda.core.TestListenerFactory.TestListenerFailed;
import org.sejda.core.notification.context.ThreadLocalNotificationContext;
import org.sejda.core.support.io.IOUtils;
import org.sejda.model.output.DirectoryTaskOutput;
import org.sejda.model.parameter.image.AbstractPdfToMultipleImageParameters;
import org.sejda.model.pdf.page.PageRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrea Vacondio
 * 
 */
@Ignore
public abstract class MultipleImageConversionTaskTest<T extends AbstractPdfToMultipleImageParameters>
        extends BaseTaskTest<T> implements TestableTask<T> {
    private static Logger LOG = LoggerFactory.getLogger(MultipleImageConversionTaskTest.class);

    abstract T getMultipleImageParametersWithoutSource();

    @Test
    public void testExecuteEncryptedStreamToMultipleImage() throws IOException {
        AbstractPdfToMultipleImageParameters parameters = getMultipleImageParametersWithoutSource();
        parameters.setSource(encryptedInput());
        doExecute(parameters, 4);
    }

    @Test
    public void testExecuteStreamToMultipleImage() throws IOException {
        AbstractPdfToMultipleImageParameters parameters = getMultipleImageParametersWithoutSource();
        parameters.setSource(customInput("pdf/test_jpg.pdf"));
        doExecute(parameters, 1);
    }

    @Test
    public void testExecuteStreamToMultipleImageWithPageSelection() throws IOException {
        AbstractPdfToMultipleImageParameters parameters = getMultipleImageParametersWithoutSource();
        parameters.setSource(shortInput());
        parameters.addPageRange(new PageRange(2, 3));
        doExecute(parameters, 2);
    }

    @Test
    public void testWrongPageSelection() {
        AbstractPdfToMultipleImageParameters parameters = getMultipleImageParametersWithoutSource();
        parameters.setSource(shortInput());
        parameters.addPageRange(new PageRange(10));
        TestListenerFailed failListener = TestListenerFactory.newFailedListener();
        ThreadLocalNotificationContext.getContext().addListener(failListener);
        execute(parameters);
        assertTrue(failListener.isFailed());
    }

    private void doExecute(AbstractPdfToMultipleImageParameters parameters, int size) throws IOException {
        testContext.directoryOutputTo(parameters);
        execute(parameters);
        testContext.assertTaskCompleted();
        testContext.assertOutputSize(size).forEachRawOutput(p -> {
            try {
                RenderedImage ri = ImageTestUtils.loadImage(p.toFile());
                assertTrue(ri.getHeight() > 0);
                assertTrue(ri.getWidth() > 0);
            } catch (Exception e) {
                LOG.error("Test failed", e);
                fail(e.getMessage());
            }
        });
    }

    @Ignore("In place of a better way to check image quality with automated tests")
    public void testImageConversion() {
        AbstractPdfToMultipleImageParameters parameters = getMultipleImageParametersWithoutSource();
        File out = IOUtils.createTemporaryFolder();
        parameters.setOutput(new DirectoryTaskOutput(out));
        execute(parameters);
        System.out.println("Images generated to: " + out.getAbsolutePath());
    }
}
