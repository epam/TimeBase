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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public abstract class AttributeDef extends Element {
    public final String         id;
    public final String         title;
    public final String         comment;
    
    protected AttributeDef (String id, String title, String comment, long location) {
        super (location);
        this.id = id;
        this.title = title;
        this.comment = comment;
    }
    
    protected final void        printHeader (StringBuilder s) {
        GrammarUtil.escapeVarId (id, s);
        
        if (title != null) {
            s.append (' ');
            GrammarUtil.escapeStringLiteral (title, s);
        }
    }
    
    protected final void        printComment (StringBuilder s) {
        if (comment != null) {
            s.append (" COMMENT ");
            GrammarUtil.escapeStringLiteral (comment, s);
        }
    }
}
