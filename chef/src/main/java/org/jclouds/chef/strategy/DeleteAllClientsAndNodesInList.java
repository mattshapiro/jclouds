/**
 *
 * Copyright (C) 2010 Cloud Conscious, LLC. <info@cloudconscious.com>
 *
 * ====================================================================
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
 * ====================================================================
 */
package org.jclouds.chef.strategy;

import static com.google.common.collect.Maps.newHashMap;
import static org.jclouds.concurrent.ConcurrentUtils.awaitCompletion;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.Constants;
import org.jclouds.chef.ChefAsyncClient;
import org.jclouds.chef.ChefClient;
import org.jclouds.logging.Logger;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;

/**
 * 
 * 
 * @author Adrian Cole
 */
@Singleton
public class DeleteAllClientsAndNodesInList {

   protected final ChefClient chefClient;
   protected final ChefAsyncClient chefAsyncClient;
   protected final ExecutorService userExecutor;
   @Resource
   protected Logger logger = Logger.NULL;

   @Inject(optional = true)
   @Named(Constants.PROPERTY_REQUEST_TIMEOUT)
   protected Long maxTime;

   @Inject
   DeleteAllClientsAndNodesInList(@Named(Constants.PROPERTY_USER_THREADS) ExecutorService userExecutor,
         ChefClient getAllNode, ChefAsyncClient ablobstore) {
      this.userExecutor = userExecutor;
      this.chefAsyncClient = ablobstore;
      this.chefClient = getAllNode;
   }

   public void execute() {
      execute(chefClient.listNodes());
   }

   public void execute(Iterable<String> names) {
      Map<String, Exception> exceptions = newHashMap();
      Map<String, ListenableFuture<?>> responses = newHashMap();
      for (String name : names) {
         responses.put(name, chefAsyncClient.deleteClient(name));
         responses.put(name, chefAsyncClient.deleteNode(name));
      }
      exceptions = awaitCompletion(responses, userExecutor, maxTime, logger, String.format(
            "getting deleting clients and nodes: %s", names));
      if (exceptions.size() > 0)
         throw new RuntimeException(String.format("errors deleting clients and nodes: %s: %s", names, exceptions));
   }
}