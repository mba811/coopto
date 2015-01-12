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

import org.hexlogic.model.DockerNode;
import com.vmware.o11n.plugin.sdk.module.ModuleBuilder;

public final class CooptoModuleBuilder extends ModuleBuilder 
{
    private static final String PLUGINNAME = "Coopto";
    private static final String DESCRIPTION = "Coopto - a Docker plug-in for vRealize Orchestrator.\n\nThis is a open source project provided on GNU Lesser General Public License version 3.0 terms. Get it at https://github.com/m451/coopto.\n\n\nMaintainer(s):\n- Robert Szymczak (rszymczak@fum.de)";
    private static final String DISPLAYNAME = "Coopto";
    private static final String DATASOURCE = "main-datasource";
	public static final String ROOT = "COOPTOROOT";
	public static final String NODERELATION = "DockerNodes";

//	private static final Logger log = Logger.getLogger(ModuleBuilder.class);
	
    @Override
    public void configure() 
    {
        module(PLUGINNAME)
        .displayName(DISPLAYNAME)
        .withDescription(DESCRIPTION)
        .withImage("images/root_32x32.png")
        .buildNumber("${buildNumber}") // Will be attached to the version number, e.g.: version 1.0.1 build 1 == 1.0.0.1
        .basePackages(CooptoModuleBuilder.class.getPackage().getName()).version("${project.version}")
        .installation(InstallationMode.VERSION)
        .action(ActionType.INSTALL_PACKAGE,"packages/${project.artifactId}-package-${project.version}.package");
        
        finderDatasource(CooptoPluginAdaptor.class, DATASOURCE).anonymousLogin(LoginMode.INTERNAL);
        inventory(ROOT);
        finder(ROOT, DATASOURCE).addRelation(DockerNode.TYPE, NODERELATION).hide(true);
    }
}
