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
package com.gigaspaces.internal.server.space.tiered_storage;

import com.gigaspaces.start.SystemLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

public class TieredStorageMachineCleaner {
        private static Logger logger = LoggerFactory.getLogger(TieredStorageMachineCleaner.class);


    public static void deleteTieredStorageData(String spaceName) {
        if (logger.isDebugEnabled()){
            logger.debug("Trying to delete db of space {}", spaceName);
        }
        Path path = SystemLocations.singleton().work("tiered-storage/" + spaceName);
        File folder = path.toFile();
        File[] files = folder.listFiles();
        if (files == null) {
            if (logger.isDebugEnabled()){
                logger.debug("Did not find db of space {} ", spaceName);
            }
        } else {
            for (final File file : files) {
                if (!file.delete()) {
                    logger.warn("Can't remove " + file.getAbsolutePath());
                }
            }
            folder.delete();
            logger.info("Successfully deleted db of space {} in path {}", spaceName, folder.getAbsolutePath());
        }
    }
}