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
package org.apache.geode.internal.cache.partitioned;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import org.apache.geode.DataSerializer;
import org.apache.geode.cache.CacheException;
import org.apache.geode.cache.EntryExistsException;
import org.apache.geode.cache.EntryNotFoundException;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.DM;
import org.apache.geode.distributed.internal.DirectReplyProcessor;
import org.apache.geode.distributed.internal.DistributionManager;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.distributed.internal.ReplyException;
import org.apache.geode.distributed.internal.ReplyMessage;
import org.apache.geode.distributed.internal.ReplyProcessor21;
import org.apache.geode.distributed.internal.ReplySender;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.Assert;
import org.apache.geode.internal.NanoTimer;
import org.apache.geode.internal.cache.DataLocationException;
import org.apache.geode.internal.cache.EntryEventImpl;
import org.apache.geode.internal.cache.EnumListenerEvent;
import org.apache.geode.internal.cache.FilterRoutingInfo;
import org.apache.geode.internal.cache.ForceReattemptException;
import org.apache.geode.internal.cache.PartitionedRegion;
import org.apache.geode.internal.cache.PartitionedRegionDataStore;
import org.apache.geode.internal.cache.PartitionedRegionHelper;
import org.apache.geode.internal.cache.PrimaryBucketException;
import org.apache.geode.internal.cache.versions.VersionTag;
import org.apache.geode.internal.i18n.LocalizedStrings;
import org.apache.geode.internal.logging.LogService;
import org.apache.geode.internal.logging.log4j.LogMarker;
import org.apache.geode.internal.offheap.annotations.Released;

public final class InvalidateMessage extends DestroyMessage {
  private static final Logger logger = LogService.getLogger();

  /**
   * Empty constructor to satisfy {@link org.apache.geode.DataSerializer}
   * requirements
   */
  public InvalidateMessage() {
  }

  private InvalidateMessage(Set recipients,
                            boolean notifyOnly,
                            int regionId,
                            DirectReplyProcessor processor,
                            EntryEventImpl event) {
    super(recipients,
          notifyOnly,
          regionId,
          processor,
          event,
          null); // expectedOldValue
  }

  InvalidateMessage(InvalidateMessage original, EntryEventImpl event) {
    super(original);
    this.versionTag = event.getVersionTag();
  }
  
  /**
   * added for sending old value over the wire to the bridge servers with Cqs
   * @param original invalidateMessage originated at remote vm.
   * @param event EntryEventImpl generated by operation on the bucket region.
   * @param members list of members which needs old value.
   * @since GemFire 5.5
   */
  InvalidateMessage(InvalidateMessage original, EntryEventImpl event, Set members) {
    super(original, event, members);
  }


  @Override
  public PartitionMessage getMessageForRelayToListeners(EntryEventImpl event, Set members) {
    if (event.hasOldValue() && ( members != null && !members.isEmpty())){ 
      return new InvalidateMessage(this, event, members);
    }
    return new InvalidateMessage(this, event);
  }
  
  /**
   * send a notification-only message to a set of listeners.  The processor
   * id is passed with the message for reply message processing.  This method
   * does not wait on the processor.
   * 
   * @param cacheOpReceivers receivers of associated bucket CacheOperationMessage
   * @param adjunctRecipients receivers that must get the event
   * @param filterRoutingInfo client routing information
   * @param r the region affected by the event
   * @param event the event that prompted this action
   * @param processor the processor to reply to
   * @return members that could not be notified
   */
  public static Set notifyListeners(Set cacheOpReceivers, Set adjunctRecipients,
      FilterRoutingInfo filterRoutingInfo, 
      PartitionedRegion r, EntryEventImpl event, 
      DirectReplyProcessor processor) {
    InvalidateMessage msg = new InvalidateMessage(Collections.EMPTY_SET, 
        true, r.getPRId(), processor, event);
    msg.versionTag = event.getVersionTag();
    return msg.relayToListeners(cacheOpReceivers, adjunctRecipients,
        filterRoutingInfo, event, r, processor);
  }

  
  /**
   * Sends an InvalidateMessage
   * {@link org.apache.geode.cache.Region#invalidate(Object)}message to the
   * recipient
   * 
   * @param recipient the recipient of the message
   * @param r
   *          the PartitionedRegion for which the invalidate was performed
   * @param event the event causing this message
   * @return the InvalidateResponse processor used to await the potential
   *         {@link org.apache.geode.cache.CacheException}
   * @throws ForceReattemptException if the peer is no longer available
   */
  public static InvalidateResponse send(DistributedMember recipient,
      PartitionedRegion r, EntryEventImpl event)
      throws ForceReattemptException
  {
    //Assert.assertTrue(recipient != null, "InvalidateMessage NULL recipient");  recipient may be null for remote notifications
    Set recipients = Collections.singleton(recipient);
    InvalidateResponse p = new InvalidateResponse(r.getSystem(), recipients, event.getKey());
    InvalidateMessage m = new InvalidateMessage(recipients, false,
        r.getPRId(), p, event);
    Set failures =r.getDistributionManager().putOutgoing(m); 
    if (failures != null && failures.size() > 0 ) {
      throw new ForceReattemptException(LocalizedStrings.InvalidateMessage_FAILED_SENDING_0.toLocalizedString(m));
    }
    return p;
  }

