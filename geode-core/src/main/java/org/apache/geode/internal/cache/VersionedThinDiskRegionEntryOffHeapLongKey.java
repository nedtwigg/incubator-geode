/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache;

// DO NOT modify this class. It was generated from LeafRegionEntry.cpp
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.internal.cache.lru.EnableLRU;
import org.apache.geode.internal.cache.persistence.DiskRecoveryStore;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.cache.versions.VersionSource;
import org.apache.geode.internal.cache.versions.VersionStamp;
import org.apache.geode.internal.cache.versions.VersionTag;
import org.apache.geode.internal.offheap.OffHeapRegionEntryHelper;
import org.apache.geode.internal.offheap.annotations.Released;
import org.apache.geode.internal.offheap.annotations.Retained;
import org.apache.geode.internal.offheap.annotations.Unretained;
import org.apache.geode.internal.util.concurrent.CustomEntryConcurrentHashMap.HashEntry;

// macros whose definition changes this class:
// disk: DISK
// lru: LRU
// stats: STATS
// versioned: VERSIONED
// offheap: OFFHEAP
// One of the following key macros must be defined:
// key object: KEY_OBJECT
// key int: KEY_INT
// key long: KEY_LONG
// key uuid: KEY_UUID
// key string1: KEY_STRING1
// key string2: KEY_STRING2
/**
 * Do not modify this class. It was generated. Instead modify LeafRegionEntry.cpp and then run
 * bin/generateRegionEntryClasses.sh from the directory that contains your build.xml.
 */
