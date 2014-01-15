/**
 * Licensed to jclouds, Inc. (jclouds) under one or more
 * contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  jclouds licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jclouds.demo.tweetstore.functions;

import javax.annotation.Resource;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.demo.tweetstore.domain.StoredTweetStatus;
import org.jclouds.demo.tweetstore.reference.TweetStoreConstants;
import org.jclouds.logging.Logger;
import org.jclouds.util.Strings2;

import com.google.common.base.Function;

/**
 * 
 * @author Adrian Cole
 */
public class MetadataToStoredTweetStatus implements Function<StorageMetadata, StoredTweetStatus> {
   private final String host;
   private final BlobStore store;
   private final String service;
   private final String container;

   @Resource
   protected Logger logger = Logger.NULL;

   MetadataToStoredTweetStatus(BlobStore store, String service, String host, String container) {
      this.host = host;
      this.store = store;
      this.service = service;
      this.container = container;
   }

   public StoredTweetStatus apply(StorageMetadata blobMetadata) {
      String id = blobMetadata.getName();
      String status;
      String from;
      String tweet;
      try {
         long start = System.currentTimeMillis();
         Blob blob = store.getBlob(container, id);
         status = ((System.currentTimeMillis() - start) + "ms");
         from = blob.getMetadata().getUserMetadata().get(TweetStoreConstants.SENDER_NAME);
         tweet = Strings2.toString(blob.getPayload());
      } catch (Exception e) {
         logger.error(e, "Error listing container %s//%s/%s", service, container, id);
         status = (e.getMessage());
         tweet = "";
         from = "";
      }
      return new StoredTweetStatus(service, host, container, id, from, tweet, status);
   }
}