  /**
   * This method is called upon receipt and make the desired changes to the
   * PartitionedRegion Note: It is very important that this message does NOT
   * cause any deadlocks as the sender will wait indefinitely for the
   * acknowledgement
   * 
   * @throws EntryExistsException
   * @throws DataLocationException 
   */
  @Override
  protected boolean operateOnPartitionedRegion(DistributionManager dm,
      PartitionedRegion r, long startTime)
      throws EntryExistsException, DataLocationException
  {
    InternalDistributedMember eventSender = originalSender;
    if (eventSender == null) {
       eventSender = getSender();
    }
    final Object key = getKey();
    @Released final EntryEventImpl event = EntryEventImpl.create(
        r,
        getOperation(),
        key,
        null, /*newValue*/
        getCallbackArg(),
        false/*originRemote - false to force distribution in buckets*/,
        eventSender,
        true/*generateCallbacks*/,
        false/*initializeId*/);
    try {
    if (this.versionTag != null) {
      this.versionTag.replaceNullIDs(getSender());
      event.setVersionTag(this.versionTag);
    }
    if (this.bridgeContext != null) {
      event.setContext(this.bridgeContext);
    }
//    Assert.assertTrue(eventId != null);  bug #47235: region invalidation doesn't send event ids
    event.setEventId(eventId);
    event.setPossibleDuplicate(this.posDup);
    
    PartitionedRegionDataStore ds = r.getDataStore();
    boolean sendReply = true;
//    boolean failed = false;
    event.setInvokePRCallbacks(!notificationOnly);
    if (!notificationOnly) {
      Assert.assertTrue(ds!=null, "This process should have storage for an item in " + this.toString());
      try {
        Integer bucket = Integer.valueOf(PartitionedRegionHelper.getHashKey(event));
        event.setCausedByMessage(this);
        r.getDataView().invalidateOnRemote(event, true/*invokeCallbacks*/, false/*forceNewEntry*/);
        this.versionTag = event.getVersionTag();
        if (logger.isTraceEnabled(LogMarker.DM)) {
          logger.trace(LogMarker.DM, "{} invalidateLocally in bucket: {}, key: {}", getClass().getName(), bucket, key);
        }
      }
      catch (DataLocationException e) {
        ((ForceReattemptException)e).checkKey(event.getKey());
        throw e;
      }
      catch (EntryNotFoundException eee) {
        //        failed = true;
        if (logger.isDebugEnabled()) {
          logger.debug("{}: operateOnRegion caught EntryNotFoundException {}", getClass().getName(), eee.getMessage(), eee);
        }
        sendReply(getSender(), getProcessorId(), dm, new ReplyException(eee), r, startTime);
        sendReply = false; // this prevents us from acking later
      }
      catch (PrimaryBucketException pbe) {
        sendReply(getSender(), getProcessorId(), dm, new ReplyException(pbe), r, startTime);
        return false;
      }

    }
    else {
      event.setRegion(r);
      event.setOriginRemote(true);
      if (this.versionTag != null) {
        this.versionTag.replaceNullIDs(getSender());
        event.setVersionTag(this.versionTag);
      }
      if (this.filterInfo != null) {
        event.setLocalFilterInfo(this.filterInfo.getFilterInfo(dm.getDistributionManagerId()));
      }
      r.invokeInvalidateCallbacks(EnumListenerEvent.AFTER_INVALIDATE, event, r.isInitialized());
    }
    
    return sendReply;

    } finally {
      event.release();
    }
  }


  // override reply processor type from PartitionMessage
  PartitionResponse createReplyProcessor(PartitionedRegion r, Set recipients, Object key) {
    return new InvalidateResponse(r.getSystem(), recipients, key);
  }
  
