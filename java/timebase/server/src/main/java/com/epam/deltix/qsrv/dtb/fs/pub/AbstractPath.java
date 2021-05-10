package com.epam.deltix.qsrv.dtb.fs.pub;

import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.CheckReturnValue;
import java.io.*;

/**
 *
 */
public interface AbstractPath {
    AbstractFileSystem   getFileSystem ();

    /** @return immutable string that identifies this path */
    String               getPathString ();

    /**
     * @return file/folder name (without path)
     */
    String               getName ();

    /**
     * @return names of files and folders (name only, without full path). Sorted.
     */
    String []            listFolder () throws IOException;

    AbstractPath         append (String name);
    
    AbstractPath         getParentPath ();
    
    InputStream          openInput (long offset) throws IOException;

    /**
     * Same as {@link #openInput(long)} but guaranties to load only first {@code length} bytes of data (if available).
     *
     * @param offset file positions
     * @param length number of bytes reader want to read
     */

    default InputStream          openInput (long offset, long length) throws IOException {
        return openInput(offset);
    }

    /** @param size when greater than zero specifies output file size (strict),  when zero - size is unknown (usually small) */
    @CheckReturnValue
    OutputStream         openOutput (long size) throws IOException;

    /**
     * Opens output stream to append data to existing file.
     */
    @CheckReturnValue
    default OutputStream         openOutputForAppend() throws IOException {
        throw new NotImplementedException("openOutputForAppend is not implemented for this FS");
    }
    
    long                 length ();
    
    boolean              isFile ();
    
    boolean              isFolder ();

    boolean              exists ();
    
    void                 makeFolder () throws IOException;

    void                 makeFolderRecursive () throws IOException;

    void                 moveTo (AbstractPath newPath) throws IOException;

    /**
     * Renames the file denoted by this abstract pathname.
     *
     * @param newName The new name for the abstract path
     * @return new AbstractPath if rename was done successfully.
     * @throws IOException
     */
    AbstractPath         renameTo (String newName) throws IOException;

    void                 deleteExisting () throws IOException;
    
    void                 deleteIfExists () throws IOException;

    /**
     * @return timestamp (ms) of last file modification
     */
    default long         getModificationTime() throws IOException {
        throw new NotImplementedException("getModificationTime is not implemented for this FS");
    }

    /**
     * Sets timestamp of last file modification.
     */
    default void         setModificationTime(long timestamp) throws IOException {
        throw new NotImplementedException("setModificationTime is not implemented for this FS");
    }

    /**
     * This method can be use to change metadata caching policy.
     * If caching is turned on then some query operations will return result without querying the remote system.
     * However this may produce incorrect behavior for operations that expect concurrent access.
     */
    default void         setCacheMetadata(boolean cache) {}
}