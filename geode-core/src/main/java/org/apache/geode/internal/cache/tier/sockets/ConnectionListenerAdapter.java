/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.internal.cache.tier.sockets;

/**
 * A ConnectionListener that does nothing.
 *
 * @since GemFire 5.7
 */
public class ConnectionListenerAdapter implements ConnectionListener {

  public void connectionClosed(boolean lastConnection, byte communicationMode) {}

  public void connectionOpened(boolean firstConnection, byte communicationMode) {}

  public void queueAdded(ClientProxyMembershipID id) {}

  public void queueRemoved() {}
}
