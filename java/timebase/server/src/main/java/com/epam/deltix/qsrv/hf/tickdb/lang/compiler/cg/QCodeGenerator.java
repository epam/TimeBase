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

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.BasicStreamSelector;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs.SingleMessagePreparedQuery;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.util.jcg.*;
import com.epam.deltix.util.lang.JavaCompilerHelper;

/**
 *
 */
public abstract class QCodeGenerator {
    static void                 move (
        QValue                      from,
        QValue                      to,
        JCompoundStatement          addTo
    )
    {
        to.type.move (from, to, addTo);
    }

    public static PreparedQuery        createQuery (CompiledQuery cq) {
        if (cq instanceof StreamSelector)
            return (createStreamSelector ((StreamSelector) cq));

        if (cq instanceof CompiledFilter) {
            CompiledFilter  cf = (CompiledFilter) cq;
            PreparedQuery   source = createQuery (cf.source);
            JavaCompilerHelper helper = new JavaCompilerHelper (CompiledFilter.class.getClassLoader ());
            return (new FilterGenerator ((CompiledFilter) cq).finish (helper, source));
        }

        if (cq instanceof SingleMessageSource)
            return (new SingleMessagePreparedQuery ());
        
        throw new UnsupportedOperationException (cq.getClass ().getName ());
    }

    private static PreparedQuery       createStreamSelector (StreamSelector ss) {
        return (new BasicStreamSelector (ss.mode, ss.streams));
    }
}
