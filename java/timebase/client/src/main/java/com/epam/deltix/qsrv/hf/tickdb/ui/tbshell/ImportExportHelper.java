/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaMapping;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import com.epam.deltix.util.time.Interval;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class ImportExportHelper {

    public static void filterMessageFile(@NotNull File src, @NotNull DXTickStream stream, @NotNull Selector selector) throws IOException {
        if (!Util.QUIET)
            System.out.println("Importing; hit <Enter> to abort ...");
        DBLock lock = null;
        try {
            if (TickDBShell.SECURITIES_STREAM.equalsIgnoreCase(stream.getKey()))
                lock = stream.tryLock(LockType.WRITE, 5000L);

            RecordClassDescriptor[] outTypes = stream.isFixedType() ?
                    new RecordClassDescriptor[]{stream.getFixedType()} :
                    stream.getPolymorphicDescriptors();

            MessageFileHeader header = Protocol.readHeader(src);

            boolean firstVersion = header.version == 0;

            RecordClassDescriptor[] inTypes = null;

            if (firstVersion) {
                throw new IllegalArgumentException("Supported built-in messages only.");
            } else if (!firstVersion) {
                inTypes = header.getTypes();
                if (!isCompatible(inTypes, outTypes)) {
                    throw new IllegalArgumentException("Input types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \noutput types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                }
            }


            final TickLoader loader = stream.createLoader(new LoadingOptions(!firstVersion));

            final ConsumableMessageSource<InstrumentMessage> reader = MessageReader2.createRaw(src);
            if (isSelectorChanged(selector)) {
                loadData(reader, loader, stream, selector, getSchemaMapping(inTypes, outTypes));
            } else {
                loadData(reader, loader, stream, getSchemaMapping(inTypes, outTypes));
            }
        } finally {
            if (lock != null)
                lock.release();
        }
    }

    public static void filterMessageFile(@NotNull File src, @NotNull DXTickStream stream) throws IOException {
        if (!Util.QUIET)
            System.out.println("Importing; hit <Enter> to abort ...");
        DBLock lock = null;
        try {
            if (TickDBShell.SECURITIES_STREAM.equalsIgnoreCase(stream.getKey()))
                lock = stream.tryLock(LockType.WRITE, 5000L);

            RecordClassDescriptor[] outTypes = stream.isFixedType() ?
                    new RecordClassDescriptor[]{stream.getFixedType()} :
                    stream.getPolymorphicDescriptors();

            MessageFileHeader header = Protocol.readHeader(src);

            boolean firstVersion = header.version == 0;

            RecordClassDescriptor[] inTypes = null;

            if (firstVersion) {
                throw new IllegalArgumentException("Supported built-in messages only.");
            } else if (!firstVersion) {
                inTypes = header.getTypes();
                if (!isCompatible(inTypes, outTypes)) {
                    throw new IllegalArgumentException("Input types (" + MessageProcessor.toDetailedString(inTypes) +
                            ") \nis not compatible with \noutput types (" +
                            MessageProcessor.toDetailedString(outTypes) + ")");
                }
            }


            final TickLoader loader = stream.createLoader(new LoadingOptions(!firstVersion));

            final ConsumableMessageSource<InstrumentMessage> reader =
                    MessageReader2.createRaw(src);

            loadData(reader, loader, stream, getSchemaMapping(inTypes, outTypes));
        } finally {
            if (lock != null)
                lock.release();
        }
    }

    public static boolean isCompatible(@NotNull RecordClassDescriptor[] inTypes, @NotNull RecordClassDescriptor[] outTypes) {
        HashSet<String> set = new HashSet<>();
        for (RecordClassDescriptor outType : outTypes) {
            set.add(outType.getName());
        }
//        ClassMappings classMappings = new ClassMappings();
//        for (RecordClassDescriptor inType : inTypes) {
//            String newName = classMappings.getClassName(inType.getName());
//            if (!set.contains(inType.getName())) {
//                if (newName == null) {
//                    return false;
//                } else if (!set.contains(newName)) {
//                    return false;
//                }
//            }
//        }
        return true;
    }

    public static void writeStreamsToFile(@NotNull File file,
                                          @NotNull DXTickDB db,
                                          @NotNull DXTickStream[] streams,
                                          SelectionOptions options)
            throws IOException, ClassNotFoundException {
        Interval periodicity = streams.length == 1 ? streams[0].getPeriodicity().getInterval() : null;
        if (options == null) {
            options = new SelectionOptions(true, false);
        }
        try (MessageWriter2 writer = MessageWriter2.create(
                file,
                periodicity,
                options.raw ? null : TypeLoaderImpl.DEFAULT_INSTANCE,
                TickDBShell.collectTypes(streams)
        )) {
            export(db, streams, writer, options);
        }
    }

    private static void loadData(ConsumableMessageSource<InstrumentMessage> reader,
                                 MessageChannel<InstrumentMessage> writer,
                                 DXTickStream stream, @NotNull Selector selector,
                                 SchemaMapping schemaMapping) {
        HashMap<RecordClassDescriptor, SchemaConverter> changes = new HashMap<>();
        SchemaAnalyzer analyzer = new SchemaAnalyzer(schemaMapping);
        ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();

        if (!Util.QUIET)
            cpi.setTotalWork(1);

        long inCount = 0;
        long outCount = 0;

        while (reader.next()) {
            RawMessage message = (RawMessage) reader.getMessage();
            RecordClassDescriptor descriptor = find(stream.getTypes(), message.type);
            SchemaConverter converter;
            if (selector == null) {
                selector = new Selector(null);
            }
            if (message.getTimeStampMs() < selector.getTime()) // filter messages by start time
                continue;

            inCount++;

            if (inCount % 1000 == 0) {
                if (!Util.QUIET)
                    cpi.setWorkDone(reader.getProgress());
            }


            if (selector.accept(message)) {
                outCount++;
                if (changes.containsKey(message.type)) {
                    converter = changes.get(message.type);
                } else {
                    MetaDataChange change = analyzer.getChanges(new RecordClassSet(new RecordClassDescriptor[]{message.type}),
                            MetaDataChange.ContentType.Fixed,
                            new RecordClassSet(new RecordClassDescriptor[]{descriptor}),
                            MetaDataChange.ContentType.Fixed);
                    converter = new SchemaConverter(change);
                    changes.put(message.type, converter);
                }
                writer.send(converter.convert(message));
            }

            if (selector.enough(message))
                break;
        }

        if (!Util.QUIET)
            System.out.printf("\nIn: %,d messages; out: %,d messages.\n", inCount, outCount);
    }

    private static void loadData(ConsumableMessageSource<InstrumentMessage> reader,
                                 MessageChannel<InstrumentMessage> writer,
                                 DXTickStream stream, SchemaMapping schemaMapping) {
        HashMap<RecordClassDescriptor, SchemaConverter> changes = new HashMap<>();
        SchemaAnalyzer analyzer = new SchemaAnalyzer(schemaMapping);
        ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();

        if (!Util.QUIET)
            cpi.setTotalWork(1);

        long inCount = 0;
        long outCount = 0;

        while (reader.next()) {
            RawMessage message = (RawMessage) reader.getMessage();
            RecordClassDescriptor descriptor = find(stream.getTypes(), message.type);
            SchemaConverter converter;

            inCount++;
            if (inCount % 1000 == 0) {
                if (!Util.QUIET)
                    cpi.setWorkDone(reader.getProgress());
            }
            outCount++;
            if (changes.containsKey(message.type)) {
                converter = changes.get(message.type);
            } else {
                MetaDataChange change = analyzer.getChanges(new RecordClassSet(new RecordClassDescriptor[]{message.type}),
                        MetaDataChange.ContentType.Fixed,
                        new RecordClassSet(new RecordClassDescriptor[]{descriptor}),
                        MetaDataChange.ContentType.Fixed);
                converter = new SchemaConverter(change);
                changes.put(message.type, converter);
            }
            writer.send(converter.convert(message));

        }

        if (!Util.QUIET)
            System.out.printf("\nIn: %,d messages; out: %,d messages.\n", inCount, outCount);
    }

    private static void export(DXTickDB db, DXTickStream[] src,
                               MessageChannel<InstrumentMessage> dest, SelectionOptions options) {
        long[] tr = TickTools.getTimeRange(src);

        if (tr == null) {
            System.out.println ("No data in source.");
            return;
        }

        ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();

        System.out.println("Copying ...");

        try (MessageSource<InstrumentMessage> cur = db.select(tr[0], options, src)) {
            TickTools.copy(cur, tr, dest, cpi);
        } finally {
            System.out.println();
        }
    }

    private static SchemaMapping getSchemaMapping(RecordClassDescriptor[] inTypes, RecordClassDescriptor[] outTypes) {
        SchemaMapping mapping = new SchemaMapping();
        //todo
//        ClassMappings classMappings = new ClassMappings();
//        HashMap<String, RecordClassDescriptor> map = new HashMap<>();
//        if (outTypes != null) {
//            for (RecordClassDescriptor outType : outTypes) {
//                map.put(outType.getName(), outType);
//            }
//        }
//        if (inTypes != null) {
//            for (RecordClassDescriptor inType : inTypes) {
//                String name = inType.getName();
//                String classMapping = classMappings.getClassName(name);
//                if (classMapping != null) {
//                    mapping.descriptors.put(inType.getGuid(), map.get(classMapping).getGuid());
//                }
//            }
//        }
        return mapping;
    }

    private static RecordClassDescriptor find(RecordClassDescriptor[] types, RecordClassDescriptor type) {
        //todo:
//        ClassMappings classMappings = new ClassMappings();
//        String newName = classMappings.getClassName(type.getName());
//        for (RecordClassDescriptor desc : types) {
//            if (type.getName().compareTo(desc.getName()) == 0) {
//                return desc;
//            }
//            if (newName != null && newName.compareTo(desc.getName()) == 0) {
//                return desc;
//            }
//        }
        return null;
    }

    private static boolean isSelectorChanged(Selector selector) {
        return selector.getTime() != Selector.DEFAULT_TIME || selector.getEndtime() != Selector.DEFAULT_ENDTIME ||
                selector.getSelectedEntities() != null;
    }
}
