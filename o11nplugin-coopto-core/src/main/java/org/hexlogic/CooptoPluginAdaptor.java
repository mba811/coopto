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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.vmware.o11n.plugin.sdk.spring.AbstractSpringPluginAdaptor;


public final class CooptoPluginAdaptor extends  AbstractSpringPluginAdaptor
{

    private static final String DEFAULT_CONFIG = "org/hexlogic/pluginConfig.xml";
    
    @Override
    protected ApplicationContext createApplicationContext(ApplicationContext defaultParent) 
    {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
        		new String[] { DEFAULT_CONFIG }, false, defaultParent);
        applicationContext.setClassLoader(getClass().getClassLoader());
        applicationContext.refresh();
    
        return applicationContext;
    }
    
    @Override
    protected void afterContextInitialized()
    {
    	// TODO Reload docker node when done creating a new session
    	super.afterContextInitialized();
    }
}