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
package org.apache.geode.internal.cache.persistence;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import org.apache.geode.internal.FileUtil;
import org.apache.geode.test.junit.categories.IntegrationTest;

/**
 * TODO: fails when running integrationTest from gradle command-line on Windows 7
 *
 * <p>Tests for the BackupInspector.
 */
@Category(IntegrationTest.class)
public class BackupInspectorJUnitTest {

  private static final String UNIX_INCREMENTAL_BACKUP_SCRIPT =
      "#!/bin/bash -e\ncd `dirname $0`\n\n#Restore a backup of gemfire persistent data to the location it was backed up\n#from.\n#This script will refuse to restore if the original data still exists.\n\n#This script was automatically generated by the gemfire backup utility.\n\n#Test for existing originals. If they exist, do not restore the backup.\ntest -e '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar.if' && echo 'Backup not restored. Refusing to overwrite /Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar.if' && exit 1 \ntest -e '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo.if' && echo 'Backup not restored. Refusing to overwrite /Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo.if' && exit 1 \n\n#Restore data\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3'\ncp -rp 'diskstores/bar/dir0'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk4'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1'\ncp -rp 'diskstores/foo/dir0'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk2'\n\n#Incremental backup.  Restore baseline originals from a previous backup.\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/bar/dir0/BACKUPbar_1.drf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar_1.drf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir1/BACKUPfoo_2.crf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk2/BACKUPfoo_2.crf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir1/BACKUPfoo_2.drf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk2/BACKUPfoo_2.drf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/bar/dir1/BACKUPbar_2.drf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk4/BACKUPbar_2.drf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir0/BACKUPfoo_1.crf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo_1.crf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/bar/dir1/BACKUPbar_2.crf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk4/BACKUPbar_2.crf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/bar/dir0/BACKUPbar_1.crf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar_1.crf'\ncp -p '/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir0/BACKUPfoo_1.drf' '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo_1.drf'";
  private static final String UNIX_FULL_BACKUP_SCRIPT =
      "#!/bin/bash -e\ncd `dirname $0`\n\n#Restore a backup of gemfire persistent data to the location it was backed up\n#from.\n#This script will refuse to restore if the original data still exists.\n\n#This script was automatically generated by the gemfire backup utility.\n\n#Test for existing originals. If they exist, do not restore the backup.\ntest -e '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar.if' && echo 'Backup not restored. Refusing to overwrite /Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar.if' && exit 1 \ntest -e '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo.if' && echo 'Backup not restored. Refusing to overwrite /Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo.if' && exit 1 \n\n#Restore data\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3'\ncp -rp 'diskstores/bar/dir0'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk3'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk4'\ncp -rp 'diskstores/bar/dir1'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk4'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1'\ncp -rp 'diskstores/foo/dir0'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk1'\nmkdir -p '/Users/rholmes/Projects/gemfire/test/cacheserver/disk2'\ncp -rp 'diskstores/foo/dir1'/* '/Users/rholmes/Projects/gemfire/test/cacheserver/disk2'";

  private static final String WINDOWS_INCREMENTAL_BACKUP_SCRIPT =
      "rem echo off\n\nrem Restore a backup of gemfire persistent data to the location it was backed up\nrem from.\nrem This script will refuse to restore if the original data still exists.\n\nrem This script was automatically generated by the gemfire backup utility.\n\nrem Test for existing originals. If they exist, do not restore the backup.\nIF EXIST \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar.if\" echo \"Backup not restored. Refusing to overwrite \\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar.if\" && exit /B 1 \nIF EXIST \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo.if\" echo \"Backup not restored. Refusing to overwrite \\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo.if\" && exit /B 1 \n\nrem Restore data\nxcopy \"diskstores\\bar\\dir0\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\" /I /E\nxcopy \"diskstores\\foo\\dir0\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\" /I /E\n\nrem Incremental backup.  Restore baseline originals from a previous backup.\ncopy \\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\bar\\dir0\\BACKUPbar_1.drf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar_1.drf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir1\\BACKUPfoo_2.crf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk2\\BACKUPfoo_2.crf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir1\\BACKUPfoo_2.drf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk2\\BACKUPfoo_2.drf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\bar\\dir1\\BACKUPbar_2.drf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk4\\BACKUPbar_2.drf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir0\\BACKUPfoo_1.crf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo_1.crf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\bar\\dir1\\BACKUPbar_2.crf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk4\\BACKUPbar_2.crf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\bar\\dir0\\BACKUPbar_1.crf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar_1.crf\"\ncopy \"\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir0\\BACKUPfoo_1.drf\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo_1.drf\"";
  private static final String WINDOWS_FULL_BACKUP_SCRIPT =
      "rem echo off\n\nrem Restore a backup of gemfire persistent data to the location it was backed up\nrem from.\nrem This script will refuse to restore if the original data still exists.\n\nrem This script was automatically generated by the gemfire backup utility.\n\nrem Test for existing originals. If they exist, do not restore the backup.\nIF EXIST \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar.if\" echo \"Backup not restored. Refusing to overwrite \\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar.if\" && exit /B 1 \nIF EXIST \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo.if\" echo \"Backup not restored. Refusing to overwrite \\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo.if\" && exit /B 1 \n\nrem Restore data\nxcopy \"diskstores\\bar\\dir0\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\"\nxcopy \"diskstores\\bar\\dir1\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk4\"\nxcopy \"diskstores\\foo\\dir0\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\"\nxcopy \"diskstores\\foo\\dir1\" \"\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk2\"";

