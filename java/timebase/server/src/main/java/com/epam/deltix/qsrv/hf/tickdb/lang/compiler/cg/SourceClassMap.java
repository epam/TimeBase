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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataFieldRef;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import java.util.*;

/**
 *
 */
class SourceClassMap {
    public final RecordClassDescriptor []           concreteTypes;

    private final Map <RecordClassDescriptor, ClassSelectorInfo>  map =
        new HashMap <RecordClassDescriptor, ClassSelectorInfo> ();

    private final Set <TypeCheckInfo>               typeChecks =
        new HashSet <TypeCheckInfo> ();

    private ClassSelectorInfo                        getOrCreate (
        RecordClassDescriptor                           type
    )
    {
        ClassSelectorInfo           csi = map.get (type);

        if (csi == null) {
            RecordClassDescriptor   parentRCD = type.getParent ();

            ClassSelectorInfo       parentCSI =
                parentRCD == null ?
                    null :
                    getOrCreate (parentRCD);

            csi = new ClassSelectorInfo (parentCSI, type);

            map.put (type, csi);
        }
        
        return (csi);
    }

    public SourceClassMap (RecordClassDescriptor [] concreteTypes) {
        this.concreteTypes = concreteTypes;

        int                         n = concreteTypes.length;

        for (int ii = 0; ii < n; ii++) {
            RecordClassDescriptor   rcd = concreteTypes [ii];
            ClassSelectorInfo       csi = getOrCreate (rcd);
            csi.ordinal = ii;
        }
    }

    public Collection <ClassSelectorInfo>       allClassInfo () {
        return (map.values ());        
    }

    public Collection <TypeCheckInfo>           allTypeChecks () {
        return (typeChecks);
    }

    public ClassSelectorInfo                    getSelectorInfo (
        RecordClassDescriptor                       rcd
    )
    {
        return (map.get (rcd));
    }

    public void                                 discoverFieldSelectors (
        CompiledExpression                          e
    )
    {
        if (e instanceof FieldSelector)
            register ((FieldSelector) e);
        else if (e instanceof TypeCheck)
            register ((TypeCheck) e);

        if (e instanceof CompiledComplexExpression) {
            CompiledComplexExpression   ccx = (CompiledComplexExpression) e;

            for (CompiledExpression arg : ccx.args)
                discoverFieldSelectors (arg);
        }
    }

    private void                                register (
        TypeCheck                                   typeCheck
    )
    {
        typeChecks.add (new TypeCheckInfo (typeCheck));
    }

    private void                                register (
        FieldSelector                               fieldSelector
    )
    {
        DataFieldRef            fieldRef = fieldSelector.fieldRef;
        DataField               df = fieldRef.field;

        if (!(df instanceof NonStaticDataField))
            return;
        
        for (ClassSelectorInfo csi : map.values ())
            csi.nonStaticFieldUsedFrom (fieldSelector);
    }
}
