/*
**    Copyright (C) 2003-2010 Institute for Systems Biology 
**                            Seattle, Washington, USA. 
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.systemsbiology.biotapestry.plugin;

/****************************************************************************
**
** Interface for plugins that display experimental data
*/

public interface ExternalLinkDataDisplayPlugIn extends DataDisplayPlugIn {
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC CONSTANTS
  //
  ////////////////////////////////////////////////////////////////////////////  
 
  public static final boolean IS_INTERNAL_ANSWER = false;
  
  ////////////////////////////////////////////////////////////////////////////
  //
  // PUBLIC METHODS
  //
  ////////////////////////////////////////////////////////////////////////////
  
  /***************************************************************************
  **
  ** Answers if this plugin will require a per-instance display (as opposed to
  ** e.g. a single data window for a gene that is shared by all instances)
  */
  
  public boolean requiresPerInstanceDisplay(String[] modelNameChain, 
                                            String srcNodeName, String srcRegionName, 
                                            String trgNodeName, String trgRegionName);
  
  /***************************************************************************
  **
  ** Answers if we can get back data later after retrieving it on a background thread.
  ** If true, the getDataAsHTML routine above should only provide an "initial" data view.
  */
  
  public boolean haveCallbackWorker();
  
  /***************************************************************************
  **
  ** Get the callback worker
  */
  
  public PluginCallbackWorker getCallbackWorker(String[] modelNameChain, 
                                                String srcNodeName, String srcRegionName, 
                                                String trgNodeName, String trgRegionName);
    
  /***************************************************************************
  **
  ** Get a string of valid HTML (no html or body tags, please!) to display
  ** as experimental data for the desired linkage between the source and
  ** target.  The specified source and target regions may both 
  ** simultaneously be null for the top-level VfG.  The model name chain
  ** provides the names of all the models down from the root ("Full Genome") 
  */
  
  public String getDataAsHTML(String[] modelNameChain, 
                              String srcNodeName, String srcRegionName, 
                              String trgNodeName, String trgRegionName);

}
