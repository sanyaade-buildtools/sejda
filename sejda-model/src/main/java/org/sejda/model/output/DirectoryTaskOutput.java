/*
 * Created on 30/mag/2010
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
package org.sejda.model.output;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sejda.model.exception.TaskOutputVisitException;
import org.sejda.model.validation.constraint.Directory;

/**
 * Directory output destination.
 * 
 * @author Andrea Vacondio
 * 
 */
public class DirectoryTaskOutput implements MultipleTaskOutput<File> {

    @Directory
    private final File directory;

    /**
     * Creates a new instance of a {@link DirectoryTaskOutput} using the input directory
     * 
     * @param directory
     * @throws IllegalArgumentException
     *             if the input directory is null or not a directory
     */
    public DirectoryTaskOutput(File directory) {
        if (directory == null || !directory.isDirectory()) {
            throw new IllegalArgumentException("A not null directory instance is expected.");
        }
        this.directory = directory;
    }

    @Override
    public File getDestination() {
        return directory;
    }

    @Override
    public void accept(TaskOutputDispatcher writer) throws TaskOutputVisitException {
        try {
            writer.dispatch(this);
        } catch (IOException e) {
            throw new TaskOutputVisitException("Exception dispatching the file task output.", e);
        }
    }

    @Override
    public String toString() {
        return directory.toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(directory).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DirectoryTaskOutput)) {
            return false;
        }
        DirectoryTaskOutput output = (DirectoryTaskOutput) other;
        return new EqualsBuilder().append(directory, output.getDestination()).isEquals();
    }
}
