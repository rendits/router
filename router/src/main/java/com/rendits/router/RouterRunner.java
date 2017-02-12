/* Copyright 2016 Albin Severinson
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

package com.rendits.router;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for running the router and listening to configuration changes over the
 * network. Whenever a new set of properties is received, the router is restarted with the new
 * properties.
 */
public class RouterRunner implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(RouterRunner.class);
  Properties props;
  ServerSocket configSocket;

  /**
   * RouterRunner constructor.
   *
   * @param props Default properties.
   * @exception IOException Thrown on problem setting up sockets or on missing required parameters.
   */
  RouterRunner(Properties props) throws IOException {
    this.props = props;

    /* Setup the config socket used to push config changes
     * over the network
     */
    int configPort = Integer.parseInt(props.getProperty("configPort"));
    configSocket = new ServerSocket(configPort);
  }

  /**
   * Start the router abd wait for a config change to arrive. When one does the router is restarted
   * with the new properties.
   */
  @Override
  public void run() {
    while (true) {
      Router router = null;

      /* Start the router */
      while (router == null) {
        try {
          router = new Router(this.props);
        } catch (IOException e) {
          logger.error("IOException occurred when starting the router:", e);

          /* Sleep for a while before trying again */
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ie) {
            logger.warn("RouterRunner interrupted when trying to start router.");
          }
        }
      }

      /* Wait for a config change to arrive */
      while (true) {
        try {
          Socket clientSocket = configSocket.accept();
          BufferedReader in =
              new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

          props.load(in);
          logger.info("Loaded props.." + props);

          in.close();
          clientSocket.close();

          break;

        } catch (IOException e) {

          /* Sleep for a while before trying again */
          try {
            Thread.sleep(2000);
          } catch (InterruptedException ie) {
            logger.warn("RouterRunner interrupted when waiting for config changes.");
          }
        }
      }

      /* Close the router. It will be restarted with the
       * updated properties in the next iteration. */
      router.close();
    }
  }

  /** The main method will start the router with the provided properties. */
  public static void main(String[] args) throws IOException {
    /* TODO: Allow loading custom config via cli */

    /* Load properties from file */
    Properties props = new Properties();
    FileInputStream in = new FileInputStream("router.properties");
    props.load(in);
    in.close();

    /* Time to get the ball rolling! */
    RouterRunner routerRunner = new RouterRunner(props);
    Thread runnerThread = new Thread(routerRunner);
    runnerThread.start();
  }
}
