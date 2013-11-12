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

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.TransientApiMetadata;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.demo.tweetstore.domain.StoredTweetStatus;
import org.jclouds.demo.tweetstore.reference.TweetStoreConstants;
import org.testng.annotations.Test;

/**
 * Tests behavior of {@code MetadataToStoredTweetStatus}
 *
 * @author Adrian Cole
 */
@Test(groups = "unit")
public class MetadataToStoredTweetStatusTest {

   BlobStore createStoreAndContainer(String container) throws InterruptedException, ExecutionException {
      BlobStoreContext context =
          ContextBuilder.newBuilder(TransientApiMetadata.builder().build()).build(BlobStoreContext.class);
      BlobStore store = context.getBlobStore();
      store.createContainerInLocation(null, container);
      return store;
   }

   public void testStoreTweets() throws IOException, InterruptedException, ExecutionException {
      String container = "test1";
      BlobStore store = createStoreAndContainer(container);
      Blob blob = store.blobBuilder("1").build();
      blob.getMetadata().getUserMetadata().put(TweetStoreConstants.SENDER_NAME, "frank");
      blob.setPayload("I love beans!");
      store.putBlob(container, blob);
      String host = "localhost";
      String service = "stub";

      MetadataToStoredTweetStatus function = new MetadataToStoredTweetStatus(store, service, host, container);
      StoredTweetStatus result = function.apply(blob.getMetadata());

      StoredTweetStatus expected = new StoredTweetStatus(service, host, container, "1", "frank",
               "I love beans!", null);

      assertEquals(result, expected);

   }
}