public class VersionedThinDiskRegionEntryOffHeapLongKey
    extends VersionedThinDiskRegionEntryOffHeap {
  public VersionedThinDiskRegionEntryOffHeapLongKey(RegionEntryContext context, long key,
      @Retained Object value) {
    super(context, (value instanceof RecoveredEntry ? null : value));
    // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
    initialize(context, value);
    this.key = key;
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // common code
  protected int hash;
  private HashEntry<Object, Object> next;
  @SuppressWarnings("unused")
  private volatile long lastModified;
  private static final AtomicLongFieldUpdater<VersionedThinDiskRegionEntryOffHeapLongKey> lastModifiedUpdater =
      AtomicLongFieldUpdater.newUpdater(VersionedThinDiskRegionEntryOffHeapLongKey.class,
          "lastModified");
  /**
   * All access done using ohAddrUpdater so it is used even though the compiler can not tell it is.
   */
  @SuppressWarnings("unused")
  @Retained
  @Released
  private volatile long ohAddress;
  /**
   * I needed to add this because I wanted clear to call setValue which normally can only be called
   * while the re is synced. But if I sync in that code it causes a lock ordering deadlock with the
   * disk regions because they also get a rw lock in clear. Some hardware platforms do not support
   * CAS on a long. If gemfire is run on one of those the AtomicLongFieldUpdater does a sync on the
   * re and we will once again be deadlocked. I don't know if we support any of the hardware
   * platforms that do not have a 64bit CAS. If we do then we can expect deadlocks on disk regions.
   */
  private final static AtomicLongFieldUpdater<VersionedThinDiskRegionEntryOffHeapLongKey> ohAddrUpdater =
      AtomicLongFieldUpdater.newUpdater(VersionedThinDiskRegionEntryOffHeapLongKey.class,
          "ohAddress");

  @Override
  public Token getValueAsToken() {
    return OffHeapRegionEntryHelper.getValueAsToken(this);
  }

  @Override
  protected Object getValueField() {
    return OffHeapRegionEntryHelper._getValue(this);
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  @Override
  @Unretained
  protected void setValueField(@Unretained Object v) {
    OffHeapRegionEntryHelper.setValue(this, v);
  }

  @Override
  @Retained
  public Object _getValueRetain(RegionEntryContext context, boolean decompress) {
    return OffHeapRegionEntryHelper._getValueRetain(this, decompress, context);
  }

  @Override
  public long getAddress() {
    return ohAddrUpdater.get(this);
  }

  @Override
  public boolean setAddress(long expectedAddr, long newAddr) {
    return ohAddrUpdater.compareAndSet(this, expectedAddr, newAddr);
  }

  @Override
  @Released
  public void release() {
    OffHeapRegionEntryHelper.releaseEntry(this);
  }

  @Override
  public void returnToPool() {
    // Deadcoded for now; never was working
    // if (this instanceof VMThinRegionEntryLongKey) {
    // factory.returnToPool((VMThinRegionEntryLongKey)this);
    // }
  }

  protected long getlastModifiedField() {
    return lastModifiedUpdater.get(this);
  }

  protected boolean compareAndSetLastModifiedField(long expectedValue, long newValue) {
    return lastModifiedUpdater.compareAndSet(this, expectedValue, newValue);
  }

  /**
   * @see HashEntry#getEntryHash()
   */
  public final int getEntryHash() {
    return this.hash;
  }

  protected void setEntryHash(int v) {
    this.hash = v;
  }

  /**
   * @see HashEntry#getNextEntry()
   */
  public final HashEntry<Object, Object> getNextEntry() {
    return this.next;
  }

  /**
   * @see HashEntry#setNextEntry
   */
  public final void setNextEntry(final HashEntry<Object, Object> n) {
    this.next = n;
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // disk code
  protected void initialize(RegionEntryContext context, Object value) {
    diskInitialize(context, value);
  }

  @Override
  public int updateAsyncEntrySize(EnableLRU capacityController) {
    throw new IllegalStateException("should never be called");
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  private void diskInitialize(RegionEntryContext context, Object value) {
    DiskRecoveryStore drs = (DiskRecoveryStore) context;
    DiskStoreImpl ds = drs.getDiskStore();
    long maxOplogSize = ds.getMaxOplogSize();
    // get appropriate instance of DiskId implementation based on maxOplogSize
    this.id = DiskId.createDiskId(maxOplogSize, true/* is persistence */, ds.needsLinkedList());
    Helper.initialize(this, drs, value);
  }

  /**
   * DiskId
   * 
   * @since GemFire 5.1
   */
  protected DiskId id;// = new DiskId();

  public DiskId getDiskId() {
    return this.id;
  }

  @Override
  void setDiskId(RegionEntry old) {
    this.id = ((AbstractDiskRegionEntry) old).getDiskId();
  }

  // // inlining DiskId
  // // always have these fields
  // /**
  // * id consists of
  // * most significant
  // * 1 byte = users bits
  // * 2-8 bytes = oplog id
  // * least significant.
  // *
  // * The highest bit in the oplog id part is set to 1 if the oplog id
  // * is negative.
  // * @todo this field could be an int for an overflow only region
  // */
  // private long id;
  // /**
  // * Length of the bytes on disk.
  // * This is always set. If the value is invalid then it will be set to 0.
  // * The most significant bit is used by overflow to mark it as needing to be written.
  // */
  // protected int valueLength = 0;
  // // have intOffset or longOffset
  // // intOffset
  // /**
  // * The position in the oplog (the oplog offset) where this entry's value is
  // * stored
  // */
  // private volatile int offsetInOplog;
  // // longOffset
  // /**
  // * The position in the oplog (the oplog offset) where this entry's value is
  // * stored
  // */
  // private volatile long offsetInOplog;
  // // have overflowOnly or persistence
  // // overflowOnly
  // // no fields
  // // persistent
  // /** unique entry identifier * */
  // private long keyId;
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // versioned code
  private VersionSource memberID;
  private short entryVersionLowBytes;
  private short regionVersionHighBytes;
  private int regionVersionLowBytes;
  private byte entryVersionHighByte;
  private byte distributedSystemId;

  public int getEntryVersion() {
    return ((entryVersionHighByte << 16) & 0xFF0000) | (entryVersionLowBytes & 0xFFFF);
  }

  public long getRegionVersion() {
    return (((long) regionVersionHighBytes) << 32) | (regionVersionLowBytes & 0x00000000FFFFFFFFL);
  }

  public long getVersionTimeStamp() {
    return getLastModified();
  }

  public void setVersionTimeStamp(long time) {
    setLastModified(time);
  }

  public VersionSource getMemberID() {
    return this.memberID;
  }

  public int getDistributedSystemId() {
    return this.distributedSystemId;
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  public void setVersions(VersionTag tag) {
    this.memberID = tag.getMemberID();
    int eVersion = tag.getEntryVersion();
    this.entryVersionLowBytes = (short) (eVersion & 0xffff);
    this.entryVersionHighByte = (byte) ((eVersion & 0xff0000) >> 16);
    this.regionVersionHighBytes = tag.getRegionVersionHighBytes();
    this.regionVersionLowBytes = tag.getRegionVersionLowBytes();
    if (!(tag.isGatewayTag()) && this.distributedSystemId == tag.getDistributedSystemId()) {
      if (getVersionTimeStamp() <= tag.getVersionTimeStamp()) {
        setVersionTimeStamp(tag.getVersionTimeStamp());
      } else {
        tag.setVersionTimeStamp(getVersionTimeStamp());
      }
    } else {
      setVersionTimeStamp(tag.getVersionTimeStamp());
    }
    this.distributedSystemId = (byte) (tag.getDistributedSystemId() & 0xff);
  }

  public void setMemberID(VersionSource memberID) {
    this.memberID = memberID;
  }

  @Override
  public VersionStamp getVersionStamp() {
    return this;
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  public VersionTag asVersionTag() {
    VersionTag tag = VersionTag.create(memberID);
    tag.setEntryVersion(getEntryVersion());
    tag.setRegionVersion(this.regionVersionHighBytes, this.regionVersionLowBytes);
    tag.setVersionTimeStamp(getVersionTimeStamp());
    tag.setDistributedSystemId(this.distributedSystemId);
    return tag;
  }

  public void processVersionTag(LocalRegion r, VersionTag tag, boolean isTombstoneFromGII,
      boolean hasDelta, VersionSource thisVM, InternalDistributedMember sender,
      boolean checkForConflicts) {
    basicProcessVersionTag(r, tag, isTombstoneFromGII, hasDelta, thisVM, sender, checkForConflicts);
  }

  @Override
  public void processVersionTag(EntryEvent cacheEvent) {
    // this keeps Eclipse happy. without it the sender chain becomes confused
    // while browsing this code
    super.processVersionTag(cacheEvent);
  }

  /** get rvv internal high byte. Used by region entries for transferring to storage */
  public short getRegionVersionHighBytes() {
    return this.regionVersionHighBytes;
  }

  /** get rvv internal low bytes. Used by region entries for transferring to storage */
  public int getRegionVersionLowBytes() {
    return this.regionVersionLowBytes;
  }

  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
  // key code
  private final long key;

  @Override
  public final Object getKey() {
    return this.key;
  }

  @Override
  public boolean isKeyEqual(Object k) {
    if (k instanceof Long) {
      return ((Long) k).longValue() == this.key;
    }
    return false;
  }
  // DO NOT modify this class. It was generated from LeafRegionEntry.cpp
}
