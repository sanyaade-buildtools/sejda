/*
 * Created on 24/ago/2015
 * Copyright 2015 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * This file is part of Sejda.
 *
 * Sejda is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sejda is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Sejda.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sejda.impl.sambox.component.split;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.function.Supplier;

import org.sejda.core.support.prefix.model.NameGenerationRequest;
import org.sejda.impl.sambox.component.PagesExtractor;
import org.sejda.impl.sambox.component.optimizaton.ImagesHitter;
import org.sejda.impl.sambox.component.optimizaton.ResourceDictionaryCleaner;
import org.sejda.model.exception.TaskExecutionException;
import org.sejda.model.exception.TaskIOException;
import org.sejda.model.parameter.SplitBySizeParameters;
import org.sejda.model.split.NextOutputStrategy;
import org.sejda.sambox.cos.COSDictionary;
import org.sejda.sambox.cos.COSName;
import org.sejda.sambox.output.ExistingPagesSizePredictor;
import org.sejda.sambox.output.WriteOption;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PDResources;
import org.sejda.util.IOUtils;

/**
 * Splitter implementation that tries to split a document at roughly a given size
 * 
 * @author Andrea Vacondio
 */
public class SizePdfSplitter extends AbstractPdfSplitter<SplitBySizeParameters> {

    private static final WriteOption[] COMPRESSED_OPTS = new WriteOption[] { WriteOption.COMPRESS_STREAMS,
            WriteOption.XREF_STREAM };

    private static final int PDF_HEADER_SIZE = 15;
    // euristic trailer ID size
    private static final int ID_VALUE_SIZE = 70;
    // euristic overhead per page (ex. page ref in the page tree)
    private static final int PAGE_OVERHEAD = 10;

    private OutputSizeStrategy nextOutputStrategy;

    public SizePdfSplitter(PDDocument document, SplitBySizeParameters parameters, boolean optimize) {
        super(document, parameters, optimize, parameters.discardOutline());
        if (parameters.isCompress()) {
            this.nextOutputStrategy = new OutputSizeStrategy(document, parameters, optimize, () -> {
                return new ExistingPagesSizePredictor(WriteOption.COMPRESS_STREAMS, WriteOption.XREF_STREAM);
            });
        } else {
            this.nextOutputStrategy = new OutputSizeStrategy(document, parameters, optimize);
        }
    }

    @Override
    NameGenerationRequest enrichNameGenerationRequest(NameGenerationRequest request) {
        return request;
    }

    @Override
    NextOutputStrategy nextOutputStrategy() {
        return nextOutputStrategy;
    }

    @Override
    protected void onOpen(int page) throws TaskIOException {
        nextOutputStrategy.newPredictor();
        nextOutputStrategy.addPage(page);
    }

    @Override
    protected void onRetain(int page) throws TaskIOException {
        nextOutputStrategy.addPage(page + 1);
    }

    @Override
    protected void onClose(int page) {
        nextOutputStrategy.closePredictor();

    }

    @Override
    protected PagesExtractor supplyPagesExtractor(PDDocument document) {
        return new PagesExtractor(document) {
            @Override
            public void setCompress(boolean compress) {
                if (compress) {
                    destinationDocument().addWriteOption(COMPRESSED_OPTS);
                } else {
                    destinationDocument().removeWriteOption(COMPRESSED_OPTS);
                }
            }
        };
    }

    static class OutputSizeStrategy implements NextOutputStrategy {
        private long sizeLimit;
        private PDDocument document;
        private ExistingPagesSizePredictor predictor;
        private Supplier<ExistingPagesSizePredictor> predictorSupplier = () -> {
            return new ExistingPagesSizePredictor();
        };
        private boolean optimize;
        private ImagesHitter hitter = new ImagesHitter();
        private ResourceDictionaryCleaner cleaner = new ResourceDictionaryCleaner();

        OutputSizeStrategy(PDDocument document, SplitBySizeParameters parameters, boolean optimize) {
            this.sizeLimit = parameters.getSizeToSplitAt();
            this.document = document;
            this.optimize = optimize;
        }

        OutputSizeStrategy(PDDocument document, SplitBySizeParameters parameters, boolean optimize,
                Supplier<ExistingPagesSizePredictor> predictorSupplier) {
            this(document, parameters, optimize);
            this.predictorSupplier = predictorSupplier;
        }

        public void newPredictor() throws TaskIOException {
            try {
                predictor = predictorSupplier.get();
                predictor.addIndirectReferenceFor(document.getDocumentInformation());
                predictor.addIndirectReferenceFor(document.getDocumentCatalog().getViewerPreferences());
            } catch (IOException e) {
                throw new TaskIOException("Unable to initialize the pages size predictor", e);
            }
        }

        public void addPage(int page) throws TaskIOException {
            try {
                if (page <= document.getNumberOfPages()) {
                    predictor.addPage(copyOf(document.getPage(page - 1)));
                }
            } catch (IOException e) {
                throw new TaskIOException("Unable to simulate page " + page + " addition", e);
            }
        }

        private PDPage copyOf(PDPage page) {
            PDPage copy = new PDPage(page.getCOSObject().duplicate());
            copy.setCropBox(page.getCropBox());
            copy.setMediaBox(page.getMediaBox());
            copy.setResources(page.getResources());
            copy.setRotation(page.getRotation());
            if (optimize) {
                // each page must have it's own resource dic and it's own xobject name dic
                // so we don't optimize shared resource dic or xobjects name dictionaries
                COSDictionary resources = ofNullable(copy.getResources().getCOSObject()).map(COSDictionary::duplicate)
                        .orElseGet(COSDictionary::new);
                // resources are cached in the PDPage so make sure they are replaced
                copy.setResources(new PDResources(resources));
                ofNullable(resources.getDictionaryObject(COSName.XOBJECT)).filter(b -> b instanceof COSDictionary)
                        .map(b -> (COSDictionary) b).map(COSDictionary::duplicate)
                        .ifPresent(d -> resources.setItem(COSName.XOBJECT, d));
                hitter.accept(copy);
                cleaner.clean(copy);
            }
            return copy;
        }

        public void closePredictor() {
            IOUtils.closeQuietly(predictor);
            this.predictor = null;
        }

        @Override
        public void ensureIsValid() throws TaskExecutionException {
            if (sizeLimit < 1) {
                throw new TaskExecutionException(
                        String.format("Unable to split at %d, a positive size is required.", sizeLimit));
            }
        }

        @Override
        public boolean isOpening(Integer page) {
            return predictor == null || !predictor.hasPages();
        }

        @Override
        public boolean isClosing(Integer page) throws TaskIOException {
            try {
                long currentPageSize = predictor.predictedPagesSize();
                return (PDF_HEADER_SIZE + ID_VALUE_SIZE + currentPageSize + predictor.predictedXrefTableSize()
                        + documentFooterSize(currentPageSize) + (predictor.pages() * PAGE_OVERHEAD)) > sizeLimit;
            } catch (IOException e) {
                throw new TaskIOException("Unable to simulate page " + page + " addition", e);
            }
        }

        private int documentFooterSize(long documentSize) {
            // startxref + %%EOF + few EOL
            return 17 + Long.toString(documentSize).length();
        }
    }
}
