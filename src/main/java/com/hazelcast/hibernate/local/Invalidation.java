/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.hibernate.local;

import com.hazelcast.nio.IOUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

/**
 * @mdogan 11/12/12
 */
public class Invalidation implements DataSerializable {

    private Object key;
    private Object version;

    public Invalidation() {
    }

    public Invalidation(final Object key, final Object version) {
        this.key = key;
        this.version = version;
    }

    public Object getKey() {
        return key;
    }

    public Object getVersion() {
        return version;
    }

    public void writeData(final ObjectDataOutput out) throws IOException {
        IOUtil.writeNullableObject(out, key);
        IOUtil.writeNullableObject(out, version);
    }

    public void readData(final ObjectDataInput in) throws IOException {
        key = IOUtil.readNullableObject(in);
        version = IOUtil.readNullableObject(in);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Invalidation");
        sb.append("{key=").append(key);
        sb.append(", version=").append(version);
        sb.append('}');
        return sb.toString();
    }
}
