/*	This file is part of project "Coopto", a computer software plugin for 		*
 *  utilizing Docker in VMware vRealize Orchestrator.							*
 *																				*
 *	Copyright (C) 2014-2015  Robert Szymczak	(rszymczak@fum.de)				*
 *																				*
 *																				*
 *	This program is free software: you can redistribute it and/or modify		*
 *	it under the terms of the GNU Lesser General Public License as published 	*
 *	by the Free Software Foundation, either version 3 of the License, or		*
 *	(at your option) any later version.											*
 *																				*
 *	This program is distributed in the hope that it will be useful,				*
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of				*
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  						*
 *	See the GNU Lesser General Public License for more details.					*
 *																				*
 *	You should have received a copy of the GNU Lesser General Public License	*
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.		*/
package org.hexlogic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hexlogic.model.DockerContainer;
import org.hexlogic.model.DockerImage;
import org.hexlogic.model.DockerNode;
import org.hexlogic.model.DockerNodeService;
import org.springframework.beans.factory.annotation.Autowired;

import ch.dunes.vso.sdk.api.QueryResult;
import ch.dunes.vso.sdk.endpoints.IEndpointConfiguration;

import com.vmware.o11n.plugin.sdk.spring.AbstractSpringPluginFactory;
import com.vmware.o11n.plugin.sdk.spring.InventoryRef;

public final class CooptoPluginFactory extends AbstractSpringPluginFactory 
{
	private static final Logger log = LogManager.getLogger(CooptoPluginFactory.class);
	private static List<DockerNode> nodes = Collections.synchronizedList(new ArrayList<DockerNode>());
	
	public CooptoPluginFactory()
	{
		log.setLevel(Level.DEBUG);
	}
	
	@Autowired
	private DockerNodeService service;

