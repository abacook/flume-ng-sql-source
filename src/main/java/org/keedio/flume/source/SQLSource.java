/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.keedio.flume.source;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.apache.flume.source.AbstractSource;
import org.keedio.flume.metrics.SqlSourceCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*Support UTF-8 character encoding.*/
import com.google.common.base.Charsets;
import java.nio.charset.Charset;


/**
 * A Source to read data from a SQL database. This source ask for new data in a table each configured time.<p>
 * 
 * @author <a href="mailto:mvalle@keedio.com">Marcelo Valle</a>
 */
public class SQLSource extends AbstractSource implements Configurable, PollableSource {
    @Override
    public long getBackOffSleepIncrement() {
        return 0;
    }

    @Override
    public long getMaxBackOffSleepInterval() {
        return 0;
    }
    private Context context;
    private static final Logger LOG = LoggerFactory.getLogger(SQLSourceOriginal.class);
    protected SQLSourceHelper sqlSourceHelper;
    private SqlSourceCounter sqlSourceCounter;
    private HibernateHelper hibernateHelper;
	
    /**
     * Configure the source, load configuration properties and establish connection with database
     */
    @Override
    public void configure(Context context) {
    	
    	LOG.getName();
        	
    	LOG.info("Reading and processing configuration values for source " + getName());
		
    	/* Initialize configuration parameters */
    	sqlSourceHelper = new SQLSourceHelper(context, this.getName());
        
    	/* Initialize metric counters */
		sqlSourceCounter = new SqlSourceCounter("SOURCESQL." + this.getName());
        
        /* Establish connection with database */
        hibernateHelper = new HibernateHelper(sqlSourceHelper);
        hibernateHelper.establishSession();
       
    }  
    
    /**
     * Process a batch of events performing SQL Queries
     */
	@Override
	public Status process() throws EventDeliveryException {
		try {
			List<Event> events = new ArrayList<>();
			
			sqlSourceCounter.startProcess();			
			
			List<List<Object>> result = hibernateHelper.executeQuery();
						
			if (!result.isEmpty())
			{       
				List<String> allRows = sqlSourceHelper.getAllRows(result);
						
				
				Event event = null;
                for (String row : allRows) {
                    event = new SimpleEvent();
                    event.setBody(row.getBytes());
                    events.add(event);
                }
                
                getChannelProcessor().processEventBatch(events);
                events.clear();		
                
				sqlSourceCounter.incrementEventCount(result.size());
				sqlSourceHelper.updateStatusFile();
			}
			
			sqlSourceCounter.endProcess(result.size());
			
			if (result.size() < sqlSourceHelper.getMaxRows()){
				Thread.sleep(sqlSourceHelper.getRunQueryDelay());
			}
						
			return Status.READY;
			
		} catch (IOException | InterruptedException e) {
			LOG.error("Error procesing row", e);
			return Status.BACKOFF;
		}
	}
 
	/**
	 * Starts the source. Starts the metrics counter.
	 */
	@Override
    public void start() {
        
    	LOG.info("Starting sql source {} ...", getName());
        sqlSourceCounter.start();
        super.start();
    }

	/**
	 * Stop the source. Close database connection and stop metrics counter.
	 */
    @Override
    public void stop() {
        
        LOG.info("Stopping sql source {} ...", getName());
        
        try 
        {
            hibernateHelper.closeSession();  
        } catch (IOException e) {
        	LOG.warn("Error ", e);
        } finally {
        	this.sqlSourceCounter.stop();
        	super.stop();
        }
    }
    
   
}