  private static final String OPLOG_FILENAME_1 = "BACKUPbar_1.drf";
  private static final String OPLOG_FILENAME_2 = "BACKUPfoo_1.crf";
  private static final String OPLOG_FILENAME_3 = "BACKUPfoo_1.drf";

  private static final String UNIX_COPY_FROM_1 =
      "/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/bar/dir0/BACKUPbar_1.drf";
  private static final String UNIX_COPY_FROM_2 =
      "/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir0/BACKUPfoo_1.crf";
  private static final String UNIX_COPY_FROM_3 =
      "/Users/rholmes/Projects/gemfire/test/backup/2012-05-24-09-42/rholmes_mbp_410_v1_56425/diskstores/foo/dir0/BACKUPfoo_1.drf";

  private static final String WINDOWS_COPY_FROM_1 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\bar\\dir0\\BACKUPbar_1.drf";
  private static final String WINDOWS_COPY_FROM_2 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir0\\BACKUPfoo_1.crf";
  private static final String WINDOWS_COPY_FROM_3 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\backup\\2012-05-24-09-42\\rholmes_mbp_410_v1_56425\\diskstores\\foo\\dir0\\BACKUPfoo_1.drf";

  private static final String UNIX_COPY_TO_1 =
      "/Users/rholmes/Projects/gemfire/test/cacheserver/disk3/BACKUPbar_1.drf";
  private static final String UNIX_COPY_TO_2 =
      "/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo_1.crf";
  private static final String UNIX_COPY_TO_3 =
      "/Users/rholmes/Projects/gemfire/test/cacheserver/disk1/BACKUPfoo_1.drf";

  private static final String WINDOWS_COPY_TO_1 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk3\\BACKUPbar_1.drf";
  private static final String WINDOWS_COPY_TO_2 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo_1.crf";
  private static final String WINDOWS_COPY_TO_3 =
      "\\Users\\rholmes\\Projects\\gemfire\\test\\cacheserver\\disk1\\BACKUPfoo_1.drf";

  /** Temporary incremental backup directory. */
  private File incrementalBackupDir = null;

  /** Temporary full backup directory. */
  private File fullBackupDir = null;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  /** Set up data for all tests. */
  @Before
  public void setUp() throws Exception {
    File tempDir = temporaryFolder.newFolder();

    /*
     * Create an incremental backup on the file system.
     */
    this.incrementalBackupDir = new File(tempDir, "incremental");
    assertTrue(this.incrementalBackupDir.mkdir());

    File incrementalRestoreFile = null;
    if (BackupInspector.isWindows()) {
      incrementalRestoreFile =
          new File(this.incrementalBackupDir, WindowsBackupInspector.RESTORE_FILE);
      PrintWriter writer = new PrintWriter(incrementalRestoreFile);
      writer.write(WINDOWS_INCREMENTAL_BACKUP_SCRIPT);
      writer.close();
    } else {
      incrementalRestoreFile =
          new File(this.incrementalBackupDir, UnixBackupInspector.RESTORE_FILE);
      PrintWriter writer = new PrintWriter(incrementalRestoreFile);
      writer.write(UNIX_INCREMENTAL_BACKUP_SCRIPT);
      writer.close();
    }

    /*
     * Create a full backup on the file system.
     */
    this.fullBackupDir = new File(tempDir, "backup");
    assertTrue(this.fullBackupDir.mkdir());

    File fullRestoreFile = null;
    if (BackupInspector.isWindows()) {
      fullRestoreFile = new File(this.fullBackupDir, WindowsBackupInspector.RESTORE_FILE);
      PrintWriter writer = new PrintWriter(fullRestoreFile);
      writer.write(WINDOWS_FULL_BACKUP_SCRIPT);
      writer.close();
    } else {
      fullRestoreFile = new File(this.fullBackupDir, UnixBackupInspector.RESTORE_FILE);
      PrintWriter writer = new PrintWriter(fullRestoreFile);
      writer.write(UNIX_FULL_BACKUP_SCRIPT);
      writer.close();
    }
  }