	// find a specific item, defined by InventoryRef (vco-type, id)
    @Override
    public Object find(InventoryRef ref) 
    {
    	log.debug("running find() with id=" + ref.getId() + " and type=" + ref.getType() + ".");
        if(ref.isOfType(DockerNode.TYPE))
        {
    		try
			{
    			for(int i = 0; i<2; i++)
    			{
    				// try to find in cache - must be in synchronized block
    				synchronized (nodes) {
						Iterator<DockerNode> itr = nodes.iterator();
						while (itr.hasNext())
						{
							DockerNode node = itr.next();
							log.debug("Checking cache. Current node: " + node.getId() + ".");
							if (node.getId().equals(ref.getId()))
							{
								log.debug("Found node '" + ref.getId() + "' in cache.");
								return node;
							}
						}
					}
        			
        			// if not found in cache, refresh and try again one more time
        			if(i < 1)
        			{
            			log.warn("Unable to find node '" + ref.getId() + "' in cache.");
            			this.rebuildCache();
        			}
        			else
        			{
        				log.warn("Unable to find node '" + ref.getId() + "' after reloading cache.");
        			}
    			}
			}
			catch (Exception e)
			{
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				log.error("Error: " + sw.getBuffer().toString());
			}
        }
        else if(ref.isOfType(DockerImage.TYPE))
        {
    		try
    		{	
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
        				DockerImage image = node.getImage(ref.getId());
                		if(image != null)
                		{
                			log.debug("Found " + ref.getType() + " '" + ref.getId() + "' in cache.");
                			return image;
                		}
					}
				}
    			// getImage will also run reloadImages on it's node if not found, thus we don't have to handle that here. If it's not returned by getImage, it dosn't exist.
				log.warn("Unable to find " + ref.getType() + " '" + ref.getId() + "'.");
				//TODO dunno what should be returned if the item was not found. currently we end up with a [EMPTY_NODE] item in the inventory, even after we already called notifyDeleted in order to inform the inventory about the deleted object
    		}
    		catch (Exception e)
    		{
    			final StringWriter sw = new StringWriter();
    			final PrintWriter pw = new PrintWriter(sw, true);
    			e.printStackTrace(pw);
    			log.error("Error: " + sw.getBuffer().toString());
    		}
        }
        else if(ref.isOfType(DockerContainer.TYPE))
        {
    		try
    		{
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
        				DockerContainer container = node.getContainer(ref.getId());
                		if(container != null)
                		{
                			log.debug("Found " + ref.getType() + " '" + ref.getId() + "' in cache.");
                			return container;
                		}
					}
				}
    			// getContainer will also run reloadContainers on it's node if not found, thus we don't have to handle that here. If it's not returned by getContainer, it dosn't exist.
				log.warn("Unable to find " + ref.getType() + " '" + ref.getId() + "'.");
				//TODO dunno what should be returned if the item was not found. currently we end up with a [EMPTY_NODE] item in the inventory, even after we already called notifyDeleted in order to inform the inventory about the deleted object
    		}
    		catch (Exception e)
    		{
    			final StringWriter sw = new StringWriter();
    			final PrintWriter pw = new PrintWriter(sw, true);
    			e.printStackTrace(pw);
    			log.error("Error: " + sw.getBuffer().toString());
    		}
        }
        
        return null;
    }


    // find all items of a specific vco-type (optional, provide a query (guess OGNL?))
    @Override
    public QueryResult findAll(String type, String query) 
    {
    	log.debug("running findAll() with type=" + type + " and query=" + query + ".");
    	QueryResult qr = new QueryResult();
    	
        if(type.equals(DockerNode.TYPE))
        {
    		try
    		{
    			// try to load from EndpointConfiguration if empty
    			if(nodes.isEmpty())
    			{
    				log.warn("Unable to find " + type + " in cache.");
    				this.rebuildCache();
    			}
    			else
    			{
    				log.debug("Found " + type + " in cache.");
    			}
    			
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
						if(node != null)
						{
		    	      		qr.addElement(node);
						}
					}
				}
    		}
    		catch (Exception e)
    		{
    			final StringWriter sw = new StringWriter();
    			final PrintWriter pw = new PrintWriter(sw, true);
    			e.printStackTrace(pw);
    			log.error("Error: " + sw.getBuffer().toString());
    		}
        }
        else if(type.equals(DockerImage.TYPE))
        {
    		try
    		{
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
						if(node != null)
						{
		    				List<DockerImage> children = node.getImages();
							qr.addElements(children);
						}
					}
				}
				log.debug("Found children: " + qr);
    		}
    		catch (Exception e)
    		{
    			final StringWriter sw = new StringWriter();
    			final PrintWriter pw = new PrintWriter(sw, true);
    			e.printStackTrace(pw);
    			log.error("Error: " + sw.getBuffer().toString());
    		}
        }
        else if(type.equals(DockerContainer.TYPE))
        {
    		try
    		{
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
						if(node != null)
						{
		    				List<DockerContainer> container = node.getContainers();
							qr.addElements(container);
						}
					}
				}
				log.debug("Found children: " + qr);
    		}
    		catch (Exception e)
    		{
    			final StringWriter sw = new StringWriter();
    			final PrintWriter pw = new PrintWriter(sw, true);
    			e.printStackTrace(pw);
    			log.error("Error: " + sw.getBuffer().toString());
    		}
        }
        return qr;
    }

    // Return all children of all objects of a specific type that are related by the given relationName
    @Override
    public List<?> findChildrenInRootRelation(String type, String relationName) 
    {
    	log.debug("running findChildrenInRootRelation() with type=" + type + " and relationName=" + relationName + ".");

    	if(type.equals(CooptoModuleBuilder.ROOT))
    	{
	    	if (relationName.equals(CooptoModuleBuilder.NODERELATION)) 
	    	{
	    		return findAll(DockerNode.TYPE, null).getElements();
	    	}
	    	else 
	    	{
	    		throw new IndexOutOfBoundsException("Unknown relation name: " + relationName);
	    	}
    	}
    	else 
    	{
    		return Collections.emptyList();
    	}
    }

    // Return all children of a specific object - defined by it's InventoryRef - that are related by the given relationName
    @Override
    public List<?> findChildrenInRelation(InventoryRef parent, String relationName) 
    {
    	log.debug("running findChildrenInRelation() with parent=" + parent + " and relationName=" + relationName + ".");
    	
    	if (parent.isOfType(CooptoModuleBuilder.ROOT)) 
    	{
    		log.debug("parent is of type " + CooptoModuleBuilder.ROOT);
	    	if (relationName.equals(CooptoModuleBuilder.NODERELATION)) 
	    	{
	    		log.debug("relation is " + CooptoModuleBuilder.NODERELATION);
	    		return findAll(DockerNode.TYPE, null).getElements();
	    	}
	    	else
	    	{
	    		throw new IndexOutOfBoundsException("Unknown relation name: " + relationName);
	    	}
    	}
    	else if(parent.isOfType(DockerNode.TYPE))
    	{
    		log.debug("parent is of type " + DockerNode.TYPE + ". Searching...");
	    	
    		try
			{
  				// try to find in cache - must be in synchronized block
				synchronized (nodes) 
				{
					Iterator<DockerNode> itr = nodes.iterator();
					while (itr.hasNext())
					{
						DockerNode node = itr.next();
	    				if(node.getId().equals(parent.getId()))
	    				{
	    					log.debug("Found parent " + DockerNode.TYPE + " with id " + parent.getId());
	    			  		if (relationName.equals(DockerNode.IMAGERELATION)) 
	    			    	{
	    			    		log.debug("Relation is " + DockerNode.IMAGERELATION);
	    						if(node != null)
	    						{
	    							return node.getImages();
	    						}
	    			    	}
	    			  		else if (relationName.equals(DockerNode.CONTAINERRELATION)) 
	    			    	{
	    			    		log.debug("Relation is " + DockerNode.CONTAINERRELATION);
	    						if(node != null)
	    						{
	    							return node.getContainers();
	    						}
	    			    	}
	    			    	else 
	    			    	{
	    			    		throw new IndexOutOfBoundsException("Unknown relation name: "+ relationName);
	    			    	}
	    				}
					}
				}
			}
			catch (Exception e)
			{
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw, true);
				e.printStackTrace(pw);
				log.error("Error: " + sw.getBuffer().toString());
			}
    	}
    	log.warn("Warning: no such parent object or relation was found");
    	return Collections.emptyList();
    }
    
    /* We cannot instantiate our parent class or we'll loose @autowired functionality which we need for multiple services to work
     * Because only the finder really needs the java object, we build it here using createScriptingObject and only save the properties
     * of the object.
     */
    private DockerNode makeDockerNode(IEndpointConfiguration config) 
    {

    	String id = config.getId();
    	String displayName = config.getString(DockerNode.DISPLAYNAME);
    	String hostName = config.getString(DockerNode.HOSTNAME);
    	int portNumber = config.getAsInteger(DockerNode.HOSTPORT).intValue();
    	String apiVersion = config.getString(DockerNode.APIVERSION);
    	
    	// The same docker host may be added multiple times to the inventory - we have no way to prevent that - but the will be a different one
    	log.debug("Running makeDockerNode() for node with vco-id:" + id);
    	
    	if(id != null && !id.isEmpty())
    	{
    		if(displayName!= null && !displayName.isEmpty())
    		{
    			if(hostName != null && !hostName.isEmpty())
    			{
    				log.debug("Starting node scripting object creation...");
    				// No need to test port and API version since they will fallback to default if not set
    				DockerNode object = createScriptingObject(DockerNode.class);
    		        object.setId(id);
    		        object.setDisplayName(displayName);
    		        object.setHostName(hostName);
    		        object.setHostPortNumber(portNumber);
    		        object.setDockerApiVersion(apiVersion);
    		        log.debug("Finished node scripting object creation.");
    		        
					log.debug("Running init methods of the new node scripting object...");
					if(object.isOnline())
					{
						object.initImages(object.getShowRelatedImages());
						object.initContainers(object.getShowStoppedContainers());
						// get additional realtime info
						object.reloadNode();
					}
					else
					{
						log.warn("Warning: node '" + displayName + "' is offline.");
						object.setStatus(DockerNode.STATUS_OFFLINE);
						object.clearImages();
						object.clearContainer();
					}
		        	
					log.debug("Finished running init methods of the new node scripting object.");
    		        return object;
    			}
    	    	else
    	    	{
    	    		log.error("Unable to makeDockerHost. Reason: no hostName was provided.");
    	    	}
    		}
        	else
        	{
        		log.error("Unable to makeDockerHost. Reason: no displayName was provided.");
        	}
    	}
    	else
    	{
    		log.error("Unable to makeDockerHost. Reason: no id was provided.");
    	}
    	
    	return null;
    }
    
    // invalidateAll may be called from within the plugin or from the vCO client when clicking "refresh" in the inventory
    // No other factory methods are called after invalidateAll was called
    @Override
    public void invalidateAll()
    {
    	log.debug("Running INVALIDATEALL..");
    	this.rebuildCache();
    	super.invalidateAll();
    	log.debug("Finished running INVALIDATEALL.");
    }
    
    // invalidate may be called from within the plugin or from the vCO client when right-clicking an inventory object and selecting "refresh"
    // the following factory methods are called when invalidate was called:
    // find() with the given type and id
    // findChildrenInRelation() with the given type and id as parent (will be called once for every relation that is found on the parent object)
    @Override
    public void invalidate(String type, String id)
    {
    	log.debug("Running INVALIDATE for type '" + type + "' and id '" + id + "'...");
    	if(type.equals(DockerNode.TYPE))
    	{
    		// reload the node configuration from EndpointConfiguration
    		log.debug("Found node with id '" + id + "'.");
        	rebuildCache(id);
    	}
    	else if(type.equals(DockerImage.TYPE))
    	{
			// try to find in cache - must be in synchronized block
			synchronized (nodes) 
			{
				Iterator<DockerNode> itr = nodes.iterator();
				while (itr.hasNext())
				{
					DockerNode node = itr.next();
	    			if(node.getImage(id) != null)
	    			{
	    				log.debug("Found image on node with id '" + node.getId() + "'.");
	    				node.reloadImages();
	    			}
				}
			}
    	}
    	else if(type.equals(DockerContainer.TYPE))
    	{
			// try to find in cache - must be in synchronized block
			synchronized (nodes) 
			{
				Iterator<DockerNode> itr = nodes.iterator();
				while (itr.hasNext())
				{
					DockerNode node = itr.next();
		   			if(node.getContainer(id) != null)
	    			{
	    				log.debug("Found container on node with id '" + node.getId() + "'.");
	    				node.reloadContainers();
	    			}
				}
			}
    	}
    	super.invalidate(type, id);
    	log.debug("Finished running INVALIDATE for type '" + type + "' and id '" + id + "'.");
    }
    
    private void rebuildCache()
    {
    	log.debug("Rebuilding full cache...");
		log.debug("Current cachesize: " + nodes.size() + ".");
		/* Do not just simply clear the cache because e.g. find() requests executed while the new cache is being build 
		 * would result in cache misses and trigger a cache rebuild, causing multiple inventory-objects to be created.
		 * 
		 * Instead, build the new cache into a newNode array, and replace it in a synchronized way
		 */
		ArrayList<DockerNode> newNodes = new ArrayList<DockerNode>();
		
		// Load node data from EndpointConfiguration
		for (IEndpointConfiguration config : service.getNodes())
		{
			if(config != null)
			{
				// create scripting objects, reload child objects & add to cache
				newNodes.add(this.makeDockerNode(config));
			}
		}
		
		// replace old list - must be in synchronized block
		synchronized (nodes) 
		{
			nodes = Collections.synchronizedList(newNodes);
		}
		log.debug("New cachesize: " + nodes.size() + ".");
		log.debug("Finished full cache rebuild.");
    }
    
    private void rebuildCache(String nodeId)
    {
    	log.debug("Rebuilding cache for node '" + nodeId + "'.");
    	log.debug("Current cachesize: " + nodes.size() + ".");
    	// Remove the node which should be refreshed if present in the cache - must be in synchronized block
		synchronized (nodes) 
		{
			// Do not use for-loops for this, since it will cause ConcurrentModificationExceptions to be thrown!
			Iterator<DockerNode> itr = nodes.iterator();
			while (itr.hasNext())
			{
				DockerNode node = itr.next();
				if(node.getId().equals(nodeId))
	    		{
					// remove this node from nodes list without causing ConcurrentModificationExceptions by using iterator.remove()
					itr.remove();

	    			log.debug("Cleared cache for node ' " + nodeId +"'.");
	    		}
			}
		}

    	// Reload the node from the EndpointConfiguration and add it to our synchronized list
		DockerNode node = this.makeDockerNode(service.getNode(nodeId));
		
		nodes.add(node);
		log.debug("New cachesize: " + nodes.size() + ".");
    	log.debug("Finished cache rebuild for node '" + nodeId + "'.");
    }
    
}