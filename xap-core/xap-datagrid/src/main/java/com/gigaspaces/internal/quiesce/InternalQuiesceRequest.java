/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.gigaspaces.internal.quiesce;

import com.gigaspaces.admin.quiesce.QuiesceToken;
import com.gigaspaces.admin.quiesce.QuiesceTokenFactory;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.serialization.SmartExternalizable;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Boris
 * @since 10.1.0
 */
@com.gigaspaces.api.InternalApi
public class InternalQuiesceRequest implements SmartExternalizable {
    private static final long serialVersionUID = 1L;
    private String description;
    private QuiesceToken token;


    public InternalQuiesceRequest() {
    }

    public InternalQuiesceRequest(String description) {
        this.description = description;
        this.token = QuiesceTokenFactory.createUUIDToken();
    }

    public InternalQuiesceRequest(String description, QuiesceToken token) {
        this.description = description;
        this.token = token;
    }

    public String getDescription() {
        return description;
    }

    public QuiesceToken getToken() {
        return token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InternalQuiesceRequest that = (InternalQuiesceRequest) o;

        return token.equals(that.token);

    }

    @Override
    public int hashCode() {
        return token.hashCode();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeObject(out, token);
        IOUtils.writeString(out, description);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        token = IOUtils.readObject(in);
        description = IOUtils.readString(in);
    }
}
