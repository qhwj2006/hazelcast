/*
 * Copyright (c) 2008-2012, Hazel Bilisim Ltd. All Rights Reserved.
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

package com.hazelcast.cluster;

import com.hazelcast.instance.Node;
import com.hazelcast.spi.impl.NodeServiceImpl;
import com.hazelcast.spi.Operation;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.Connection;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Level;

public class SetMasterOperation extends Operation implements JoinOperation {

    protected Address masterAddress;

    public SetMasterOperation() {
    }

    public SetMasterOperation(final Address originAddress) {
        super();
        this.masterAddress = originAddress;
    }

    public void run() {
        ClusterService cm = (ClusterService) getService();
        NodeServiceImpl nodeService = (NodeServiceImpl) getNodeService();
        Node node = nodeService.getNode();
        ILogger logger = nodeService.getLogger(SetMasterOperation.class.getName());
        if (!node.joined() && !node.getThisAddress().equals(masterAddress)) {
            logger.log(Level.FINEST, "Handling master response: " + this);
            final Address currentMaster = node.getMasterAddress();
            if (currentMaster != null && !currentMaster.equals(masterAddress)) {
                final Connection conn = node.connectionManager.getConnection(currentMaster);
                if (conn != null && conn.live()) {
                    logger.log(Level.FINEST, "Ignoring master response " + this +
                                             " since node has an active master: " + currentMaster);
                    return;
                }
            }
            node.setMasterAddress(masterAddress);
            final Connection connMaster = node.connectionManager.getOrConnect(masterAddress);
            if (connMaster != null) {
                cm.sendJoinRequest(masterAddress, true);
            }
        }
    }

    public Address getMasterAddress() {
        return masterAddress;
    }

    public void readInternal(final DataInput in) throws IOException {
        masterAddress = new Address();
        masterAddress.readData(in);
    }

    public void writeInternal(final DataOutput out) throws IOException {
        masterAddress.writeData(out);
    }

    @Override
    public String toString() {
        return "Master " + masterAddress;
    }
}