  /** Tests that an IOException is thrown for a non-existent restore script. */
  @Test
  public void testNonExistentScriptFile() throws Exception {
    boolean ioexceptionThrown = false;

    try {
      @SuppressWarnings("unused")
      BackupInspector inspector =
          BackupInspector.createInspector(new File(System.getProperty("java.io.tmpdir")));
    } catch (IOException e) {
      ioexceptionThrown = true;
    }

    assertTrue(ioexceptionThrown);
  }

  /**
   * Tests copy lines for windows.
   *
   * @param inspector a BackupInspector.
   */
  private void testIncrementalBackupScriptForWindows(BackupInspector inspector) throws Exception {
    assertEquals(WINDOWS_COPY_FROM_1, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_1));
    assertEquals(WINDOWS_COPY_TO_1, inspector.getCopyToForOplogFile(OPLOG_FILENAME_1));
    assertEquals(WINDOWS_COPY_FROM_2, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_2));
    assertEquals(WINDOWS_COPY_TO_2, inspector.getCopyToForOplogFile(OPLOG_FILENAME_2));
    assertEquals(WINDOWS_COPY_FROM_3, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_3));
    assertEquals(WINDOWS_COPY_TO_3, inspector.getCopyToForOplogFile(OPLOG_FILENAME_3));
  }

  /**
   * Tests copy lines for unix.
   *
   * @param inspector a BackupInspector.
   */
  private void testIncrementalBackupScriptForUnix(BackupInspector inspector) throws Exception {
    assertEquals(UNIX_COPY_FROM_1, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_1));
    assertEquals(UNIX_COPY_TO_1, inspector.getCopyToForOplogFile(OPLOG_FILENAME_1));
    assertEquals(UNIX_COPY_FROM_2, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_2));
    assertEquals(UNIX_COPY_TO_2, inspector.getCopyToForOplogFile(OPLOG_FILENAME_2));
    assertEquals(UNIX_COPY_FROM_3, inspector.getCopyFromForOplogFile(OPLOG_FILENAME_3));
    assertEquals(UNIX_COPY_TO_3, inspector.getCopyToForOplogFile(OPLOG_FILENAME_3));
  }

  /** Tests that the parser succeeds for an incremental backup restore script. */
  @Test
  public void testIncrementalBackupScript() throws Exception {
    BackupInspector inspector = BackupInspector.createInspector(incrementalBackupDir);

    assertTrue(inspector.isIncremental());

    Set<String> oplogFiles = inspector.getIncrementalOplogFileNames();

    assertFalse(oplogFiles.isEmpty());
    assertEquals(8, oplogFiles.size());
    assertTrue(oplogFiles.contains(OPLOG_FILENAME_1));
    assertTrue(oplogFiles.contains(OPLOG_FILENAME_2));
    assertTrue(oplogFiles.contains(OPLOG_FILENAME_3));

    if (BackupInspector.isWindows()) {
      testIncrementalBackupScriptForWindows(inspector);
    } else {
      testIncrementalBackupScriptForUnix(inspector);
    }
  }

  /** Tests that the parser works with a full backup restore script. */
  @Test
  public void testFullBackupScript() throws Exception {
    BackupInspector inspector = BackupInspector.createInspector(fullBackupDir);
    assertFalse(inspector.isIncremental());
    assertTrue(inspector.getIncrementalOplogFileNames().isEmpty());
    assertNull(inspector.getScriptLineForOplogFile(OPLOG_FILENAME_1));
  }
}
