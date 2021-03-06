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
package com.epam.deltix.qsrv.hf.tickdb.schema.migration;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.StaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.timebase.messages.schema.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class Test_AddDescriptorChangeMessageBuilder {

    private SchemaChangeMessageBuilder builder = new SchemaChangeMessageBuilder();

    @Test
    public void testAddNewDescriptorMigration() {
        StreamMetaDataChange streamMetaDataChange = getStreamMetaDataChange();

        SchemaChangeMessage actualSchemaChangeMessage = builder.build(streamMetaDataChange, "event", 0l);

        assertThat(actualSchemaChangeMessage, is(getExpectedSchemaChangeMessage()));
    }

    private StreamMetaDataChange getStreamMetaDataChange() {
        StreamMetaDataChange streamMetaDataChange = new StreamMetaDataChange();

        DataField targetField1 = new NonStaticDataField(
                "target_field",
                "target_title",
                new VarcharDataType("UTF8", false, false)
        );
        RecordClassDescriptor targetDescriptor = new RecordClassDescriptor(
                "guid2",
                "targetName",
                "title",
                false,
                null,
                targetField1
        );

        DataField sourceField1 = new NonStaticDataField(
                "field1",
                "field1_title",
                new VarcharDataType("UTF8", false, false)
        );
        DataField sourceField2 = new StaticDataField(
                "field2",
                "field2_title",
                new VarcharDataType("UTF8", false, false),
                "default_value"
        );
        RecordClassDescriptor sourceDescriptor = new RecordClassDescriptor(
                "guid1",
                "sourceName",
                "title",
                false,
                null,
                sourceField2,
                sourceField1
        );

        RecordClassSet targetClassSet = new RecordClassSet();
        // TODO: possible may cause an error of assertion. RecordClassSet uses HashMap as ClassDescriptor's container.
        targetClassSet.setClassDescriptors(targetDescriptor, sourceDescriptor);

        RecordClassSet sourceClassSet = new RecordClassSet();
        sourceClassSet.setClassDescriptors(sourceDescriptor);

        streamMetaDataChange.setMetaData(targetClassSet);
        streamMetaDataChange.setSource(sourceClassSet);

        return streamMetaDataChange;
    }

    private SchemaChangeMessage getExpectedSchemaChangeMessage() {
        SchemaChangeMessage schemaChangeMessage = new SchemaChangeMessage();
        schemaChangeMessage.setTimeStampMs(0);
        schemaChangeMessage.setSymbol("event");

        ObjectArrayList<ClassDescriptorInfo> previousState = new ObjectArrayList<>();
        com.epam.deltix.timebase.messages.schema.RecordClassDescriptor sourceDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        ObjectArrayList<DataFieldInfo> sourceDescriptorFields = new ObjectArrayList<>();

          com.epam.deltix.timebase.messages.schema.VarcharDataType varcharDataType = new com.epam.deltix.timebase.messages.schema.VarcharDataType();
        varcharDataType.setEncodingType(-1000);
        varcharDataType.setEncoding("UTF8");
        varcharDataType.setLength(0);
        varcharDataType.setIsMultiline(false);
        varcharDataType.setIsNullable(false);

          com.epam.deltix.timebase.messages.schema.DataField sourceField1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        sourceField1.setTitle("field1_title");
        sourceField1.setName("field1");
        sourceField1.setDataType(varcharDataType);

          com.epam.deltix.timebase.messages.schema.StaticDataField sourceField2 = new com.epam.deltix.timebase.messages.schema.StaticDataField();
        sourceField2.setTitle("field2_title");
        sourceField2.setName("field2");
        sourceField2.setDataType(varcharDataType);
        sourceField2.setStaticValue("default_value");

        sourceDescriptorFields.addAll(Arrays.asList(sourceField2, sourceField1));

        sourceDescriptor.setTitle("title");
        sourceDescriptor.setName("sourceName");
        sourceDescriptor.setDataFields(sourceDescriptorFields);
        sourceDescriptor.setIsAbstract(false);

        previousState.add(sourceDescriptor);
        schemaChangeMessage.setPreviousState(previousState);

        // expected new state
        ObjectArrayList<ClassDescriptorInfo> newState = new ObjectArrayList<>();
          com.epam.deltix.timebase.messages.schema.RecordClassDescriptor targetDescriptor = new com.epam.deltix.timebase.messages.schema.RecordClassDescriptor();
        targetDescriptor.setName("targetName");
        targetDescriptor.setTitle("title");
        targetDescriptor.setIsAbstract(false);

        ObjectArrayList<DataFieldInfo> targetDescriptorFields = new ObjectArrayList<>();

          com.epam.deltix.timebase.messages.schema.DataField targetField1 = new com.epam.deltix.timebase.messages.schema.NonStaticDataField();
        targetField1.setDataType(varcharDataType);
        targetField1.setName("target_field");
        targetField1.setTitle("target_title");

        targetDescriptorFields.add(targetField1);

        targetDescriptor.setDataFields(targetDescriptorFields);
        newState.addAll(Arrays.asList(sourceDescriptor, targetDescriptor));
        schemaChangeMessage.setNewState(newState);

        ObjectArrayList<SchemaDescriptorChangeActionInfo> changes = new ObjectArrayList<>();
        SchemaDescriptorChangeAction addAction = new SchemaDescriptorChangeAction();
        addAction.setNewState(targetDescriptor);
        addAction.setChangeTypes(SchemaDescriptorChangeType.ADD);
        addAction.setPreviousState(null);

        changes.add(addAction);

        schemaChangeMessage.setDescriptorChangeActions(changes);

        return schemaChangeMessage;
    }
}