  // override reply message type from PartitionMessage
  @Override
  protected void sendReply(InternalDistributedMember member, int procId, DM dm, ReplyException ex, PartitionedRegion pr, long startTime) {
    if (pr != null && startTime > 0) {
      pr.getPrStats().endPartitionMessagesProcessing(startTime); 
    }
    InvalidateReplyMessage.send(member, procId, getReplySender(dm), ex, this.versionTag);
  }

  @Override
  public int getDSFID() {
    return PR_INVALIDATE_MESSAGE;
  }
  
  public static final class InvalidateReplyMessage extends ReplyMessage {
    VersionTag versionTag;

    /**
     * Empty constructor to conform to DataSerializable interface 
     */
    public InvalidateReplyMessage() {
    }
  
    private InvalidateReplyMessage(int processorId, VersionTag version, ReplyException ex)
    {
      super();
      setProcessorId(processorId);
      this.versionTag = version;
      setException(ex);
    }
    
    /** Send an ack */
    public static void send(InternalDistributedMember recipient, int processorId,
        ReplySender replySender, ReplyException ex, VersionTag version) 
    {
      Assert.assertTrue(recipient != null, "InvalidateReplyMessage NULL reply message");
      InvalidateReplyMessage m = new InvalidateReplyMessage(processorId, version, ex);
      m.setRecipient(recipient);
      replySender.putOutgoing(m);
    }
      
    /**
     * Processes this message.  This method is invoked by the receiver
     * of the message.
     * @param dm the distribution manager that is processing the message.
     */
    @Override
    public void process(final DM dm, final ReplyProcessor21 rp) {
      final long startTime = getTimestamp();
      if (logger.isTraceEnabled(LogMarker.DM)) {
        logger.trace(LogMarker.DM, "InvalidateReplyMessage process invoking reply processor with processorId: {}", this.processorId);
      }
  
      if (rp == null) {
        if (logger.isTraceEnabled(LogMarker.DM)) {
          logger.trace(LogMarker.DM, "InvalidateReplyMessage processor not found");
        }
        return;
      }
      if (rp instanceof InvalidateResponse) {
        InvalidateResponse processor = (InvalidateResponse)rp;
        processor.setResponse(this);
      }
      rp.process(this);
  
      if (logger.isTraceEnabled(LogMarker.DM)) {
        logger.trace(LogMarker.DM, "{} processed {}", rp, this);
      }

      dm.getStats().incReplyMessageTime(NanoTimer.getTime()-startTime);
    }
    
    @Override
    public int getDSFID() {
      return PR_INVALIDATE_REPLY_MESSAGE;
    }
    
    @Override
    public void toData(DataOutput out) throws IOException {
      super.toData(out);
      DataSerializer.writeObject(this.versionTag, out);
    }
  
    @Override
    public void fromData(DataInput in)
      throws IOException, ClassNotFoundException {
      super.fromData(in);
      this.versionTag = (VersionTag)DataSerializer.readObject(in);
    }
  
    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("InvalidateReplyMessage ")
      .append("processorid=").append(this.processorId)
      .append(" exception=").append(getException())
      .append(" versionTag=").append(this.versionTag);
      return sb.toString();
    }

  }
  /**
   * A processor to capture the value returned by {@link InvalidateMessage}
   * @since GemFire 5.1
   */
  public static class InvalidateResponse extends PartitionResponse  {
    private volatile boolean returnValueReceived;
    final Object key;
    public VersionTag versionTag;
    
    public InvalidateResponse(InternalDistributedSystem ds, Set recipients, Object key) {
      super(ds, recipients, false);
      this.key = key;
    }

    public void setResponse(InvalidateReplyMessage msg) {
      this.returnValueReceived = true;
      this.versionTag = msg.versionTag;
      if (this.versionTag != null) {
        this.versionTag.replaceNullIDs(msg.getSender());
      }
    }

    /**
     * @throws ForceReattemptException if the peer is no longer available
     * @throws CacheException if the peer generates an error
     */
    public void waitForResult() throws CacheException, ForceReattemptException
    {
      try {
        waitForCacheException();
      }
      catch (ForceReattemptException e) {
        e.checkKey(key);
        throw e;
      }
      if (!this.returnValueReceived) {
        throw new ForceReattemptException(LocalizedStrings.InvalidateMessage_NO_RESPONSE_CODE_RECEIVED.toLocalizedString());
      }
      return;
    }
  }
  


}
