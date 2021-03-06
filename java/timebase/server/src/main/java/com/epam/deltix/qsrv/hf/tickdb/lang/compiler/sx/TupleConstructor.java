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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class TupleConstructor extends CompiledComplexExpression {
    private static CompiledExpression []    cat (
        CompiledExpression        timestampInitializer,
        CompiledExpression        symbolInitializer,
        CompiledExpression        typeInitializer,
        CompiledExpression ...    nonStaticInitializers
    )
    {
        int                         n = nonStaticInitializers.length;
        CompiledExpression [] a = new CompiledExpression [n + 3];

        a [0] = timestampInitializer;
        a [1] = symbolInitializer;
        a [2] = typeInitializer;

        if (n != 0)
            System.arraycopy (nonStaticInitializers, 0, a, 3, n);

        return (a);
    }

    public TupleConstructor (
        ClassDataType                   type,
        CompiledExpression        timestampInitializer,
        CompiledExpression        symbolInitializer,
        CompiledExpression        typeInitializer,
        CompiledExpression ...    nonStaticInitializers
    )
    {
        super (type, cat (timestampInitializer, symbolInitializer, typeInitializer, nonStaticInitializers));
    }

    public RecordClassDescriptor        getClassDescriptor () {
        return (((ClassDataType) type).getDescriptors () [0]);
    }

    public CompiledExpression     getTimestampInitializer () {
        return (args [0]);
    }

    public CompiledExpression     getSymbolInitializer () {
        return (args [1]);
    }

    public CompiledExpression     getTypeInitializer () {
        return (args [2]);
    }

    public CompiledExpression []  getNonStaticInitializers () {
        int                                 n = args.length - 3;
        CompiledExpression []         ret = new CompiledExpression [n];

        System.arraycopy (args, 3, ret, 0, n);
        
        return (ret);
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) &&
            getClassDescriptor ().equals (((TupleConstructor) obj).getClassDescriptor ());
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + getClassDescriptor ().hashCode ();
    }

    @Override
    protected void print (StringBuilder out) {
        out.append ("new ");
        out.append (getClassDescriptor ().getName ());
        out.append (" (");
        printArgs (out, 3);
        out.append (")");
    }
}
